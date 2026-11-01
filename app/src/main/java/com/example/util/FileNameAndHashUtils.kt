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
/** Real QR codes via ZXing (the previous hand-written encoder produced unreadable codes). */
object QrCodeGenerator {
    fun matrix(content: String, sizePx: Int): com.google.zxing.common.BitMatrix =
        com.google.zxing.qrcode.QRCodeWriter().encode(
            content,
            com.google.zxing.BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            mapOf(
                com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M,
                com.google.zxing.EncodeHintType.MARGIN to 2,
                com.google.zxing.EncodeHintType.CHARACTER_SET to "UTF-8"
            )
        )

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val m = matrix(content, sizePx)
        val pixels = IntArray(m.width * m.height) { i ->
            if (m.get(i % m.width, i / m.width)) 0xFF0F172A.toInt() else 0xFFFFFFFF.toInt()
        }
        return Bitmap.createBitmap(pixels, m.width, m.height, Bitmap.Config.ARGB_8888)
    }
}
