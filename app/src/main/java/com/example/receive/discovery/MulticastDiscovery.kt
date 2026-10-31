package com.example.receive.discovery

import com.example.receive.identity.DeviceIdentity
import com.example.receive.net.TrustAllHttp
import com.example.receive.protocol.DeviceInfoDto
import com.example.receive.protocol.ProtocolJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket

/**
 * LocalSend multicast presence: announces this device and answers other devices' announcements
 * (HTTP register first, UDP reply as fallback), so LocalSend and ShareAPK senders list us.
 */
class MulticastDiscovery(
    private val scope: CoroutineScope,
    private val identity: () -> DeviceIdentity
) {
    private var socket: MulticastSocket? = null
    private var job: Job? = null
    private val http by lazy { TrustAllHttp.client(timeoutSeconds = 3) }
    private val group: InetAddress by lazy { InetAddress.getByName(DiscoveryMessages.GROUP) }

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.IO) {
            val s = runCatching {
                MulticastSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(DiscoveryMessages.PORT))
                    @Suppress("DEPRECATION") joinGroup(group)
                }
            }.getOrNull() ?: return@launch
            socket = s
            // Announce a few times: the first packets are often dropped while Wi-Fi wakes up.
            launch {
                repeat(3) {
                    announce(s)
                    delay(1_500)
                }
            }
            val buf = ByteArray(8 * 1024)
            while (isActive && !s.isClosed) {
                val packet = DatagramPacket(buf, buf.size)
                runCatching { s.receive(packet) }.onFailure { return@launch }
                val msg = DiscoveryMessages.parse(packet.data.copyOf(packet.length)) ?: continue
                val me = identity()
                if (DiscoveryMessages.shouldRespond(msg, me.fingerprint)) {
                    val from = packet.address
                    launch { respond(s, from, msg, me) }
                }
            }
        }
    }

    /** Re-announce (e.g. after the network changes or the alias is renamed). */
    fun announceNow() {
        val s = socket ?: return
        scope.launch(Dispatchers.IO) { announce(s) }
    }

    fun stop() {
        job?.cancel()
        job = null
        socket?.let { s ->
            @Suppress("DEPRECATION") runCatching { s.leaveGroup(group) }
            s.close()
        }
        socket = null
    }

    private fun announce(s: MulticastSocket) {
        val bytes = DiscoveryMessages.announcement(identity(), announce = true)
        runCatching { s.send(DatagramPacket(bytes, bytes.size, group, DiscoveryMessages.PORT)) }
    }

    private fun respond(s: MulticastSocket, from: InetAddress, msg: DeviceInfoDto, me: DeviceIdentity) {
        val scheme = if (msg.protocol == "http") "http" else "https"
        val port = msg.port ?: DiscoveryMessages.PORT
        val host = from.hostAddress?.let { if (it.contains(':')) "[$it]" else it } ?: return
        val registered = runCatching {
            val req = Request.Builder()
                .url("$scheme://$host:$port/api/localsend/v2/register")
                .post(ProtocolJson.encode(me.toDto()).toRequestBody("application/json".toMediaType()))
                .build()
            http.newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)
        if (!registered) {
            val bytes = DiscoveryMessages.announcement(me, announce = false)
            runCatching { s.send(DatagramPacket(bytes, bytes.size, group, DiscoveryMessages.PORT)) }
        }
    }
}
