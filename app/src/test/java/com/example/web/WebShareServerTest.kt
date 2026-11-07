package com.example.web

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PinGateTest {
    @Test fun rightPinGivesValidToken() {
        val gate = PinGate()
        val token = gate.login(gate.pin)
        assertNotNull(token)
        assertTrue(gate.isValid(token))
        assertTrue(!gate.isValid("nope") && !gate.isValid(null))
    }

    @Test fun pinUsesAsciiDigitsEvenInArabicLocale() {
        val old = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("ar-EG"))
            val gate = PinGate()
            assertTrue(gate.pin.all { it in '0'..'9' })
            val arabic = gate.pin.map { '\u0660' + (it - '0') }.joinToString("")
            assertNotNull(gate.login(arabic))
            assertNotNull(gate.login(gate.pin))
        } finally {
            java.util.Locale.setDefault(old)
        }
    }

    @Test fun pinRotatesAfterFiveWrongTries() {
        var changed: String? = null
        val gate = PinGate(onPinChanged = { changed = it })
        val first = gate.pin
        val wrong = if (first == "0000") "1111" else "0000"
        repeat(4) { assertNull(gate.login(wrong)) }
        assertNull(changed)
        gate.login(wrong)
        assertNotNull(changed)
        assertEquals(4, gate.pin.length)
    }
}

class WebShareServerTest {
    @get:Rule val tmp = TemporaryFolder()
    private val http = OkHttpClient.Builder().followRedirects(false).build()
    private lateinit var server: WebShareServer
    private lateinit var gate: PinGate
    private var port = 0
    private val uploads = mutableListOf<Pair<String, String>>()
    private val texts = mutableListOf<String>()

    @Before fun setUp() {
        gate = PinGate()
        val source = object : WebShareSource {
            override fun items() = listOf(
                WebItem("x1", WebItem.Kind.FILE, "ملف.txt", "text/plain", 5, "text/plain", "ملف.txt", open = { "hello".byteInputStream() })
            )
            override fun iconPng(id: String): ByteArray? = null
            override suspend fun acceptUpload(tmp: File, name: String) { uploads += name to tmp.readText(); tmp.delete() }
            override suspend fun acceptText(text: String) { texts += text }
        }
        server = WebShareServer(gate, source, tmp.newFolder("up"))
        port = server.start(0)
    }

    @After fun tearDown() = server.stop()

    private fun url(path: String) = "http://127.0.0.1:$port$path"

    private fun login(pin: String = gate.pin): String? =
        http.newCall(Request.Builder().url(url("/api/login")).post(pin.toRequestBody()).build()).execute().use { r ->
            r.header("Set-Cookie")?.substringBefore(';')
        }

    @Test fun pageIsPublicButApiNeedsPin() {
        http.newCall(Request.Builder().url(url("/")).build()).execute().use { assertEquals(200, it.code) }
        http.newCall(Request.Builder().url(url("/api/items")).build()).execute().use { assertEquals(401, it.code) }
        http.newCall(Request.Builder().url(url("/dl/x1")).build()).execute().use { assertEquals(401, it.code) }
        assertNull(login(if (gate.pin == "0000") "1111" else "0000"))
    }

    @Test fun downloadUploadAndTextWithSession() {
        val cookie = login()!!
        fun req(path: String) = Request.Builder().url(url(path)).header("Cookie", cookie)
        http.newCall(req("/api/items").build()).execute().use { r ->
            assertEquals(200, r.code)
            assertTrue(r.body!!.string().contains("\"id\":\"x1\""))
        }
        http.newCall(req("/dl/x1").build()).execute().use { r ->
            assertEquals("hello", r.body!!.string())
            assertTrue(r.header("Content-Disposition")!!.contains("filename*=UTF-8''"))
        }
        http.newCall(req("/dl/missing").build()).execute().use { assertEquals(404, it.code) }
        http.newCall(req("/api/upload?name=..%2F..%2Fevil.txt").post("data".toRequestBody()).build()).execute().use { assertEquals(204, it.code) }
        assertEquals(listOf("evil.txt" to "data"), uploads)
        http.newCall(req("/api/text").post("مرحبا".toRequestBody()).build()).execute().use { assertEquals(204, it.code) }
        assertEquals(listOf("مرحبا"), texts)
    }

    @Test fun safeNameStripsPathsAndHiddenPrefix() {
        assertEquals("a.apk", WebShareServer.safeName("C:\\x\\a.apk"))
        assertEquals("passwd", WebShareServer.safeName("../../etc/.passwd").let { it })
        assertNotEquals("", WebShareServer.safeName(""))
    }
}
