package com.example.send

import com.example.data.local.HistoryRepository
import com.example.receive.discovery.NearbyDevice
import com.example.receive.discovery.PeerRegistry
import com.example.receive.identity.DeviceIdentity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

sealed interface SendState {
    data object Idle : SendState
    data class Preparing(val peer: NearbyDevice) : SendState
    data class Waiting(val peer: NearbyDevice) : SendState
    data class Sending(val peer: NearbyDevice, val sent: Long, val total: Long) : SendState
    data class Done(val peer: NearbyDevice, val count: Int) : SendState
    data class Rejected(val peer: NearbyDevice) : SendState
    data class Busy(val peer: NearbyDevice) : SendState
    data class Failed(val peer: NearbyDevice, val message: String) : SendState
}

/** App-wide sending: one transfer at a time, plus legacy subnet discovery. */
class SendController(
    private val scope: CoroutineScope,
    private val identity: () -> DeviceIdentity,
    private val onBusyChanged: () -> Unit,
    private val history: HistoryRepository? = null,
    private val client: SendClient = SendClient()
) {
    private val _state = MutableStateFlow<SendState>(SendState.Idle)
    val state: StateFlow<SendState> = _state.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private var job: Job? = null

    fun isBusy() = when (_state.value) {
        is SendState.Preparing, is SendState.Waiting, is SendState.Sending -> true
        else -> false
    }

    fun peerAlias(): String? = when (val s = _state.value) {
        is SendState.Preparing -> s.peer.info.alias
        is SendState.Waiting -> s.peer.info.alias
        is SendState.Sending -> s.peer.info.alias
        else -> null
    }

    /** Prepares files (e.g. extracts APKs) and sends them to [peer]. Ignored while another send runs. */
    @Synchronized
    fun send(peer: NearbyDevice, prepare: suspend () -> List<OutgoingFile>) {
        // Synchronized + state set before launching: a double tap can't start two sends.
        if (isBusy()) return
        _state.value = SendState.Preparing(peer)
        job = scope.launch(Dispatchers.IO) {
            val me = coroutineContext[Job]
            set(SendState.Preparing(peer))
            val files = runCatching { prepare() }.getOrElse {
                set(SendState.Failed(peer, it.message ?: "prepare failed")); return@launch
            }
            set(SendState.Waiting(peer))
            val result = try {
                client.send(peer, identity().toDto(), files) { sent, total ->
                    // Progress arrives on OkHttp threads; ignore it once this send was cancelled.
                    if (me?.isActive == true) _state.value = SendState.Sending(peer, sent, total)
                }
            } catch (e: CancellationException) {
                set(SendState.Idle); throw e
            }
            if (me?.isActive != true) return@launch
            set(
                when (result) {
                    SendResult.Sent -> SendState.Done(peer, files.size)
                    SendResult.Rejected -> SendState.Rejected(peer)
                    SendResult.Busy -> SendState.Busy(peer)
                    is SendResult.Failed -> SendState.Failed(peer, result.message)
                }
            )
            if (result == SendResult.Sent) files.forEach { f ->
                runCatching {
                    history?.logOperation(
                        actionType = "SEND",
                        appLabel = f.label,
                        packageName = f.meta?.packageName.orEmpty(),
                        versionName = f.meta?.versionName.orEmpty(),
                        fileFormat = ".${f.fileName.substringAfterLast('.', "")}",
                        fileSizeBytes = f.size,
                        sha256Hash = ""
                    )
                }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        set(SendState.Idle)
    }

    /** Leave a finished state. */
    fun reset() {
        if (!isBusy()) set(SendState.Idle)
    }

    /** Legacy HTTP discovery: ask every host of the local /24 (for routers that block multicast). */
    fun scanSubnet(ownIp: String?, registry: PeerRegistry, port: Int = DeviceIdentity.DEFAULT_PORT) {
        val prefix = ownIp?.substringBeforeLast('.', "")?.takeIf { it.isNotEmpty() } ?: return
        if (_scanning.value) return
        _scanning.value = true
        scope.launch(Dispatchers.IO) {
            val me = identity().toDto()
            val gate = Semaphore(32)
            (1..254).map { "$prefix.$it" }.filter { it != ownIp }.map { host ->
                launch { gate.withPermit { client.probe(host, port, me)?.let { registry.seen(it.info, it.ip) } } }
            }.forEach { it.join() }
            _scanning.value = false
        }
    }

    /** Resolves a typed address ("192.168.1.5" or "192.168.1.5:53317"). */
    suspend fun probeAddress(address: String): NearbyDevice? {
        val trimmed = address.trim()
        val host = trimmed.substringBefore(':')
        val port = trimmed.substringAfter(':', "").toIntOrNull() ?: DeviceIdentity.DEFAULT_PORT
        if (host.isBlank()) return null
        return client.probe(host, port, identity().toDto())
    }

    private fun set(s: SendState) {
        _state.value = s
        onBusyChanged()
    }
}
