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

    @Volatile private var fingerprint = ""
    val peers = PeerRegistry({ fingerprint })
    val sender = SendController(scope, ::identity, onBusyChanged = { refreshService() }, history = history)

    private val lock = Mutex()
    private var server: LocalSendServer? = null
    private var discovery: MulticastDiscovery? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    @Volatile private var enabled = true
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
        scope.launch(Dispatchers.IO) { _received.value = library.list() }
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

    private fun busy() = sessions.state.value is ReceiveState.Receiving || sender.isBusy()

    private fun refreshService() {
        val receiving = sessions.state.value as? ReceiveState.Receiving
        when {
            receiving != null -> ReceiveForegroundService.start(app, receiving.sender.alias)
            sender.isBusy() -> ReceiveForegroundService.start(app, sender.peerAlias().orEmpty())
            else -> ReceiveForegroundService.stop(app)
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
        if (server != null) return@withLock
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
        if (force) sessions.cancelActive()
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
            done.files.forEach { rf ->
                rf.meta?.let { library.saveMeta(rf.file, it) }
                val item = library.inspect(rf.file)
                history.logOperation(
                    actionType = "RECEIVE",
                    appLabel = item.label,
                    packageName = item.packageName.orEmpty(),
                    versionName = item.versionName.orEmpty(),
                    fileFormat = ".${rf.file.extension}",
                    fileSizeBytes = rf.file.length(),
                    sha256Hash = runCatching { FileNameAndHashUtils.computeFileSha256(rf.file) }.getOrDefault("")
                )
            }
            _received.value = library.list()
        }
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
