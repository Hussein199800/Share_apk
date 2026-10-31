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
import com.example.receive.identity.CertificateStore
import com.example.receive.identity.DeviceIdentity
import com.example.receive.server.LocalSendServer
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
 * App-wide receiver: HTTPS server + multicast discovery while the app is in the foreground and
 * receiving is enabled. During a transfer a foreground service keeps the process alive even if
 * the user leaves the app; the receiver stops once the transfer ends in the background.
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

    private val lock = Mutex()
    private var server: LocalSendServer? = null
    private var discovery: MulticastDiscovery? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var fingerprint = ""
    private var enabled = true
    private var inForeground = false

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        refreshReceived()
        scope.launch {
            sessions.state.collect { st ->
                when (st) {
                    is ReceiveState.Receiving -> ReceiveForegroundService.start(app, st.sender.alias)
                    is ReceiveState.Done -> {
                        ReceiveForegroundService.stop(app)
                        onDone(st)
                        if (!inForeground) stop()
                    }
                    ReceiveState.Idle -> {
                        ReceiveForegroundService.stop(app)
                        if (!inForeground) stop()
                    }
                    is ReceiveState.AwaitingDecision -> Unit
                }
            }
        }
    }

    fun setAlias(alias: String?) {
        _status.value = _status.value.copy(alias = alias?.takeIf { it.isNotBlank() } ?: defaultAlias())
        discovery?.announceNow()
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        if (value && inForeground) start() else if (!value) stop(force = true)
    }

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
        if (enabled) start()
        refreshReceived()
    }

    override fun onStop(owner: LifecycleOwner) {
        inForeground = false
        if (sessions.state.value !is ReceiveState.Receiving) stop()
    }

    private fun identity() = DeviceIdentity(_status.value.alias, Build.MODEL, fingerprint, _status.value.port)

    private fun start() {
        scope.launch {
            lock.withLock {
                if (server != null) return@withLock
                runCatching {
                    val cert = withContext(Dispatchers.IO) { certStore.load() }
                    fingerprint = cert.fingerprint
                    val s = LocalSendServer(cert, sessions, ::identity)
                    // Default LocalSend port; fall back to any free port if it's taken.
                    val port = runCatching { s.start(DeviceIdentity.DEFAULT_PORT) }.getOrElse { s.start(0) }
                    server = s
                    _status.value = _status.value.copy(running = true, port = port, ip = localLanIpv4(), error = null)
                    acquireMulticast()
                    discovery = MulticastDiscovery(scope, ::identity).also { it.start() }
                }.onFailure { e ->
                    _status.value = _status.value.copy(running = false, error = e.message ?: e.javaClass.simpleName)
                }
            }
        }
    }

    private fun stop(force: Boolean = false) {
        scope.launch {
            lock.withLock {
                if (!force && sessions.state.value is ReceiveState.Receiving) return@withLock
                if (force) sessions.cancelActive()
                discovery?.stop(); discovery = null
                withContext(Dispatchers.IO) { server?.stop() }; server = null
                multicastLock?.takeIf { it.isHeld }?.release(); multicastLock = null
                _status.value = _status.value.copy(running = false)
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
