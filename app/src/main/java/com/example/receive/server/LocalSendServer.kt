package com.example.receive.server

import com.example.receive.PrepareResult
import com.example.receive.ReceiveSessionManager
import com.example.receive.UploadResult
import com.example.receive.identity.CertificateStore
import com.example.receive.identity.DeviceIdentity
import com.example.receive.protocol.DeviceInfoDto
import com.example.receive.protocol.PrepareUploadRequest
import com.example.receive.protocol.ProtocolJson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.connector
import io.netty.handler.ssl.OptionalSslHandler
import io.netty.handler.ssl.SslContextBuilder
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.origin
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.runBlocking

/** LocalSend v2 receiver (upload API) over HTTPS or plain HTTP on the same port. */
class LocalSendServer(
    private val cert: CertificateStore.Loaded,
    private val manager: ReceiveSessionManager,
    /** Called with every device that registers with us (replies to our announcement, scans). */
    private val onPeer: (DeviceInfoDto, String) -> Unit = { _, _ -> },
    private val identity: () -> DeviceIdentity
) {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    /** Starts on [port] (0 = any free port) and returns the bound port. */
    fun start(port: Int): Int {
        stop()
        val s = embeddedServer(
            Netty,
            environment = applicationEnvironment { },
            configure = {
                // One port for both: LocalSend peers with encryption on speak TLS, peers with it
                // off speak plain HTTP. OptionalSslHandler sniffs the first bytes and adds TLS only
                // when the client starts a TLS handshake.
                connector {
                    this.host = "0.0.0.0"
                    this.port = port
                }
                val sslContext = SslContextBuilder.forServer(cert.privateKey, cert.certificate).build()
                channelPipelineConfig = {
                    addFirst("optional-tls", OptionalSslHandler(sslContext))
                }
            }
        ) {
            routing {
                post("$BASE/register") {
                    runCatching { ProtocolJson.decode<DeviceInfoDto>(call.receiveText()) }.getOrNull()
                        ?.let { onPeer(it, call.request.origin.remoteAddress) }
                    call.respondJson(ProtocolJson.encode(identity().toDto()))
                }
                get("$BASE/info") { call.respondJson(ProtocolJson.encode(identity().toDto())) }
                post("$BASE/prepare-upload") {
                    // Metadata only: refuse oversized bodies instead of reading them into memory.
                    if ((call.request.headers["Content-Length"]?.toLongOrNull() ?: 0) > MAX_JSON_BYTES) {
                        return@post call.respond(HttpStatusCode.PayloadTooLarge)
                    }
                    val req = runCatching { ProtocolJson.decode<PrepareUploadRequest>(call.receiveText()) }.getOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest)
                    when (val r = manager.prepare(req, call.request.origin.remoteAddress)) {
                        is PrepareResult.Accepted -> call.respondJson(ProtocolJson.encode(r.response))
                        PrepareResult.Blocked -> call.respond(HttpStatusCode.Conflict)
                        PrepareResult.NoTransfer -> call.respond(HttpStatusCode.NoContent)
                        PrepareResult.Rejected -> call.respond(HttpStatusCode.Forbidden)
                    }
                }
                post("$BASE/upload") {
                    val q = call.request.queryParameters
                    val sid = q["sessionId"]; val fid = q["fileId"]; val token = q["token"]
                    if (sid == null || fid == null || token == null) return@post call.respond(HttpStatusCode.BadRequest)
                    val result = manager.receive(sid, fid, token, call.request.origin.remoteAddress, call.receiveChannel().toInputStream())
                    call.respond(
                        when (result) {
                            UploadResult.Ok -> HttpStatusCode.OK
                            UploadResult.Forbidden -> HttpStatusCode.Forbidden
                            UploadResult.ChecksumMismatch -> HttpStatusCode.UnprocessableEntity
                            is UploadResult.Error -> HttpStatusCode.InternalServerError
                        }
                    )
                }
                post("$BASE/cancel") {
                    call.request.queryParameters["sessionId"]?.let { manager.cancel(it, call.request.origin.remoteAddress) }
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
        s.start(wait = false)
        server = s
        return runBlocking { s.engine.resolvedConnectors().first().port }
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 200, timeoutMillis = 1_000)
        server = null
    }

    private suspend fun ApplicationCall.respondJson(text: String) = respondText(text, ContentType.Application.Json)

    companion object {
        const val BASE = "/api/localsend/v2"
        const val MAX_JSON_BYTES = 1_000_000L
    }
}
