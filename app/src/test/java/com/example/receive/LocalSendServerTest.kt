package com.example.receive

import com.example.receive.identity.CertificateStore
import com.example.receive.identity.DeviceIdentity
import com.example.receive.net.TrustAllHttp
import com.example.receive.protocol.DeviceInfoDto
import com.example.receive.protocol.PrepareUploadResponse
import com.example.receive.protocol.ProtocolJson
import com.example.receive.server.LocalSendServer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.concurrent.thread

class LocalSendServerTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var server: LocalSendServer
    private lateinit var manager: ReceiveSessionManager
    private var port = 0
    private val client = TrustAllHttp.client()
    private val json = "application/json".toMediaType()

    @Before fun setUp() {
        val cert = CertificateStore(tmp.newFolder()).load()
        manager = ReceiveSessionManager(tmp.newFolder(), decisionTimeoutMs = 5_000)
        server = LocalSendServer(cert, manager) { DeviceIdentity("Pixel", "Pixel 8", cert.fingerprint, port) }
        port = server.start(0)
    }

    @After fun tearDown() = server.stop()

    private fun url(path: String) = "https://127.0.0.1:$port/api/localsend/v2/$path"
    private fun post(path: String, body: String) =
        client.newCall(Request.Builder().url(url(path)).post(body.toRequestBody(json)).build()).execute()

    private val prepareBody = """{"info":{"alias":"Desk","fingerprint":"x"},
        "files":{"b":{"id":"b","fileName":"a.apk","size":5},"p":{"id":"p","fileName":"p.png","size":5}}}"""

    @Test fun registerAndInfoReturnOurIdentity() {
        val reg = post("register", """{"alias":"Desk","fingerprint":"x"}""")
        assertEquals(200, reg.code)
        assertEquals("Pixel", ProtocolJson.decode<DeviceInfoDto>(reg.body!!.string()).alias)
        val info = client.newCall(Request.Builder().url(url("info")).build()).execute()
        assertEquals("https", ProtocolJson.decode<DeviceInfoDto>(info.body!!.string()).protocol)
    }

    @Test fun fullUploadFlow() {
        thread { runBlocking { manager.state.first { it is ReceiveState.AwaitingDecision }; manager.decide(true) } }
        val prep = post("prepare-upload", prepareBody)
        assertEquals(200, prep.code)
        val resp = ProtocolJson.decode<PrepareUploadResponse>(prep.body!!.string())
        assertEquals(setOf("b"), resp.files.keys)
        val up = client.newCall(
            Request.Builder()
                .url(url("upload?sessionId=${resp.sessionId}&fileId=b&token=${resp.files["b"]}"))
                .post(byteArrayOf(1, 2, 3, 4, 5).toRequestBody("application/octet-stream".toMediaType()))
                .build()
        ).execute()
        assertEquals(200, up.code)
        assertEquals(5L, (manager.state.value as ReceiveState.Done).files.single().file.length())
    }

    @Test fun errorCodes() {
        assertEquals(400, post("prepare-upload", "{bad").code)
        assertEquals(403, post("prepare-upload", """{"info":{"alias":"D","fingerprint":"x"},"files":{"p":{"id":"p","fileName":"p.png","size":1}}}""").code)
        thread { runBlocking { manager.state.first { it is ReceiveState.AwaitingDecision }; manager.decide(false) } }
        assertEquals(403, post("prepare-upload", prepareBody).code)
        assertEquals(403, post("upload?sessionId=x&fileId=b&token=t", "abc").code)
        assertEquals(400, post("upload", "abc").code)
        assertEquals(200, post("cancel?sessionId=x", "").code)
    }
}
