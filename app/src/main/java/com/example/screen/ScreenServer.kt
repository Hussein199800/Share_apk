package com.example.screen

import com.example.web.PinGate
import java.io.BufferedReader
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Host side of the stream, free of android.* so it runs in unit tests: accepts one viewer at a
 * time after the PIN, sends it the latest codec config and every frame, and reports its events.
 * A slow viewer drops frames (bounded queue) instead of stalling the encoder.
 */
class ScreenServer(
    private val gate: PinGate,
    private val size: () -> Pair<Int, Int>,
    private val onViewer: (name: String?) -> Unit,
    private val onEvent: (ScreenEvent) -> Unit,
    /** Asks the encoder for a key frame so a new viewer can start decoding at once. */
    private val requestKeyFrame: () -> Unit
) {
    private var server: ServerSocket? = null
    @Volatile private var viewer: Socket? = null
    @Volatile private var queue: ArrayBlockingQueue<ScreenProtocol.Frame>? = null
    @Volatile private var config: ScreenProtocol.Frame? = null
    @Volatile private var running = false
    @Volatile private var resync = false

    fun start(port: Int): Int {
        val s = runCatching { ServerSocket(port) }.getOrElse { ServerSocket(0) }
        server = s
        running = true
        thread(name = "screen-accept", isDaemon = true) {
            while (running) {
                val sock = try { s.accept() } catch (_: SocketException) { break }
                thread(name = "screen-viewer", isDaemon = true) { serve(sock) }
            }
        }
        return s.localPort
    }

    fun stop() {
        running = false
        runCatching { server?.close() }
        runCatching { viewer?.close() }
        server = null
    }

    /** Called by the encoder for every output buffer. */
    fun offer(frame: ScreenProtocol.Frame) {
        if (frame.isConfig) config = frame
        val q = queue ?: return
        if (!q.offer(frame)) {
            // Viewer is behind: drop what's queued and resync on the next key frame.
            q.clear()
            resync = true
            requestKeyFrame()
        }
    }

    private fun serve(sock: Socket) {
        sock.use {
            sock.soTimeout = 10_000
            sock.tcpNoDelay = true
            val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))
            val out = DataOutputStream(BufferedOutputStream(sock.getOutputStream(), 64 * 1024))
            val hello = ScreenProtocol.parseHello(runCatching { reader.readLine() }.getOrNull())
            val busy = viewer != null
            if (hello == null || busy || gate.login(hello.pin) == null) {
                runCatching { out.write((ScreenProtocol.NO + "\n").toByteArray()); out.flush() }
                return
            }
            val (w, h) = size()
            out.write((ScreenProtocol.ok(w, h) + "\n").toByteArray()); out.flush()
            sock.soTimeout = 0
            val q = ArrayBlockingQueue<ScreenProtocol.Frame>(30)
            viewer = sock
            queue = q
            onViewer(hello.viewerName.ifBlank { sock.inetAddress.hostAddress })
            val events = thread(name = "screen-events", isDaemon = true) {
                runCatching {
                    while (true) {
                        val line = reader.readLine() ?: break
                        ScreenEvent.decode(line)?.let(onEvent)
                    }
                }
                runCatching { sock.close() }
            }
            try {
                config?.let { ScreenProtocol.writeFrame(out, it) }
                requestKeyFrame()
                var waitingForKey = true
                while (running && !sock.isClosed) {
                    val f = q.poll(500, TimeUnit.MILLISECONDS) ?: continue
                    if (resync) { resync = false; waitingForKey = true }
                    if (f.isConfig) { ScreenProtocol.writeFrame(out, f); continue }
                    // After a drop or at start, skip until a key frame so the decoder never sees gaps.
                    if (waitingForKey && !f.isKey) continue
                    waitingForKey = false
                    ScreenProtocol.writeFrame(out, f)
                }
            } catch (_: Exception) {
                // Viewer left.
            } finally {
                queue = null
                viewer = null
                events.interrupt()
                onViewer(null)
            }
        }
    }
}
