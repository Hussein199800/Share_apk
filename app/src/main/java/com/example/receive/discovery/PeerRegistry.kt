package com.example.receive.discovery

import com.example.receive.protocol.DeviceInfoDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** A LocalSend-compatible device seen on the network. */
data class NearbyDevice(val info: DeviceInfoDto, val ip: String, val lastSeen: Long) {
    val port get() = info.port ?: DiscoveryMessages.PORT
    val scheme get() = if (info.protocol == "http") "http" else "https"
    val isShareApk get() = info.shareApk == true
}

/** Devices discovered via multicast, /register calls or subnet scan; deduplicated by fingerprint. */
class PeerRegistry(
    private val ownFingerprint: () -> String,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val _peers = MutableStateFlow<List<NearbyDevice>>(emptyList())
    val peers: StateFlow<List<NearbyDevice>> = _peers.asStateFlow()

    fun seen(info: DeviceInfoDto, ip: String) {
        if (info.fingerprint == ownFingerprint()) return
        val device = NearbyDevice(info.copy(announce = null), ip, clock())
        _peers.update { list -> (list.filterNot { it.info.fingerprint == info.fingerprint } + device).sortedBy { it.info.alias.lowercase() } }
    }

    fun clear() { _peers.value = emptyList() }
}
