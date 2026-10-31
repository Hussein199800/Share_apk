package com.example.send

import com.example.receive.ReceiveSessionManager
import com.example.receive.ReceiveState
import com.example.receive.discovery.NearbyDevice
import com.example.receive.discovery.PeerRegistry
import com.example.receive.identity.CertificateStore
import com.example.receive.identity.DeviceIdentity
import com.example.receive.protocol.DeviceInfoDto
import com.example.receive.protocol.ShareApkMetaDto
import com.example.receive.server.LocalSendServer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.concurrent.thread

class SendFlowTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var server: LocalSendServer
    private lateinit var receiver: ReceiveSessionManager
    private val registry = PeerRegistry({ "receiver-fp" })
    private var port = 0

    @Before fun setUp() {
        val cert = CertificateStore(tmp.newFolder()).load()
        receiver = ReceiveSessionManager(tmp.newFolder(), decisionTimeoutMs = 5_000)
        server = LocalSendServer(cert, receiver, onPeer = registry::seen) { DeviceIdentity("Receiver", "Pixel", cert.fingerprint, port) }
        port = server.start(0)
    }

    @After fun tearDown() = server.stop()

    private val me = DeviceInfoDto(alias = "Sender", fingerprint = "sender-fp", port = 53317, protocol = "https")
    private fun peer(protocol: String = "https") =
        NearbyDevice(DeviceInfoDto(alias = "Receiver", fingerprint = "receiver-fp", port = port, protocol = protocol), "127.0.0.1", 0)

    private fun autoDecide(accept: Boolean) = thread {
        runBlocking { receiver.state.first { it is ReceiveState.AwaitingDecision }; receiver.decide(accept) }
    }

    @Test fun acceptedSendDeliversIdenticalFile() = runBlocking {
        val bytes = ByteArray(700_000) { (it * 7).toByte() }
        val src = tmp.newFile("Sakina.apk").apply { writeBytes(bytes) }
        autoDecide(true)
        var lastProgress = 0L
        val result = SendClient().send(peer(), me, listOf(OutgoingFile(src, "Sakina.apk", ShareApkMetaDto("com.sakina", "1.3.0", 130)))) { sent, _ -> lastProgress = sent }
        assertEquals(SendResult.Sent, result)
        assertEquals(bytes.size.toLong(), lastProgress)
        val got = (receiver.state.value as ReceiveState.Done).files.single()
        assertArrayEquals(bytes, got.file.readBytes())
        assertEquals("com.sakina", got.meta?.packageName)
    }

    @Test fun plainHttpPeerWorks() = runBlocking {
        val src = tmp.newFile("a.apk").apply { writeBytes(ByteArray(10)) }
        autoDecide(true)
        assertEquals(SendResult.Sent, SendClient().send(peer("http"), me, listOf(OutgoingFile(src, "a.apk", null))) { _, _ -> })
    }

    @Test fun declinedSendIsRejected() = runBlocking {
        val src = tmp.newFile("a.apk").apply { writeBytes(ByteArray(10)) }
        autoDecide(false)
        assertEquals(SendResult.Rejected, SendClient().send(peer(), me, listOf(OutgoingFile(src, "a.apk", null))) { _, _ -> })
    }

    @Test fun registerRecordsPeerAndProbeFindsServer() = runBlocking {
        val found = SendClient().probe("127.0.0.1", port, me)
        assertEquals("Receiver", found?.info?.alias)
        // The receiver's /register handler saw us as a peer.
        assertTrue(registry.peers.value.any { it.info.alias == "Sender" })
    }

    @Test fun registryDedupesByFingerprintAndIgnoresSelf() {
        val r = PeerRegistry({ "me" })
        r.seen(DeviceInfoDto(alias = "A", fingerprint = "x"), "10.0.0.2")
        r.seen(DeviceInfoDto(alias = "A2", fingerprint = "x"), "10.0.0.3")
        r.seen(DeviceInfoDto(alias = "Me", fingerprint = "me"), "10.0.0.4")
        assertEquals(listOf("A2" to "10.0.0.3"), r.peers.value.map { it.info.alias to it.ip })
    }
}
