package com.example.receive

import com.example.receive.protocol.DeviceInfoDto
import com.example.receive.protocol.FileDto
import com.example.receive.protocol.PrepareUploadRequest
import com.example.receive.protocol.PrepareUploadResponse
import com.example.receive.protocol.ShareApkMetaDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

sealed interface PrepareResult {
    data class Accepted(val response: PrepareUploadResponse) : PrepareResult
    data object Rejected : PrepareResult
    data object Blocked : PrepareResult
    /** Only text messages: nothing to upload (HTTP 204). */
    data object NoTransfer : PrepareResult
}

sealed interface UploadResult {
    data object Ok : UploadResult
    data object Forbidden : UploadResult
    data object ChecksumMismatch : UploadResult
    data class Error(val message: String) : UploadResult
}

/** A request waiting for the user: files to upload plus text messages (shown inline). */
data class IncomingRequest(val sender: DeviceInfoDto, val senderIp: String, val files: List<FileDto>, val messages: List<String> = emptyList())

data class FileProgress(val fileId: String, val fileName: String, val size: Long, val received: Long, val done: Boolean)

/** A received file. [relativeDir] is the sender's folder path (sanitized), "" for single files. */
data class ReceivedFile(val fileName: String, val file: File, val meta: ShareApkMetaDto?, val relativeDir: String = "", val mimeType: String? = null) {
    val isApp get() = ReceiveSessionManager.isAppPackage(fileName)
}

/** Why a request ended without the user deciding (shown as a hint in the Receive tab). */
sealed interface ReceiveEvent {
    data class TimedOut(val sender: String) : ReceiveEvent
    data class Busy(val sender: String) : ReceiveEvent
}

sealed interface ReceiveState {
    data object Idle : ReceiveState
    data class AwaitingDecision(val request: IncomingRequest) : ReceiveState
    data class Receiving(val sender: DeviceInfoDto, val sessionId: String, val files: List<FileProgress>) : ReceiveState {
        val totalBytes get() = files.sumOf { it.size }
        val receivedBytes get() = files.sumOf { it.received }
    }
    data class Done(val sender: DeviceInfoDto, val files: List<ReceivedFile>, val messages: List<String> = emptyList()) : ReceiveState
}

/**
 * LocalSend upload-API session logic, independent of HTTP. One session at a time, as the
 * protocol expects (409 while busy). Files are streamed to `destRoot/<sessionId>/`.
 */
