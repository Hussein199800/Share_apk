package com.example.send

import com.example.receive.discovery.NearbyDevice
import com.example.receive.net.TrustAllHttp
import com.example.receive.protocol.DeviceInfoDto
import com.example.receive.protocol.FileDto
import com.example.receive.protocol.PrepareUploadRequest
import com.example.receive.protocol.PrepareUploadResponse
import com.example.receive.protocol.ProtocolJson
import com.example.receive.protocol.ShareApkMetaDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

data class OutgoingFile(val file: File, val fileName: String, val meta: ShareApkMetaDto?, val label: String = fileName)

sealed interface SendResult {
    data object Sent : SendResult
    data object Rejected : SendResult
    data object Busy : SendResult
    data class Failed(val message: String) : SendResult
}

/** LocalSend v2 upload-API client (works with LocalSend and ShareAPK receivers). */
class SendClient {
    private val json = "application/json".toMediaType()
    private val base = TrustAllHttp.client()
    // prepare-upload blocks until the receiver's user decides.
    private val prepareClient = base.newBuilder().readTimeout(3, TimeUnit.MINUTES).callTimeout(4, TimeUnit.MINUTES).build()
    private val probeClient = base.newBuilder().connectTimeout(1500, TimeUnit.MILLISECONDS).readTimeout(2, TimeUnit.SECONDS).build()

    /** Asks [ip]:[port] who it is (legacy HTTP discovery). Tries HTTPS, then HTTP. */
    suspend fun probe(ip: String, port: Int, me: DeviceInfoDto): NearbyDevice? = withContext(Dispatchers.IO) {
        for (scheme in listOf("https", "http")) {
            val info = runCatching {
                probeClient.newCall(
                    Request.Builder().url("$scheme://$ip:$port/api/localsend/v2/register")
                        .post(ProtocolJson.encode(me).toRequestBody(json)).build()
                ).execute().use { r -> if (r.isSuccessful) ProtocolJson.decode<DeviceInfoDto>(r.body!!.string()) else null }
            }.getOrNull() ?: continue
            if (info.fingerprint == me.fingerprint) return@withContext null
            return@withContext NearbyDevice(info.copy(port = info.port ?: port, protocol = info.protocol ?: scheme), ip, System.currentTimeMillis())
        }
        null
    }

    /** Sends [files] to [peer]. [onProgress] reports (sentBytes, totalBytes). Cancel the coroutine to abort. */
    suspend fun send(
        peer: NearbyDevice,
        me: DeviceInfoDto,
        files: List<OutgoingFile>,
        onProgress: (Long, Long) -> Unit
    ): SendResult = withContext(Dispatchers.IO) {
        val root = "${peer.scheme}://${peer.ip}:${peer.port}/api/localsend/v2"
        val dtos = files.mapIndexed { i, f ->
            FileDto(
                id = "f$i",
                fileName = f.fileName,
                size = f.file.length(),
                fileType = "application/vnd.android.package-archive",
                sha256 = sha256(f.file),
                shareApk = f.meta
            )
        }
        val body = ProtocolJson.encode(PrepareUploadRequest(me, dtos.associateBy { it.id }))
        val prep = runCatching {
            prepareClient.newCall(Request.Builder().url("$root/prepare-upload").post(body.toRequestBody(json)).build()).execute()
        }.getOrElse { return@withContext SendResult.Failed(it.message ?: "connection failed") }

        val response = prep.use { r ->
            when (r.code) {
                200 -> runCatching { ProtocolJson.decode<PrepareUploadResponse>(r.body!!.string()) }.getOrNull()
                    ?: return@withContext SendResult.Failed("bad response")
                204 -> return@withContext SendResult.Sent
                403 -> return@withContext SendResult.Rejected
                409 -> return@withContext SendResult.Busy
                else -> return@withContext SendResult.Failed("HTTP ${r.code}")
            }
        }

        val total = files.sumOf { it.file.length() }
        var sentBefore = 0L
        try {
            files.forEachIndexed { i, f ->
                coroutineContext.ensureActive()
                val id = "f$i"
                val token = response.files[id] ?: return@forEachIndexed // receiver skipped this file
                val offset = sentBefore
                val req = Request.Builder()
                    .url("$root/upload?sessionId=${response.sessionId}&fileId=$id&token=$token")
                    .post(ProgressBody(f.file) { n -> onProgress(offset + n, total) })
                    .build()
                val code = prepareClient.newCall(req).execute().use { it.code }
                if (code != 200) return@withContext SendResult.Failed("upload HTTP $code")
                sentBefore += f.file.length()
            }
        } catch (e: Exception) {
            runCatching {
                base.newCall(Request.Builder().url("$root/cancel?sessionId=${response.sessionId}").post(ByteArray(0).toRequestBody()).build()).execute().close()
            }
            if (e is kotlinx.coroutines.CancellationException) throw e
            return@withContext SendResult.Failed(e.message ?: e.javaClass.simpleName)
        }
        onProgress(total, total)
        SendResult.Sent
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) { val n = input.read(buf); if (n < 0) break; md.update(buf, 0, n) }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private class ProgressBody(private val file: File, private val onBytes: (Long) -> Unit) : RequestBody() {
        override fun contentType(): MediaType = "application/octet-stream".toMediaType()
        override fun contentLength() = file.length()
        override fun writeTo(sink: BufferedSink) {
            file.inputStream().use { input ->
                val buf = ByteArray(64 * 1024)
                var done = 0L
                var last = 0L
                while (true) {
                    val n = input.read(buf); if (n < 0) break
                    sink.write(buf, 0, n)
                    done += n
                    if (done - last >= 256 * 1024) { last = done; onBytes(done) }
                }
                onBytes(done)
            }
        }
    }
}
