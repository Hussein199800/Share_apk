package com.example.web

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.model.InstalledAppInfo
import com.example.receive.ReceivedEntry
import com.example.receive.ReceivedItem
import com.example.receive.localLanIpv4
import com.example.send.PickedItem
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class WebShareState(
    val running: Boolean = false,
    val url: String = "",
    val pin: String = "",
    val qr: Bitmap? = null,
    val transfers: Int = 0,
    /** Number of files/texts received from the browser since the page was opened. */
    val receivedCount: Int = 0,
    val error: String? = null
)

/**
 * "From browser": serves [WebPage] so any computer on the network can download what the app
 * holds and upload files/text to the phone, gated by a 4-digit PIN.
 */
class WebShare(
    private val context: Context,
    private val scope: CoroutineScope,
    private val receivedApps: () -> List<ReceivedItem>,
    private val receivedFiles: () -> List<ReceivedEntry>,
    private val onUpload: suspend (File, String) -> Unit,
    private val onText: suspend (String) -> Unit,
    private val onBusyChanged: () -> Unit
) {
    /** Installed apps, supplied by the app list screen once it has loaded them. */
    @Volatile var installedApps: () -> List<InstalledAppInfo> = { emptyList() }

    /** Files, folders and texts chosen in the Files tab (or from the sheet), offered to the browser. */
    @Volatile var pickedFiles: () -> List<PickedItem> = { emptyList() }

    private val _state = MutableStateFlow(WebShareState())
    val state: StateFlow<WebShareState> = _state.asStateFlow()

    private var server: WebShareServer? = null
    private var watchdog: Job? = null
    @Volatile private var lastActivity = 0L

    private val icons = ConcurrentHashMap<String, Bitmap>()
    @Volatile private var snapshot: List<WebItem> = emptyList()
    @Volatile private var snapshotAt = 0L

    val isBusy get() = _state.value.transfers > 0

    fun start() {
        if (server != null) return
        scope.launch(Dispatchers.IO) {
            runCatching {
                val gate = PinGate(onPinChanged = { pin -> _state.update { it.copy(pin = pin) } })
                val s = WebShareServer(
                    gate, source, File(context.cacheDir, "web-upload"),
                    onActivity = { lastActivity = System.currentTimeMillis() },
                    onTransfersChanged = { n -> _state.update { it.copy(transfers = n) }; onBusyChanged() }
                )
                val port = runCatching { s.start(PORT) }.getOrElse { s.start(0) }
                server = s
                val url = "http://${localLanIpv4() ?: "127.0.0.1"}:$port"
                lastActivity = System.currentTimeMillis()
                _state.value = WebShareState(running = true, url = url, pin = gate.pin, qr = QrCodeGenerator.generateQrBitmap(url, 480))
                startWatchdog()
            }.onFailure { e -> _state.value = WebShareState(error = e.message ?: e.javaClass.simpleName) }
        }
    }

    fun stop() {
        watchdog?.cancel(); watchdog = null
        val s = server ?: return
        server = null
        scope.launch(Dispatchers.IO) { s.stop() }
        _state.value = WebShareState()
        icons.clear(); snapshot = emptyList()
        onBusyChanged()
    }

    private fun startWatchdog() {
        watchdog?.cancel()
        watchdog = scope.launch {
            while (true) {
                delay(30_000)
                if (!isBusy && System.currentTimeMillis() - lastActivity > IDLE_MS) { stop(); break }
            }
        }
    }

    private val source = object : WebShareSource {
        override fun items(): List<WebItem> {
            val now = System.currentTimeMillis()
            if (now - snapshotAt < 2_000 && snapshot.isNotEmpty()) return snapshot
            return build().also { snapshot = it; snapshotAt = now }
        }

        override fun iconPng(id: String): ByteArray? {
            val bmp = icons[id] ?: return null
            return ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        }

        override suspend fun acceptUpload(tmp: File, name: String) {
            onUpload(tmp, name)
            snapshotAt = 0
            _state.update { it.copy(receivedCount = it.receivedCount + 1) }
        }

        override suspend fun acceptText(text: String) {
            onText(text)
            _state.update { it.copy(receivedCount = it.receivedCount + 1) }
        }
    }

    private fun build(): List<WebItem> {
        val out = mutableListOf<WebItem>()
        pickedFiles().forEach { p ->
            val id = "p-${p.id}"
            out += if (p.isText) {
                WebItem(id, WebItem.Kind.TEXT, p.text!!.lineSequence().first().take(80), "", 0, "text/plain", "text.txt", text = p.text)
            } else {
                val leaf = p.name.substringAfterLast('/')
                WebItem(id, WebItem.Kind.FILE, leaf, p.name.substringBeforeLast('/', "").ifEmpty { p.mimeType.orEmpty() }, p.size,
                    p.mimeType ?: "application/octet-stream", leaf,
                    open = { p.uri?.let { u -> runCatching { context.contentResolver.openInputStream(u) }.getOrNull() } })
            }
        }
        installedApps().filterNot { it.isSystemApp }.sortedBy { it.label.lowercase() }.forEach { app ->
            val id = "a-${app.packageName}"
            app.iconBitmap?.let { icons[id] = it }
            val ext = if (app.isSplitApk) "apks" else "apk"
            val fileName = "${app.label}_${app.versionName}.$ext".replace('/', '_')
            out += if (app.isSplitApk) {
                WebItem(id, WebItem.Kind.APP, app.label, "${app.packageName} · v${app.versionName}", app.totalSizeBytes,
                    "application/octet-stream", fileName, hasIcon = app.iconBitmap != null, write = { os -> writeBundle(app, os) })
            } else {
                WebItem(id, WebItem.Kind.APP, app.label, "${app.packageName} · v${app.versionName}", File(app.baseApkPath).length(),
                    APK_MIME, fileName, hasIcon = app.iconBitmap != null, open = { File(app.baseApkPath).takeIf { it.canRead() }?.inputStream() })
            }
        }
        receivedApps().forEachIndexed { i, item ->
            val id = "r-$i-${item.file.name.hashCode().toUInt()}"
            item.icon?.let { icons[id] = it }
            out += WebItem(id, WebItem.Kind.RECEIVED_APP, item.label, listOfNotNull(item.packageName, item.versionName?.let { "v$it" }).joinToString(" · "),
                item.file.length(), if (item.file.extension.equals("apk", true)) APK_MIME else "application/octet-stream",
                item.file.name, hasIcon = item.icon != null, open = { item.file.takeIf { it.exists() }?.inputStream() })
        }
        receivedFiles().filter { !it.isText && it.uri != null }.forEachIndexed { i, e ->
            out += WebItem("f-$i-${e.uri.hashCode().toUInt()}", WebItem.Kind.FILE, e.name, e.mimeType.orEmpty(), e.size,
                e.mimeType ?: "application/octet-stream", e.name,
                open = { runCatching { context.contentResolver.openInputStream(Uri.parse(e.uri)) }.getOrNull() })
        }
        return out
    }

    /** Streams base + splits as an .apks archive (same layout as the app's own export). */
    private fun writeBundle(app: InstalledAppInfo, os: java.io.OutputStream) {
        val zos = ZipOutputStream(os)
        fun add(file: File, name: String) {
            if (!file.canRead()) return
            zos.putNextEntry(ZipEntry(name).apply { time = file.lastModified() })
            BufferedInputStream(file.inputStream()).use { it.copyTo(zos, 64 * 1024) }
            zos.closeEntry()
        }
        add(File(app.baseApkPath), "base.apk")
        app.splitSourceDirs.forEachIndexed { i, path ->
            val f = File(path)
            add(f, if (f.name.endsWith(".apk") && f.name != "base.apk") f.name else "split_config_$i.apk")
        }
        zos.finish()
    }

    companion object {
        const val PORT = 53320
        const val IDLE_MS = 15 * 60_000L
        private const val APK_MIME = "application/vnd.android.package-archive"
    }
}
