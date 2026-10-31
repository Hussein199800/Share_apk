package com.example.wifi

import android.graphics.Bitmap
import com.example.util.FileNameAndHashUtils
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

data class HostedWifiFile(
    val index: Int,
    val appLabel: String,
    val packageName: String,
    val versionName: String,
    val file: File,
    val sizeFormatted: String,
    val sha256Short: String
)

data class WifiServerState(
    val isRunning: Boolean = false,
    val url: String = "",
    val remainingSeconds: Int = 300,
    val qrBitmap: Bitmap? = null,
    val hostedFiles: List<HostedWifiFile> = emptyList(),
    val downloadCount: Int = 0
)

class LocalWifiHttpServer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var countdownJob: Job? = null

    private val _state = MutableStateFlow(WifiServerState())
    val state: StateFlow<WifiServerState> = _state.asStateFlow()

    companion object {
        private const val AUTO_STOP_SECONDS = 300 // 5 minutes mandatory auto-stop
    }

    fun startServer(files: List<HostedWifiFile>) {
        stopServer()
        if (files.isEmpty()) return

        acceptJob = scope.launch {
            try {
                val socket = runCatching { ServerSocket(8787) }.getOrElse { ServerSocket(0) }
                serverSocket = socket
                val port = socket.localPort
                val ip = resolveLocalLanIp()
                val fullUrl = "http://$ip:$port"
                val qrBmp = QrCodeGenerator.generateQrBitmap(fullUrl, 480)

                _state.value = WifiServerState(
                    isRunning = true,
                    url = fullUrl,
                    remainingSeconds = AUTO_STOP_SECONDS,
                    qrBitmap = qrBmp,
                    hostedFiles = files,
                    downloadCount = 0
                )

                startCountdownTimer()

                while (isActive && !socket.isClosed) {
                    val client = runCatching { socket.accept() }.getOrNull() ?: break
                    launch { handleClient(client, files) }
                }
            } catch (_: Exception) {
                stopServer()
            }
        }
    }

    private fun startCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            var remaining = AUTO_STOP_SECONDS
            while (isActive && remaining > 0 && _state.value.isRunning) {
                delay(1000L)
                remaining -= 1
                _state.value = _state.value.copy(remainingSeconds = remaining)
            }
            if (remaining <= 0) {
                stopServer()
            }
        }
    }

    fun stopServer() {
        countdownJob?.cancel()
        countdownJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        acceptJob?.cancel()
        acceptJob = null
        _state.value = WifiServerState(isRunning = false)
    }

    private fun handleClient(client: Socket, files: List<HostedWifiFile>) {
        client.use { sock ->
            sock.soTimeout = 15_000
            val reader = BufferedReader(InputStreamReader(sock.getInputStream()))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            val path = if (parts.size >= 2) parts[1] else "/"

            val output = BufferedOutputStream(sock.getOutputStream())
            if (path.startsWith("/download/")) {
                val idx = path.removePrefix("/download/").substringBefore("/").toIntOrNull()
                val target = files.find { it.index == idx }
                if (target != null && target.file.exists()) {
                    _state.value = _state.value.copy(downloadCount = _state.value.downloadCount + 1)
                    val headers = buildString {
                        append("HTTP/1.1 200 OK\r\n")
                        append("Content-Type: application/vnd.android.package-archive\r\n")
                        append("Content-Length: ${target.file.length()}\r\n")
                        append("Content-Disposition: attachment; filename=\"${target.file.name}\"\r\n")
                        append("Connection: close\r\n\r\n")
                    }
                    output.write(headers.toByteArray(Charsets.UTF_8))
                    BufferedInputStream(FileInputStream(target.file)).use { fis ->
                        fis.copyTo(output, 32 * 1024)
                    }
                    output.flush()
                    return
                }
            }

            // Serve bilingual RTL/LTR HTML catalog page
            val htmlBytes = buildHtmlIndexPage(files).toByteArray(Charsets.UTF_8)
            val headers = buildString {
                append("HTTP/1.1 200 OK\r\n")
                append("Content-Type: text/html; charset=utf-8\r\n")
                append("Content-Length: ${htmlBytes.size}\r\n")
                append("Connection: close\r\n\r\n")
            }
            output.write(headers.toByteArray(Charsets.UTF_8))
            output.write(htmlBytes)
            output.flush()
        }
    }

    /** Browser page in the app's visual identity (navy/indigo, light & dark, Arabic-first). */
    private fun buildHtmlIndexPage(files: List<HostedWifiFile>): String {
        val glyph = """<svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M4 10.5 12 6.5l8 4v8L12 22.5l-8-4z"/><path d="M4 10.5l8 4 8-4M12 14.5v8"/><path d="M12 11.5V1.8M8.6 5.2 12 1.8l3.5 3.4"/></svg>"""
        val cardsHtml = files.joinToString("\n") { item ->
            val bundle = item.file.name.endsWith(".apks", ignoreCase = true)
            val initial = escapeHtml(item.appLabel.trim().take(1).ifEmpty { "?" })
            """
            <div class="card">
              <div class="row">
                <div class="ic">$initial</div>
                <div class="info">
                  <h2>${escapeHtml(item.appLabel)}</h2>
                  <p class="ltr">v${escapeHtml(item.versionName)} · ${item.sizeFormatted}</p>
                </div>
                ${if (bundle) "<span class=\"badge\">Split</span>" else ""}
              </div>
              <p class="pkg ltr">${escapeHtml(item.packageName)}</p>
              <p class="pkg ltr">SHA-256 ${item.sha256Short}…</p>
              <a class="btn" href="/download/${item.index}/${escapeHtml(item.file.name)}">تنزيل</a>
            </div>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1" />
          <meta name="color-scheme" content="light dark" />
          <title>شارك أبك</title>
          <style>
            :root { --bg:#eef1f8; --surface:#ffffff; --line:rgba(15,23,42,.09); --txt:#0f172a; --mute:#5b6577; --pri:#4338ca; --info:#0369a1; }
            @media (prefers-color-scheme: dark) { :root { --bg:#070d1d; --surface:#111c38; --line:rgba(255,255,255,.08); --txt:#eef2ff; --mute:#9aa6c8; --pri:#a5b4fc; --info:#38bdf8; } }
            * { box-sizing:border-box; }
            body { margin:0; background:var(--bg); color:var(--txt); font-family:"IBM Plex Sans Arabic",system-ui,-apple-system,"Segoe UI",Tahoma,sans-serif; line-height:1.6;
                   background-image:radial-gradient(520px 360px at 95% -8%, rgba(29,58,138,.33), transparent 62%), radial-gradient(460px 380px at -12% 40%, rgba(91,33,182,.2), transparent 62%); min-height:100vh; }
            .wrap { max-width:560px; margin:0 auto; padding:28px 16px 40px; }
            header { text-align:center; margin-bottom:22px; }
            .brand { width:72px; height:72px; margin:0 auto 10px; border-radius:23px; padding:15px; background:linear-gradient(135deg,#38bdf8,#4f46e5 55%,#a78bfa); box-shadow:0 12px 28px -10px #4f46e5; }
            h1 { margin:0; font-size:1.5rem; }
            .sub { margin:4px 0 0; color:var(--mute); font-size:.92rem; }
            .card { background:var(--surface); border:1px solid var(--line); border-radius:22px; padding:16px; margin-bottom:12px; }
            .row { display:flex; align-items:center; gap:12px; }
            .ic { width:46px; height:46px; flex:none; border-radius:14px; display:grid; place-items:center; color:#fff; font-weight:700; font-size:1.2rem; background:linear-gradient(145deg,#818cf8,#4338ca); }
            .info { flex:1; min-width:0; }
            h2 { margin:0; font-size:1.05rem; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
            p { margin:0; color:var(--mute); font-size:.85rem; }
            .pkg { font-size:.75rem; margin-top:6px; overflow-wrap:anywhere; }
            .ltr { direction:ltr; unicode-bidi:isolate; text-align:right; }
            .badge { font-size:.72rem; font-weight:600; color:var(--info); background:color-mix(in srgb, var(--info) 14%, transparent); padding:2px 9px; border-radius:9px; }
            .btn { display:block; margin-top:14px; text-align:center; text-decoration:none; color:#fff; font-weight:600; padding:13px; border-radius:16px; background:linear-gradient(135deg,#6d72f3,#4338ca); box-shadow:0 8px 20px -8px #4f46e5; }
            .help { background:var(--surface); border:1px solid var(--line); border-radius:22px; padding:14px 16px; margin-top:18px; color:var(--mute); font-size:.85rem; }
            .help b { color:var(--txt); }
            footer { margin-top:22px; text-align:center; color:var(--mute); font-size:.78rem; }
          </style>
        </head>
        <body>
          <div class="wrap">
            <header>
              <div class="brand">$glyph</div>
              <h1>شارك أبك</h1>
              <p class="sub">تطبيقات مشاركة معك عبر الشبكة المحلية</p>
            </header>
            $cardsHtml
            <div class="help"><b>بعد التنزيل:</b> افتح الملف من إشعار التنزيل لتثبيته. قد يطلب الهاتف السماح للمتصفح بتثبيت التطبيقات مرة واحدة. ملفات <span class="ltr">.apks</span> تُثبَّت عبر «شارك أبك».</div>
            <footer>شارك أبك · يعمل على الواي فاي أو نقطة الاتصال بلا إنترنت</footer>
          </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    private fun resolveLocalLanIp(): String {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return@runCatching ip
                        }
                    }
                }
            }
            interfaces.flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                ?.hostAddress ?: "127.0.0.1"
        }.getOrDefault("127.0.0.1")
    }
}
