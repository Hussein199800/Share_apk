package com.example.screen

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import kotlin.concurrent.thread

/**
 * Mirrors the screen into an H.264 encoder and hands each encoded buffer to [onFrame].
 * Size is the screen scaled so its long side is at most [MAX_SIDE] (multiples of 16 for encoders).
 */
class ScreenCapture(
    private val projection: MediaProjection,
    screenWidth: Int,
    screenHeight: Int,
    private val densityDpi: Int,
    private val onFrame: (ScreenProtocol.Frame) -> Unit,
    private val onStopped: () -> Unit
) {
    val width: Int
    val height: Int

    init {
        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(screenWidth, screenHeight))
        width = (screenWidth * scale).toInt() / 16 * 16
        height = (screenHeight * scale).toInt() / 16 * 16
    }

    private var codec: MediaCodec? = null
    private var display: VirtualDisplay? = null
    @Volatile private var running = false
    private val callbackThread = HandlerThread("screen-projection").apply { start() }

    fun start() {
        // Android 14+: a callback must be registered before the virtual display is created.
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { stop(); onStopped() }
        }, Handler(callbackThread.looper))

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
            // A still screen produces no frames; repeat the last one so a new viewer sees something.
            setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 200_000)
        }
        val c = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        c.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val surface = c.createInputSurface()
        c.start()
        codec = c
        running = true
        display = projection.createVirtualDisplay(
            "ShareAPK screen", width, height, densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface, null, null
        )
        thread(name = "screen-encoder", isDaemon = true) { drain(c) }
    }

    fun requestKeyFrame() {
        runCatching { codec?.setParameters(Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0) }) }
    }

    private fun drain(c: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        while (running) {
            val index = try { c.dequeueOutputBuffer(info, 100_000) } catch (_: IllegalStateException) { break }
            if (index < 0) continue
            val buf = c.getOutputBuffer(index)
            if (buf != null && info.size > 0) {
                val bytes = ByteArray(info.size)
                buf.position(info.offset); buf.limit(info.offset + info.size); buf.get(bytes)
                var flags = 0
                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) flags = flags or ScreenProtocol.FLAG_CONFIG
                if (info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0) flags = flags or ScreenProtocol.FLAG_KEY
                onFrame(ScreenProtocol.Frame(info.presentationTimeUs, flags, bytes))
            }
            runCatching { c.releaseOutputBuffer(index, false) }
        }
    }

    fun stop() {
        if (!running) return
        running = false
        runCatching { display?.release() }
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        runCatching { projection.stop() }
        callbackThread.quitSafely()
        display = null; codec = null
    }

    companion object {
        const val MAX_SIDE = 1280
        const val BIT_RATE = 2_500_000
    }
}