class ReceiveSessionManager(
    private val destRoot: File,
    private val decisionTimeoutMs: Long = 60_000,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    private class Session(
        val id: String,
        val sender: DeviceInfoDto,
        val senderIp: String,
        val files: Map<String, FileDto>,
        val tokens: Map<String, String>,
        val written: MutableMap<String, File> = mutableMapOf()
    )

    private val _state = MutableStateFlow<ReceiveState>(ReceiveState.Idle)
    val state: StateFlow<ReceiveState> = _state.asStateFlow()

    private val _lastEvent = MutableStateFlow<ReceiveEvent?>(null)
    val lastEvent: StateFlow<ReceiveEvent?> = _lastEvent.asStateFlow()

    private val lock = Any()
    private var pendingDecision: CompletableDeferred<Boolean>? = null
    private var session: Session? = null
    private var pendingMessages: List<String> = emptyList()
    @Volatile private var lastActivity = 0L
    private val usedTokens = mutableSetOf<String>()

    fun sessionDir(sessionId: String) = File(destRoot, sessionId.replace(Regex("[^A-Za-z0-9_-]"), "_"))

    suspend fun prepare(req: PrepareUploadRequest, senderIp: String): PrepareResult {
        // LocalSend sends text as a "file" with the text in `preview`: show it, don't upload it.
        val messages = req.files.values.filter(::isMessage).mapNotNull { it.preview }
        val accepted = req.files.values.filterNot(::isMessage)
        if (accepted.isEmpty() && messages.isEmpty()) return PrepareResult.Rejected
        val decision = CompletableDeferred<Boolean>()
        synchronized(lock) {
            if (pendingDecision != null || session != null) {
                _lastEvent.value = ReceiveEvent.Busy(req.info.alias)
                return PrepareResult.Blocked
            }
            pendingDecision = decision
            _state.value = ReceiveState.AwaitingDecision(
                IncomingRequest(req.info, senderIp, accepted, messages)
            )
        }
        val answer = try {
            withTimeoutOrNull(decisionTimeoutMs) { decision.await() }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Server stopped or the sender hung up while the sheet was showing: never stay wedged.
            synchronized(lock) {
                if (pendingDecision === decision) {
                    pendingDecision = null
                    _state.value = ReceiveState.Idle
                }
            }
            throw e
        }
        if (answer == null) _lastEvent.value = ReceiveEvent.TimedOut(req.info.alias)
        val ok = answer ?: false
        synchronized(lock) {
            pendingDecision = null
            if (!ok) {
                _state.value = ReceiveState.Idle
                return PrepareResult.Rejected
            }
            if (accepted.isEmpty()) {
                _state.value = ReceiveState.Done(req.info, emptyList(), messages)
                return PrepareResult.NoTransfer
            }
            val id = newId()
            pendingMessages = messages
            val tokens = accepted.associate { it.id to newId() }
            session = Session(id, req.info, senderIp, accepted.associateBy { it.id }, tokens)
            lastActivity = System.currentTimeMillis()
            sessionDir(id).mkdirs()
            _state.value = ReceiveState.Receiving(
                req.info, id, accepted.map { FileProgress(it.id, it.fileName, it.size, 0, false) }
            )
            return PrepareResult.Accepted(PrepareUploadResponse(id, tokens))
        }
    }

    /** User's answer to the pending request. */
    fun decide(accept: Boolean) {
        pendingDecision?.complete(accept)
    }

    suspend fun receive(sessionId: String, fileId: String, token: String, senderIp: String, body: InputStream): UploadResult {
        val s = synchronized(lock) { session }
        val dto = s?.files?.get(fileId)
        if (s == null || s.id != sessionId || dto == null || s.tokens[fileId] != token || s.senderIp != senderIp) {
            return UploadResult.Forbidden
        }
        synchronized(lock) { if (!usedTokens.add(token)) return UploadResult.Forbidden }
        val target = synchronized(lock) { uniqueTarget(s, dto.fileName).also { s.written[fileId] = it } }
        val digest = MessageDigest.getInstance("SHA-256")
        return try {
            withContext(Dispatchers.IO) {
                var total = 0L
                var lastReport = 0L
                target.outputStream().buffered().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = body.read(buf)
                        if (n < 0) break
                        total += n
                        // Never write more than the sender declared.
                        if (total > dto.size) throw java.io.IOException("more data than declared")
                        out.write(buf, 0, n)
                        digest.update(buf, 0, n)
                        lastActivity = System.currentTimeMillis()
                        if (total - lastReport >= 256 * 1024) {
                            lastReport = total
                            updateProgress(s.id, fileId, total, false)
                        }
                    }
                }
                val expected = dto.sha256?.lowercase()
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (expected != null && expected != actual) {
                    target.delete()
                    updateProgress(s.id, fileId, 0, false)
                    return@withContext UploadResult.ChecksumMismatch
                }
                updateProgress(s.id, fileId, total, true)
                finishIfComplete(s)
                UploadResult.Ok
            }
        } catch (e: Exception) {
            target.delete()
            // A broken upload ends the session so the receiver (and other senders) aren't stuck.
            cancel(s.id)
            UploadResult.Error(e.message ?: e.javaClass.simpleName)
        }
    }

    /** Watchdog: cancel an accepted session that has had no upload activity for [idleMs]. */
    fun expireIfIdle(nowMs: Long = System.currentTimeMillis(), idleMs: Long = 60_000) {
        val s = synchronized(lock) { session } ?: return
        if (nowMs - lastActivity > idleMs) cancel(s.id)
    }

    /** Sender cancelled (or the user did): drop partial files and return to idle. */
    fun cancel(sessionId: String, senderIp: String? = null) {
        synchronized(lock) {
            val s = session ?: return
            if (s.id != sessionId) return
            if (senderIp != null && senderIp != s.senderIp) return
            session = null
            usedTokens.clear()
            sessionDir(s.id).deleteRecursively()
            _state.value = ReceiveState.Idle
        }
    }

    /** Cancel whatever is active (UI action). */
    fun cancelActive() {
        pendingDecision?.complete(false)
        session?.let { cancel(it.id) }
    }

    /** Leave the Done screen. */
    fun acknowledge() {
        synchronized(lock) { if (_state.value is ReceiveState.Done) _state.value = ReceiveState.Idle }
    }

    private fun updateProgress(sessionId: String, fileId: String, received: Long, done: Boolean) {
        _state.update { st ->
            if (st is ReceiveState.Receiving && st.sessionId == sessionId) {
                st.copy(files = st.files.map { if (it.fileId == fileId) it.copy(received = received, done = done) else it })
            } else st
        }
    }

    private fun finishIfComplete(s: Session) {
        synchronized(lock) {
            val st = _state.value as? ReceiveState.Receiving ?: return
            if (st.sessionId != s.id || !st.files.all { it.done }) return
            session = null
            usedTokens.clear()
            _state.value = ReceiveState.Done(
                s.sender,
                s.files.values.mapNotNull { dto ->
                    s.written[dto.id]?.let { ReceivedFile(it.name, it, dto.shareApk, relativeDirOf(dto.fileName), dto.fileType) }
                },
                pendingMessages
            )
        }
    }

    private fun uniqueTarget(s: Session, rawName: String): File {
        val dir = sessionDir(s.id).apply { mkdirs() }
        val clean = sanitizeFileName(rawName)
        val base = clean.substringBeforeLast('.', clean)
        val ext = clean.substringAfterLast('.', "")
        var candidate = File(dir, clean)
        var i = 1
        while (candidate.exists() || s.written.values.any { it == candidate }) {
            candidate = File(dir, if (ext.isEmpty()) "$base ($i)" else "$base ($i).$ext")
            i++
        }
        return candidate
    }

    companion object {
        val APP_EXTENSIONS = setOf("apk", "apks", "xapk", "apkm")

        fun isAppPackage(fileName: String) = fileName.substringAfterLast('.', "").lowercase() in APP_EXTENSIONS

        fun isMessage(f: FileDto) = f.preview != null && f.fileType?.startsWith("text/") == true && f.size <= 64 * 1024

        /** Folder part of a sender path ("Trip/Day 1/a.jpg" → "Trip/Day 1"), without "..", "." or empty segments. */
        fun relativeDirOf(name: String): String =
            name.replace('\\', '/').split('/').dropLast(1)
                .map { it.replace(Regex("[:*?\"<>|\\u0000-\\u001f]"), "_").trim() }
                .filter { it.isNotEmpty() && it != "." && it != ".." }
                .joinToString("/")

        /** Keeps only the last path segment and strips characters unsafe in file names. */
        fun sanitizeFileName(name: String): String {
            val last = name.replace('\\', '/').substringAfterLast('/')
            val cleaned = last.replace(Regex("[:*?\"<>|\\u0000-\\u001f]"), "_").trimStart('.').trim()
            return cleaned.ifBlank { "package.apk" }
        }
    }
}
