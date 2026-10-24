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

    private fun buildHtmlIndexPage(files: List<HostedWifiFile>): String {
        val cardsHtml = files.joinToString("\n") { item ->
            val badge = if (item.file.name.endsWith(".apks")) "Split APKS Bundle (.apks)" else "Single APK (.apk)"
            """
            <div class="card">
              <div class="info">
                <h2>${escapeHtml(item.appLabel)}</h2>
                <p class="ltr">${escapeHtml(item.packageName)} • v${escapeHtml(item.versionName)}</p>
                <p class="meta"><span class="badge">$badge</span> • <span class="ltr">${item.sizeFormatted}</span></p>
                <p class="hash ltr">SHA-256: ${item.sha256Short}...</p>
              </div>
              <a class="btn" href="/download/${item.index}/${item.file.name}">تنزيل / Download</a>
            </div>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1" />
          <title>شارك أبك | ShareAPK Wi-Fi Transfer</title>
          <style>
            body { background:#2B1B14; color:#FFF8F0; font-family:system-ui,-apple-system,sans-serif; margin:0; padding:24px; }
            .container { max-width:680px; margin:0 auto; }
            header { border-bottom:1px solid rgba(215,204,200,0.25); padding-bottom:16px; margin-bottom:20px; }
            h1 { color:#C9A227; margin:0 0 6px 0; font-size:1.5rem; }
            .sub { color:#D7CCC8; font-size:0.9rem; margin:0; }
            .card { background:#38241C; border:1px solid rgba(201,162,39,0.35); border-radius:16px; padding:16px; margin-bottom:14px; display:flex; flex-wrap:wrap; align-items:center; justify-content:space-between; gap:12px; }
            h2 { margin:0 0 4px 0; font-size:1.15rem; color:#FFF8F0; }
            p { margin:2px 0; color:#D7CCC8; font-size:0.86rem; }
            .ltr { direction:ltr; unicode-bidi:embed; font-family:monospace; }
            .badge { background:#4E342E; color:#C9A227; padding:2px 8px; border-radius:6px; font-weight:600; }
            .hash { font-size:0.75rem; opacity:0.8; }
            .btn { background:#C9A227; color:#2B1B14; text-decoration:none; font-weight:700; padding:10px 18px; border-radius:10px; display:inline-block; }
            footer { margin-top:28px; text-align:center; color:#D7CCC8; font-size:0.8rem; opacity:0.75; }
          </style>
        </head>
        <body>
          <div class="container">
            <header>
              <h1>شارك أبك | ShareAPK — خادم Wi-Fi المحلي</h1>
              <p class="sub">100% Offline Local LAN Transfer • المطور: حسين محمد علي الطويل</p>
            </header>
            $cardsHtml
            <footer>ShareAPK v2.4.0 • Works 100% Offline on Local Wi-Fi / Hotspot</footer>
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
