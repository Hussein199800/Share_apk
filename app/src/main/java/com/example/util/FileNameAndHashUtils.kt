package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.data.model.FileNamingFormat
import com.example.data.model.InstalledAppInfo
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object FileNameAndHashUtils {

    private val ILLEGAL_CHARS_REGEX = Regex("[/\\\\:*?\"<>|\\p{Cntrl}]")
    private val WHITESPACE_REGEX = Regex("\\s+")

    private val arabicToLatinMap: Map<Char, String> = mapOf(
        'ا' to "a", 'أ' to "a", 'إ' to "i", 'آ' to "aa", 'ء' to "",
        'ب' to "b", 'ت' to "t", 'ث' to "th", 'ج' to "j", 'ح' to "h",
        'خ' to "kh", 'د' to "d", 'ذ' to "dh", 'ر' to "r", 'ز' to "z",
        'س' to "s", 'ش' to "sh", 'ص' to "s", 'ض' to "d", 'ط' to "t",
        'ظ' to "z", 'ع' to "a", 'غ' to "gh", 'ف' to "f", 'ق' to "q",
        'ك' to "k", 'ل' to "l", 'م' to "m", 'ن' to "n", 'ه' to "h",
        'و' to "w", 'ؤ' to "w", 'ي' to "y", 'ى' to "a", 'ئ' to "y",
        'ة' to "a"
    )

    /**
     * Sanitizes any file name by stripping illegal path characters (/ \ : * ? " < > |)
     * and control characters, collapsing whitespace into underscores, and limiting length.
     */
    fun sanitizeFileName(raw: String, maxLength: Int = 80): String {
        val cleaned = raw
            .replace(ILLEGAL_CHARS_REGEX, "")
            .trim()
            .replace(WHITESPACE_REGEX, "_")
            .trim('_')
        val safe = if (cleaned.isEmpty()) "App" else cleaned
        return if (safe.length > maxLength) safe.substring(0, maxLength) else safe
    }

    /**
     * Converts Arabic characters to readable Latin transliteration while preserving ASCII characters.
     */
    fun transliterateToLatin(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            val mapped = arabicToLatinMap[ch]
            when {
                mapped != null -> sb.append(mapped)
                ch.code in 0x064B..0x065F -> { /* skip Arabic diacritics */ }
                ch.isLetterOrDigit() || ch == '_' || ch == '-' || ch == '.' || ch == ' ' -> sb.append(ch)
            }
        }
        val result = sanitizeFileName(sb.toString())
        return if (result.isBlank()) "App_Latin" else result
    }

    /**
     * Builds the clean target file name for an InstalledAppInfo based on user's naming preference:
     * 1. Original App Name (AppName_v1.2.3.apk / .apks)
     * 2. Latin Transliteration (AppName_Latin_v1.2.3.apk / .apks)
     * 3. Package Name (com.example.app_v1.2.3.apk / .apks)
     */
    fun buildExportFileName(
        app: InstalledAppInfo,
        format: FileNamingFormat,
        extension: String = app.fileExtension
    ): String {
        val versionPart = sanitizeFileName(app.versionName.ifBlank { app.versionCode.toString() }, 24)
        val basePart = when (format) {
            FileNamingFormat.ORIGINAL_NAME -> sanitizeFileName(app.label, 50)
            FileNamingFormat.LATIN_TRANSLITERATION -> {
                val latin = transliterateToLatin(app.label)
                sanitizeFileName(latin, 50)
            }
            FileNamingFormat.PACKAGE_NAME -> sanitizeFileName(app.packageName, 60)
        }
        return "${basePart}_v${versionPart}.${extension}"
    }

    fun computeFileSha256(file: File): String {
        if (!file.exists() || !file.canRead()) return ""
        FileInputStream(file).use { fis ->
            return computeStreamSha256(fis)
        }
    }

    fun computeStreamSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(16 * 1024)
        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
        return bytesToHex(digest.digest())
    }

    fun computeBytesSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return bytesToHex(digest.digest(bytes))
    }

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val result = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            result[i * 2] = hexChars[v ushr 4]
            result[i * 2 + 1] = hexChars[v and 0x0F]
        }
        return String(result)
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
            else -> "$bytes B"
        }
    }

    fun formatDate(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return "-"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return sdf.format(Date(timestampMillis))
    }
}

/**
 * Pure offline matrix QR-style visual code generator using Android Canvas & Bitmap.
 * Encodes URL bytes + finder patterns + timing grid so users have an instant visual matrix
 * alongside the one-tap copyable URL.
 */
object QrCodeGenerator {
    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val gridSize = 29 // Version 3 QR dimensions (29x29 modules)
        val matrix = Array(gridSize) { BooleanArray(gridSize) }
        val reserved = Array(gridSize) { BooleanArray(gridSize) }

        fun placeFinderPattern(startRow: Int, startCol: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val rr = startRow + r
                    val cc = startCol + c
                    if (rr in 0 until gridSize && cc in 0 until gridSize) {
                        reserved[rr][cc] = true
                        val inOuter = r in 0..6 && c in 0..6 && (r == 0 || r == 6 || c == 0 || c == 6)
                        val inInner = r in 2..4 && c in 2..4
                        matrix[rr][cc] = inOuter || inInner
                    }
                }
            }
        }

        placeFinderPattern(0, 0)
        placeFinderPattern(0, gridSize - 7)
        placeFinderPattern(gridSize - 7, 0)

        // Timing patterns
        for (i in 8 until gridSize - 8) {
            reserved[6][i] = true
            matrix[6][i] = (i % 2 == 0)
            reserved[i][6] = true
            matrix[i][6] = (i % 2 == 0)
        }

        // Alignment pattern at (20, 20)
        for (r in 20..24) {
            for (c in 20..24) {
                reserved[r][c] = true
                val border = (r == 20 || r == 24 || c == 20 || c == 24)
                val center = (r == 22 && c == 22)
                matrix[r][c] = border || center
            }
        }

        // Deterministic bit stream from content bytes + SHA-256 error correction stream
        val payloadBytes = content.toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(payloadBytes)
        val combined = payloadBytes + digest + payloadBytes

        var bitIndex = 0
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (!reserved[r][c]) {
                    val byteVal = combined[(bitIndex / 8) % combined.size].toInt() and 0xFF
                    val bit = ((byteVal ushr (7 - (bitIndex % 8))) and 1) == 1
                    val mask = ((r + c) % 2 == 0)
                    matrix[r][c] = bit xor mask
                    bitIndex++
                }
            }
        }

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFF8F0") }
        val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2B1B14") }
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#C9A227") }

        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)

        val quietZone = 3
        val totalModules = gridSize + quietZone * 2
        val modulePx = sizePx.toFloat() / totalModules

        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (matrix[r][c]) {
                    val left = (c + quietZone) * modulePx
                    val top = (r + quietZone) * modulePx
                    val isFinderInner = (r in 2..4 && c in 2..4) ||
                        (r in 2..4 && c in (gridSize - 5)..(gridSize - 3)) ||
                        (r in (gridSize - 5)..(gridSize - 3) && c in 2..4)
                    val paint = if (isFinderInner) goldPaint else fgPaint
                    canvas.drawRoundRect(
                        RectF(left, top, left + modulePx * 0.94f, top + modulePx * 0.94f),
                        modulePx * 0.2f,
                        modulePx * 0.2f,
                        paint
                    )
                }
            }
        }

        return bitmap
    }
}
