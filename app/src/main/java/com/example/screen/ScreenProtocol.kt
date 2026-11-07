package com.example.screen

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.util.Locale

/**
 * Wire format for phone-to-phone screen sharing (plain TCP on the LAN, port [PORT]).
 *
 * 1. Viewer → host: `SAPK-SCREEN 1 <pin> <viewer name>\n`
 * 2. Host → viewer: `OK <width> <height>\n` or `NO\n` (wrong PIN / busy)
 * 3. Host → viewer, repeated: [Frame] = int length, long ptsUs, byte flags, bytes (H.264 Annex-B)
 * 4. Viewer → host, repeated lines: [ScreenEvent] (pointer and drawing, coordinates 0…1)
 */
object ScreenProtocol {
    const val PORT = 53321
    const val MAGIC = "SAPK-SCREEN"
    const val VERSION = 1
    const val FLAG_CONFIG = 1
    const val FLAG_KEY = 2
    /** Larger frames are treated as a corrupt stream. */
    const val MAX_FRAME = 8 * 1024 * 1024

    fun hello(pin: String, viewerName: String) = "$MAGIC $VERSION $pin ${viewerName.replace('\n', ' ').take(40)}"

    data class Hello(val pin: String, val viewerName: String)

    fun parseHello(line: String?): Hello? {
        val parts = line?.trim()?.split(' ', limit = 4) ?: return null
        if (parts.size < 3 || parts[0] != MAGIC || parts[1].toIntOrNull() != VERSION) return null
        return Hello(parts[2], parts.getOrNull(3).orEmpty())
    }

    fun ok(width: Int, height: Int) = "OK $width $height"
    const val NO = "NO"

    /** Returns (width, height) for an OK reply, null for NO or garbage. */
    fun parseReply(line: String?): Pair<Int, Int>? {
        val p = line?.trim()?.split(' ') ?: return null
        if (p.size != 3 || p[0] != "OK") return null
        val w = p[1].toIntOrNull() ?: return null
        val h = p[2].toIntOrNull() ?: return null
        return if (w in 16..8192 && h in 16..8192) w to h else null
    }

    class Frame(val ptsUs: Long, val flags: Int, val data: ByteArray) {
        val isConfig get() = flags and FLAG_CONFIG != 0
        val isKey get() = flags and FLAG_KEY != 0
    }

    fun writeFrame(out: DataOutputStream, f: Frame) {
        out.writeInt(f.data.size)
        out.writeLong(f.ptsUs)
        out.writeByte(f.flags)
        out.write(f.data)
        out.flush()
    }

    /** Null at end of stream. Throws on a corrupt length. */
    fun readFrame(input: DataInputStream): Frame? {
        val len = try { input.readInt() } catch (_: EOFException) { return null }
        require(len in 0..MAX_FRAME) { "bad frame length $len" }
        val pts = input.readLong()
        val flags = input.readUnsignedByte()
        val data = ByteArray(len)
        input.readFully(data)
        return Frame(pts, flags, data)
    }
}

/** What the helper does on the shared screen. Coordinates are fractions of the screen (0…1). */
sealed interface ScreenEvent {
    /** A tap/finger position: shows a circle. */
    data class Point(val x: Float, val y: Float) : ScreenEvent
    /** A point of a drawn stroke. */
    data class Draw(val x: Float, val y: Float) : ScreenEvent
    /** The current stroke ended (finger lifted). */
    data object End : ScreenEvent

    fun encode(): String = when (this) {
        is Point -> String.format(Locale.ROOT, "P %.4f %.4f", x, y)
        is Draw -> String.format(Locale.ROOT, "D %.4f %.4f", x, y)
        End -> "E"
    }

    companion object {
        fun decode(line: String): ScreenEvent? {
            val p = line.trim().split(' ')
            if (p.firstOrNull() == "E") return End
            if (p.size != 3) return null
            val x = p[1].toFloatOrNull()?.takeIf { it in 0f..1f } ?: return null
            val y = p[2].toFloatOrNull()?.takeIf { it in 0f..1f } ?: return null
            return when (p[0]) { "P" -> Point(x, y); "D" -> Draw(x, y); else -> null }
        }
    }
}
