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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OutgoingFile(val file: File, val fileName: String, val meta: ShareApkMetaDto?, val label: String = fileName)

sealed interface SendResult {
    data object Sent : SendResult
    data object Rejected : SendResult
    data object Busy : SendResult
    data class Failed(val message: String) : SendResult
}

/** Remembers which TLS certificate each LocalSend device presented (trust on first use). */
interface PinStore {
    fun get(deviceFingerprint: String): String?
    fun put(deviceFingerprint: String, certSha256: String)
}

class InMemoryPins : PinStore {
    private val map = ConcurrentHashMap<String, String>()
    override fun get(deviceFingerprint: String) = map[deviceFingerprint]
    override fun put(deviceFingerprint: String, certSha256: String) { map[deviceFingerprint] = certSha256 }
}

/**
 * LocalSend v2 upload-API client (works with LocalSend and ShareAPK receivers).
 *
 * Peer identity over HTTPS: a ShareAPK peer must present the certificate whose SHA-256 is its
 * announced fingerprint; any other LocalSend peer is pinned on first contact and refused if its
 * certificate later changes.
 */
class SendClient(private val pins: PinStore = InMemoryPins()) {
    private val json = "application/json".toMediaType()
    private val base = TrustAllHttp.client()
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

    /** Sends [files] to [peer]. [onProgress] reports (sentBytes, totalBytes). Cancelling the coroutine aborts the HTTP calls. */
    suspend fun send(
        peer: NearbyDevice,
        me: DeviceInfoDto,
        files: List<OutgoingFile>,
        onProgress: (Long, Long) -> Unit
    ): SendResult = withContext(Dispatchers.IO) {
        val (client, seenCert) = clientFor(peer)
        // prepare-upload blocks until the receiver's user decides.
        val prepareClient = client.newBuilder().readTimeout(3, TimeUnit.MINUTES).callTimeout(4, TimeUnit.MINUTES).build()
        // Uploads may take long for big bundles: no overall call timeout, only inactivity timeouts.
        val uploadClient = client.newBuilder().callTimeout(0, TimeUnit.SECONDS).readTimeout(2, TimeUnit.MINUTES).writeTimeout(2, TimeUnit.MINUTES).build()
        val root = "${peer.scheme}://${peer.ip}:${peer.port}/api/localsend/v2".toHttpUrl()
        fun url(path: String, vararg query: Pair<String, String>): HttpUrl =
            root.newBuilder().addPathSegment(path).apply { query.forEach { (k, v) -> addQueryParameter(k, v) } }.build()

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
            prepareClient.newCall(Request.Builder().url(url("prepare-upload")).post(body.toRequestBody(json)).build()).await()
        }.getOrElse {
            if (it is kotlinx.coroutines.CancellationException) throw it
            return@withContext SendResult.Failed(it.message ?: "connection failed")
        }
        // The TLS handshake succeeded with an acceptable certificate: remember it for next time.
        seenCert()?.let { pins.put(peer.info.fingerprint, it) }

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
                    .url(url("upload", "sessionId" to response.sessionId, "fileId" to id, "token" to token))
                    .post(ProgressBody(f.file) { n -> onProgress(offset + n, total) })
                    .build()
                val code = uploadClient.newCall(req).await().use { it.code }
                if (code != 200) return@withContext SendResult.Failed("upload HTTP $code")
                sentBefore += f.file.length()
            }
        } catch (e: Exception) {
            runCatching {
                base.newCall(Request.Builder().url(url("cancel", "sessionId" to response.sessionId)).post(ByteArray(0).toRequestBody()).build()).execute().close()
            }
            if (e is kotlinx.coroutines.CancellationException) throw e
            return@withContext SendResult.Failed(e.message ?: e.javaClass.simpleName)
        }
        onProgress(total, total)
        SendResult.Sent
    }

    /** HTTP client for [peer] plus a getter for the certificate hash it presented (HTTPS only). */
    private fun clientFor(peer: NearbyDevice): Pair<OkHttpClient, () -> String?> {
        if (peer.scheme != "https") return base to { null }
        var seen: String? = null
        val expected = if (peer.isShareApk) peer.info.fingerprint.lowercase() else pins.get(peer.info.fingerprint)
        val tm = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                val leaf = chain?.firstOrNull() ?: throw CertificateException("no certificate")
                val hash = MessageDigest.getInstance("SHA-256").digest(leaf.encoded).joinToString("") { "%02x".format(it) }
                if (expected != null && hash != expected) throw CertificateException("device identity changed")
                seen = hash
            }
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val ssl = SSLContext.getInstance("TLS").apply { init(null, arrayOf(tm), SecureRandom()) }
        // A dedicated connection pool, so a connection verified for another peer is never reused.
        val client = base.newBuilder()
            .connectionPool(okhttp3.ConnectionPool())
            .sslSocketFactory(ssl.socketFactory, tm)
            .build()
        return client to { seen }
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

/** Runs the call asynchronously; cancelling the coroutine cancels the HTTP call (and its upload). */
internal suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) = cont.resume(response)
        override fun onFailure(call: Call, e: IOException) { if (!cont.isCancelled) cont.resumeWithException(e) }
    })
    cont.invokeOnCancellation { runCatching { cancel() } }
}
