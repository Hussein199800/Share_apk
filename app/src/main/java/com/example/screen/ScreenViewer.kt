package com.example.screen

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

sealed interface ViewerState {
    data object Idle : ViewerState
    data object Connecting : ViewerState
    data class Watching(val width: Int, val height: Int) : ViewerState
    data object WrongPin : ViewerState
    data class Failed(val message: String) : ViewerState
    /** The other phone stopped sharing. */
    data object Ended : ViewerState
}

/** Helper side: connects with the PIN, decodes the stream onto a Surface, sends pointer/drawing. */
class ScreenViewer {
    private val _state = MutableStateFlow<ViewerState>(ViewerState.Idle)
    val state: StateFlow<ViewerState> = _state.asStateFlow()

    @Volatile private var socket: Socket? = null
    @Volatile private var surface: Surface? = null
    @Volatile private var codec: MediaCodec? = null
    private var writer: OutputStreamWriter? = null
    private val events = LinkedBlockingQueue<ScreenEvent>(256)

    fun connect(host: String, port: Int, pin: String, myName: String) {
        disconnect()
        _state.value = ViewerState.Connecting
        thread(name = "screen-viewer", isDaemon = true) {
            try {
                val s = Socket().apply { connect(InetSocketAddress(host, port), 4_000); tcpNoDelay = true; soTimeout = 10_000 }
                socket = s
                val w = OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8)
                w.write(ScreenProtocol.hello(pin, myName) + "\n"); w.flush()
                writer = w
                val input = BufferedInputStream(s.getInputStream(), 64 * 1024)
                val reply = readLine(input)
                val size = ScreenProtocol.parseReply(reply)
                if (size == null) { _state.value = ViewerState.WrongPin; s.close(); return@thread }
                s.soTimeout = 0
                _state.value = ViewerState.Watching(size.first, size.second)
                thread(name = "screen-events-out", isDaemon = true) { pumpEvents(w) }
                val data = DataInputStream(input)
                while (true) {
                    val f = ScreenProtocol.readFrame(data) ?: break
                    feed(f, size)
                }
                if (_state.value is ViewerState.Watching) _state.value = ViewerState.Ended
            } catch (e: Exception) {
                if (socket != null && _state.value !is ViewerState.Idle) {
                    _state.value = if (_state.value is ViewerState.Watching) ViewerState.Ended else ViewerState.Failed(e.message ?: e.javaClass.simpleName)
                }
            } finally {
                releaseCodec()
            }
        }
    }

    /** The decoder needs a Surface; it may arrive before or after the stream starts. */
    fun setSurface(s: Surface?) {
        surface = s
        if (s == null) releaseCodec()
    }

    fun send(e: ScreenEvent) { events.offer(e) }

    fun disconnect() {
        _state.value = ViewerState.Idle
        runCatching { socket?.close() }
        socket = null
        events.offer(ScreenEvent.End)
        releaseCodec()
    }

    private var lastConfig: ScreenProtocol.Frame? = null

    private fun feed(f: ScreenProtocol.Frame, size: Pair<Int, Int>) {
        if (f.isConfig) lastConfig = f
        val surf = surface ?: return
        var c = codec
        if (c == null) {
            // Start only at a config + key frame; until then there is nothing decodable.
            val cfg = lastConfig ?: return
            if (!f.isConfig && !f.isKey) return
            c = runCatching {
                MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
                    configure(MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, size.first, size.second), surf, null, 0)
                    start()
                }
            }.getOrNull() ?: return
            codec = c
            queue(c, cfg, MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
            if (f.isConfig) return
        }
        queue(c, f, if (f.isConfig) MediaCodec.BUFFER_FLAG_CODEC_CONFIG else 0)
        val info = MediaCodec.BufferInfo()
        while (true) {
            val out = try { c.dequeueOutputBuffer(info, 0) } catch (_: IllegalStateException) { return }
            if (out < 0) break
            runCatching { c.releaseOutputBuffer(out, true) }
        }
    }

    private fun queue(c: MediaCodec, f: ScreenProtocol.Frame, flags: Int) {
        val i = try { c.dequeueInputBuffer(50_000) } catch (_: IllegalStateException) { return }
        if (i < 0) return
        // The surface (and codec) can go away on another thread at any moment.
        runCatching {
            val buf = c.getInputBuffer(i) ?: return
            buf.clear(); buf.put(f.data)
            c.queueInputBuffer(i, 0, f.data.size, f.ptsUs, flags)
        }
    }

    private fun releaseCodec() {
        val c = codec ?: return
        codec = null
        runCatching { c.stop() }
        runCatching { c.release() }
    }

    private fun pumpEvents(w: OutputStreamWriter) {
        runCatching {
            while (socket?.isClosed == false) {
                val e = events.take()
                if (socket?.isClosed != false) break
                w.write(e.encode() + "\n")
                if (events.isEmpty()) w.flush()
            }
        }
    }

    private fun readLine(input: BufferedInputStream): String? {
        val sb = StringBuilder()
        while (sb.length < 200) {
            val b = input.read()
            if (b < 0) return null
            if (b == '\n'.code) return sb.toString()
            sb.append(b.toChar())
        }
        return null
    }
}
