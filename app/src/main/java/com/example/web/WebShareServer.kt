package com.example.web

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger

/** Something the browser can download. [open] returns null if it vanished. */
class WebItem(
    val id: String,
    val kind: Kind,
    val name: String,
    val subtitle: String,
    val size: Long,
    val mimeType: String,
    val fileName: String,
    val hasIcon: Boolean = false,
    /** A text snippet shown (and copyable) in the page instead of a download. */
    val text: String? = null,
    /** Writes the content; used for streamed bundles whose size isn't known up front. */
    val write: ((OutputStream) -> Unit)? = null,
    val open: (() -> InputStream?)? = null
) {
    enum class Kind { APP, RECEIVED_APP, FILE, TEXT }
}

/** What the phone offers and where uploads go. */
interface WebShareSource {
    fun items(): List<WebItem>
    fun iconPng(id: String): ByteArray?
    /** Takes ownership of [tmp] (an uploaded file named [name]). */
    suspend fun acceptUpload(tmp: File, name: String)
    suspend fun acceptText(text: String)
}

@Serializable
internal data class WebItemDto(val id: String, val kind: String, val name: String, val subtitle: String, val size: Long, val icon: Boolean, val text: String? = null)

/** Plain-HTTP server for the browser page (desktop browsers reject self-signed HTTPS loudly). */
class WebShareServer(
    private val gate: PinGate,
    private val source: WebShareSource,
    private val tmpDir: File,
    private val onActivity: () -> Unit = {},
    private val onTransfersChanged: (Int) -> Unit = {}
) {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private val transfers = AtomicInteger(0)
    private val json = Json { encodeDefaults = true }

    fun start(port: Int): Int {
        stop()
        val s = embeddedServer(Netty, environment = applicationEnvironment { }, configure = {
            connector { host = "0.0.0.0"; this.port = port }
        }) {
            routing {
                get("/") {
                    onActivity()
                    call.respondText(WebPage.HTML, ContentType.Text.Html.withParameter("charset", "utf-8"))
                }
                post("/api/login") {
                    onActivity()
                    val pin = call.receiveText().take(16)
                    val token = gate.login(pin) ?: return@post call.respond(HttpStatusCode.Forbidden)
                    call.response.header(HttpHeaders.SetCookie, "$COOKIE=$token; Path=/; HttpOnly; SameSite=Strict")
                    call.respond(HttpStatusCode.NoContent)
                }
                get("/api/items") {
                    if (!authorized()) return@get
                    val dtos = source.items().map { WebItemDto(it.id, it.kind.name.lowercase(), it.name, it.subtitle, it.size, it.hasIcon, it.text) }
                    call.respondText(json.encodeToString(kotlinx.serialization.builtins.ListSerializer(WebItemDto.serializer()), dtos), ContentType.Application.Json)
                }
                get("/icon/{id}") {
                    if (!authorized()) return@get
                    val png = source.iconPng(call.parameters["id"].orEmpty()) ?: return@get call.respond(HttpStatusCode.NotFound)
                    call.response.header(HttpHeaders.CacheControl, "private, max-age=3600")
                    call.respondBytes(png, ContentType.Image.PNG)
                }
                get("/dl/{id}") {
                    if (!authorized()) return@get
                    val id = call.parameters["id"].orEmpty()
                    val item = source.items().firstOrNull { it.id == id } ?: return@get call.respond(HttpStatusCode.NotFound)
                    call.response.header(HttpHeaders.ContentDisposition, attachment(item.fileName))
                    tracked {
                        val type = ContentType.parse(item.mimeType)
                        val write = item.write
                        if (write != null) {
                            call.respondOutputStream(type) { write(this) }
                        } else {
                            val input = item.open?.invoke() ?: return@tracked call.respond(HttpStatusCode.NotFound)
                            call.respondOutputStream(type, contentLength = item.size.takeIf { it > 0 }) {
                                input.use { it.copyTo(this, 64 * 1024) }
                            }
                        }
                    }
                }
                post("/api/upload") {
                    if (!authorized()) return@post
                    val name = safeName(call.request.queryParameters["name"])
                    tmpDir.mkdirs()
                    val tmp = File.createTempFile("web-", ".part", tmpDir)
                    val ok = tracked {
                        runCatching {
                            call.receiveChannel().toInputStream().use { input -> tmp.outputStream().use { input.copyTo(it, 64 * 1024) } }
                        }.isSuccess
                    }
                    if (!ok) {
                        tmp.delete()
                        return@post call.respond(HttpStatusCode.InternalServerError)
                    }
                    source.acceptUpload(tmp, name)
                    call.respond(HttpStatusCode.NoContent)
                }
                post("/api/text") {
                    if (!authorized()) return@post
                    val text = call.receiveText().take(MAX_TEXT)
                    if (text.isNotBlank()) source.acceptText(text)
                    call.respond(HttpStatusCode.NoContent)
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

    private suspend fun RoutingContext.authorized(): Boolean {
        onActivity()
        if (gate.isValid(call.request.cookies[COOKIE])) return true
        call.respond(HttpStatusCode.Unauthorized)
        return false
    }

    private suspend fun <T> tracked(block: suspend () -> T): T {
        onTransfersChanged(transfers.incrementAndGet())
        try {
            return block()
        } finally {
            onTransfersChanged(transfers.decrementAndGet())
            onActivity()
        }
    }

    companion object {
        const val COOKIE = "sa_t"
        const val MAX_TEXT = 100_000

        /** RFC 6266 header: an ASCII fallback plus the real (UTF-8) name for modern browsers. */
        fun attachment(fileName: String): String {
            val ascii = fileName.map { if (it.code in 0x20..0x7e && it != '"' && it != '\\') it else '_' }.joinToString("")
            val encoded = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20")
            return "attachment; filename=\"$ascii\"; filename*=UTF-8''$encoded"
        }

        /** A plain file name: no directories, no control characters, never empty. */
        fun safeName(raw: String?): String {
            val base = raw.orEmpty().substringAfterLast('/').substringAfterLast('\\')
                .filterNot { it.isISOControl() || it in "<>:\"|?*" }
                .trim().trimStart('.')
                .take(150)
            return base.ifEmpty { "file-${System.currentTimeMillis()}" }
        }
    }
}
