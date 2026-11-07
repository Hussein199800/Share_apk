package com.example.receive

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.data.local.AppDatabase
import com.example.data.local.HistoryRepository
import com.example.receive.discovery.MulticastDiscovery
import com.example.receive.discovery.NearbyDevice
import com.example.receive.discovery.PeerRegistry
import com.example.receive.identity.CertificateStore
import com.example.receive.identity.DeviceIdentity
import com.example.receive.server.LocalSendServer
import com.example.send.SendController
import com.example.service.ReceiveForegroundService
import com.example.util.FileNameAndHashUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** What the Receive tab shows about this device. */
data class ReceiverStatus(
    val running: Boolean = false,
    val alias: String = "",
    val ip: String? = null,
    val port: Int = 0,
    val error: String? = null
)

/**
 * App-wide LocalSend networking:
 * - discovery (multicast listen + peer registry) whenever the app is in the foreground, for sending;
 * - the HTTPS/HTTP receive server while receiving is enabled;
 * - a foreground service while a transfer (either direction) runs, so it survives leaving the app.
 */
class ReceiveController private constructor(private val app: Context) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val certStore = CertificateStore(app.filesDir)
    val library = ReceivedLibrary(app)
    val sessions = ReceiveSessionManager(library.root)
    private val history = HistoryRepository(AppDatabase.getInstance(app).historyDao())

    private val _status = MutableStateFlow(ReceiverStatus(alias = defaultAlias()))
    val status: StateFlow<ReceiverStatus> = _status.asStateFlow()

    private val _received = MutableStateFlow<List<ReceivedItem>>(emptyList())
    val received: StateFlow<List<ReceivedItem>> = _received.asStateFlow()

    private val filesIndex = ReceivedFilesIndex(app)
    private val _receivedOther = MutableStateFlow<List<ReceivedEntry>>(emptyList())
    /** Received non-app files and text messages, newest first. */
    val receivedOther: StateFlow<List<ReceivedEntry>> = _receivedOther.asStateFlow()

    fun removeReceivedEntry(entry: ReceivedEntry) {
        scope.launch(Dispatchers.IO) { _receivedOther.value = filesIndex.remove(entry) }
    }

    @Volatile private var fingerprint = ""
    val peers = PeerRegistry({ fingerprint })
    val sender = SendController(scope, ::identity, onBusyChanged = { refreshService() }, history = history, client = com.example.send.SendClient(com.example.send.PrefsPins(app)))

    /** "From browser" page: downloads and uploads for any computer on the network. */
    val web = com.example.web.WebShare(
        app, scope,
        receivedApps = { _received.value },
        receivedFiles = { _receivedOther.value },
        onUpload = ::ingestWebUpload,
        onText = { text -> _receivedOther.value = withContext(Dispatchers.IO) { filesIndex.add(listOf(ReceivedEntry(name = "", text = text))) } },
        onBusyChanged = { refreshService() }
    )

    /** Raw errors (masked) for the developer: More → Diagnostics log. */
    val diagnostics = com.example.util.DiagnosticsLog(File(app.filesDir, "diagnostics.log"))

    private val lock = Mutex()
    private var server: LocalSendServer? = null
    private var discovery: MulticastDiscovery? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    // Off until the saved preference arrives, so a disabled receiver never starts briefly.
    @Volatile private var enabled = false
    @Volatile private var serviceRunning = false
    @Volatile private var inForeground = false

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        refreshReceived()
        scope.launch {
            sessions.state.collect { st ->
                refreshService()
                when (st) {
                    is ReceiveState.Done -> { onDone(st); if (!inForeground) stopAll() }
                    ReceiveState.Idle -> if (!inForeground) stopAll()
                    else -> Unit
                }
            }
        }
        scope.launch { sender.state.collect { if (!inForeground && !busy()) stopAll() } }
        scope.launch(Dispatchers.IO) {
            sender.state.collect { st -> if (st is com.example.send.SendState.Failed) diagnostics.add("send", st.message) }
        }
        scope.launch(Dispatchers.IO) {
            status.collect { st -> st.error?.let { diagnostics.add("receive-server", it) } }
        }
        scope.launch(Dispatchers.IO) {
            web.state.collect { st -> st.error?.let { diagnostics.add("browser", it) } }
        }
        // Watchdog: an accepted transfer with no upload activity for a minute is cancelled.
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(15_000)
                sessions.expireIfIdle()
            }
        }
    }

    fun setAlias(alias: String?) {
        _status.value = _status.value.copy(alias = alias?.takeIf { it.isNotBlank() } ?: defaultAlias())
        if (enabled) discovery?.announceNow()
    }

    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        scope.launch {
            if (value && inForeground) startServer() else if (!value) stopServer(force = true)
        }
    }

    /** Clears the list and asks the network again; falls back to a subnet scan when nothing answers. */
    fun refreshPeers() {
        peers.clear()
        discovery?.announceNow()
        sender.scanSubnet(_status.value.ip ?: localLanIpv4(), peers)
    }

    /** Adds a device typed in by the user (IP or IP:port). Returns false if nothing answered. */
    suspend fun addManualPeer(address: String): NearbyDevice? = sender.probeAddress(address)?.also { peers.seen(it.info, it.ip) }

    fun refreshReceived() {
        scope.launch(Dispatchers.IO) {
            _received.value = library.list()
            _receivedOther.value = filesIndex.load()
        }
    }

    fun deleteReceived(item: ReceivedItem) {
        scope.launch(Dispatchers.IO) {
            library.delete(item)
            _received.value = library.list()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        inForeground = true
        scope.launch {
            ensureIdentity()
            startDiscovery()
            if (enabled) startServer()
        }
        refreshReceived()
    }

    override fun onStop(owner: LifecycleOwner) {
        inForeground = false
        if (!busy()) stopAll()
    }

    private fun busy() = sessions.state.value is ReceiveState.Receiving || sender.isBusy() || web.isBusy

    /** Starts the keep-alive service once per transfer (not on every progress update), stops it after. */
    @Synchronized
    private fun refreshService() {
        val receiving = sessions.state.value as? ReceiveState.Receiving
        val alias = receiving?.sender?.alias ?: sender.peerAlias() ?: web.state.value.url.takeIf { web.isBusy }
        val needed = receiving != null || sender.isBusy() || web.isBusy
        when {
            needed && !serviceRunning -> {
                serviceRunning = true
                ReceiveForegroundService.start(app, alias.orEmpty())
            }
            !needed && serviceRunning -> {
                serviceRunning = false
                ReceiveForegroundService.stop(app)
            }
        }
    }

    fun identity() = DeviceIdentity(_status.value.alias, Build.MODEL, fingerprint, _status.value.port.takeIf { it > 0 } ?: DeviceIdentity.DEFAULT_PORT)

    private suspend fun ensureIdentity() {
        if (fingerprint.isNotEmpty()) return
        runCatching { withContext(Dispatchers.IO) { certStore.load() } }
            .onSuccess { fingerprint = it.fingerprint }
            .onFailure { e -> _status.value = _status.value.copy(error = e.message ?: e.javaClass.simpleName) }
    }

    private suspend fun startDiscovery() = lock.withLock {
        if (discovery != null) return@withLock
        acquireMulticast()
        discovery = MulticastDiscovery(scope, ::identity, onPeer = peers::seen, visible = { enabled && server != null })
            .also { it.start() }
    }

    private suspend fun startServer() = lock.withLock {
        if (server != null || !enabled || !inForeground) return@withLock
        runCatching {
            val cert = withContext(Dispatchers.IO) { certStore.load() }
            fingerprint = cert.fingerprint
            val s = LocalSendServer(cert, sessions, onPeer = peers::seen, identity = ::identity)
            // Default LocalSend port; fall back to any free port if it's taken.
            val port = runCatching { s.start(DeviceIdentity.DEFAULT_PORT) }.getOrElse { s.start(0) }
            server = s
            _status.value = _status.value.copy(running = true, port = port, ip = localLanIpv4(), error = null)
        }.onFailure { e ->
            _status.value = _status.value.copy(running = false, error = e.message ?: e.javaClass.simpleName)
        }
        discovery?.announceNow()
    }

    private suspend fun stopServer(force: Boolean) = lock.withLock {
        if (!force && sessions.state.value is ReceiveState.Receiving) return@withLock
        // A request waiting for the user can't be answered once the server is gone: decline it.
        if (force || sessions.state.value is ReceiveState.AwaitingDecision) sessions.cancelActive()
        withContext(Dispatchers.IO) { server?.stop() }
        server = null
        _status.value = _status.value.copy(running = false)
    }

    private fun stopAll() {
        scope.launch {
            if (busy()) return@launch
            stopServer(force = false)
            lock.withLock {
                discovery?.stop(); discovery = null
                multicastLock?.takeIf { it.isHeld }?.release(); multicastLock = null
            }
        }
    }

    private fun acquireMulticast() {
        val wifi = app.getSystemService(WifiManager::class.java) ?: return
        multicastLock = wifi.createMulticastLock("shareapk-localsend").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun onDone(done: ReceiveState.Done) {
        scope.launch(Dispatchers.IO) {
            val others = mutableListOf<ReceivedEntry>()
            done.messages.forEach { others += ReceivedEntry(name = "", text = it) }
            done.files.filterNot { it.isApp }.forEach { rf ->
                saveToDownloads(rf.file, rf.fileName, rf.mimeType, rf.relativeDir)?.let { others += it }
                rf.file.parentFile?.takeIf { it.listFiles().isNullOrEmpty() }?.delete()
            }
            if (others.isNotEmpty()) _receivedOther.value = filesIndex.add(others)
            done.files.filter { it.isApp }.forEach { rf ->
                rf.meta?.let { library.saveMeta(rf.file, it) }
                logReceivedApp(rf.file)
            }
            _received.value = library.list()
        }
    }

    /** A file uploaded from the browser page: apps join the received library, the rest go to Downloads. */
    private suspend fun ingestWebUpload(tmp: File, name: String) = withContext(Dispatchers.IO) {
        if (ReceiveSessionManager.isAppPackage(name)) {
            val dir = File(library.root, "web").apply { mkdirs() }
            var target = File(dir, name)
            var n = 1
            while (target.exists()) target = File(dir, "${name.substringBeforeLast('.')} ($n).${name.substringAfterLast('.')}").also { n++ }
            if (!tmp.renameTo(target)) { tmp.copyTo(target, overwrite = true); tmp.delete() }
            logReceivedApp(target)
            _received.value = library.list()
        } else {
            saveToDownloads(tmp, name, null, "")?.let { _receivedOther.value = filesIndex.add(listOf(it)) }
            tmp.delete()
        }
    }

    /** Non-app files go to Download/ShareAPK so the gallery and file managers see them. */
    private suspend fun saveToDownloads(file: File, name: String, declaredMime: String?, relativeDir: String): ReceivedEntry? {
        val mime = DownloadsSaver.mimeFor(name, declaredMime)
        val size = file.length()
        val uri = DownloadsSaver.save(app, file, name, mime, relativeDir) ?: return null
        file.delete()
        runCatching { history.logOperation("RECEIVE", name, "", "", ".${file.extension}", size, "") }
        return ReceivedEntry(name, uri.toString(), mime, size)
    }

    private suspend fun logReceivedApp(file: File) {
        val item = library.inspect(file)
        history.logOperation(
            actionType = "RECEIVE",
            appLabel = item.label,
            packageName = item.packageName.orEmpty(),
            versionName = item.versionName.orEmpty(),
            fileFormat = ".${file.extension}",
            fileSizeBytes = file.length(),
            sha256Hash = runCatching { FileNameAndHashUtils.computeFileSha256(file) }.getOrDefault("")
        )
    }

    private fun defaultAlias() = Build.MODEL?.takeIf { it.isNotBlank() } ?: "Android"

    companion object {
        @Volatile private var instance: ReceiveController? = null

        fun get(context: Context): ReceiveController =
            instance ?: synchronized(this) {
                instance ?: ReceiveController(context.applicationContext).also { instance = it }
            }
    }
}
