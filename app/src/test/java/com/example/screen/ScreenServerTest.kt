package com.example.screen

import com.example.web.PinGate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class ScreenProtocolTest {
    @Test fun helloAndReplyRoundTrip() {
        val h = ScreenProtocol.parseHello(ScreenProtocol.hello("0420", "هاتف أحمد"))!!
        assertEquals("0420", h.pin); assertEquals("هاتف أحمد", h.viewerName)
        assertNull(ScreenProtocol.parseHello("GET / HTTP/1.1"))
        assertEquals(720 to 1600, ScreenProtocol.parseReply("OK 720 1600"))
        assertNull(ScreenProtocol.parseReply(ScreenProtocol.NO))
        assertNull(ScreenProtocol.parseReply("OK 1 99999"))
    }

    @Test fun eventsRoundTripAndRejectGarbage() {
        listOf(ScreenEvent.Point(0.25f, 0.5f), ScreenEvent.Draw(1f, 0f), ScreenEvent.End).forEach {
            assertEquals(it, ScreenEvent.decode(it.encode()))
        }
        assertNull(ScreenEvent.decode("P 2 0.5"))
        assertNull(ScreenEvent.decode("X 0.1 0.1"))
        assertEquals("P 0.5000 0.2500", ScreenEvent.Point(0.5f, 0.25f).encode())
    }
}

class ScreenServerTest {
    private lateinit var gate: PinGate
    private lateinit var server: ScreenServer
    private var port = 0
    private val events = CopyOnWriteArrayList<ScreenEvent>()
    private val viewers = CopyOnWriteArrayList<String?>()

    @Before fun setUp() {
        gate = PinGate()
        server = ScreenServer(gate, { 720 to 1600 }, { viewers += it }, { events += it }, {})
        port = server.start(0)
    }

    @After fun tearDown() = server.stop()

    private fun connect(pin: String): Pair<Socket, String?> {
        val s = Socket("127.0.0.1", port).apply { soTimeout = 3000 }
        s.getOutputStream().write((ScreenProtocol.hello(pin, "helper") + "\n").toByteArray())
        val sb = StringBuilder()
        while (true) { val b = s.getInputStream().read(); if (b < 0 || b == '\n'.code) break; sb.append(b.toChar()) }
        return s to sb.toString()
    }

    private fun waitFor(cond: () -> Boolean) {
        val end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
        while (!cond() && System.nanoTime() < end) Thread.sleep(20)
        assertTrue(cond())
    }

    @Test fun wrongPinIsRefused() {
        val wrong = if (gate.pin == "0000") "1111" else "0000"
        val (s, reply) = connect(wrong)
        s.close()
        assertEquals(ScreenProtocol.NO, reply)
    }

    @Test fun viewerGetsConfigThenKeyFramesAndSendsEvents() {
        server.offer(ScreenProtocol.Frame(0, ScreenProtocol.FLAG_CONFIG, byteArrayOf(1, 2)))
        val (s, reply) = connect(gate.pin)
        assertEquals("OK 720 1600", reply)
        waitFor { viewers.contains("helper") }
        server.offer(ScreenProtocol.Frame(10, 0, byteArrayOf(9)))                       // skipped: not a key frame
        server.offer(ScreenProtocol.Frame(20, ScreenProtocol.FLAG_KEY, byteArrayOf(7, 7)))
        server.offer(ScreenProtocol.Frame(30, 0, byteArrayOf(8)))
        val input = DataInputStream(BufferedInputStream(s.getInputStream()))
        val f1 = ScreenProtocol.readFrame(input)!!; assertTrue(f1.isConfig)
        val f2 = ScreenProtocol.readFrame(input)!!; assertEquals(20L, f2.ptsUs)
        val f3 = ScreenProtocol.readFrame(input)!!; assertEquals(30L, f3.ptsUs)
        s.getOutputStream().write("P 0.5000 0.5000\nD 0.1 0.2\nE\n".toByteArray())
        waitFor { events.size == 3 }
        assertEquals(ScreenEvent.Point(0.5f, 0.5f), events[0])
        s.close()
        waitFor { viewers.last() == null }
    }

    @Test fun secondViewerIsBusy() {
        val (a, r1) = connect(gate.pin)
        assertEquals("OK 720 1600", r1)
        waitFor { viewers.contains("helper") }
        val (b, r2) = connect(gate.pin)
        assertEquals(ScreenProtocol.NO, r2)
        a.close(); b.close()
    }
}
