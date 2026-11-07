package com.example.util

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** What went wrong, in terms the user can act on. Each kind has an Arabic/English reason + fix in AppStrings. */
enum class ErrorKind { TIMEOUT, UNREACHABLE, NO_NETWORK, IDENTITY_CHANGED, REJECTED_BY_SERVER, STORAGE_FULL, RATE_LIMITED, NOT_FOUND, UNKNOWN }

object ErrorClassifier {
    fun classify(raw: String?): ErrorKind {
        val m = raw.orEmpty().lowercase(Locale.ROOT)
        return when {
            "identity changed" in m || "certificate" in m -> ErrorKind.IDENTITY_CHANGED
            "timeout" in m || "timed out" in m -> ErrorKind.TIMEOUT
            "enospc" in m || "no space" in m -> ErrorKind.STORAGE_FULL
            "unable to resolve host" in m || "network is unreachable" in m || "no route" in m -> ErrorKind.NO_NETWORK
            "refused" in m || "failed to connect" in m || "connect" in m && "reset" in m -> ErrorKind.UNREACHABLE
            Regex("\\b(403|429)\\b").containsMatchIn(m) -> ErrorKind.RATE_LIMITED
            Regex("\\b404\\b").containsMatchIn(m) -> ErrorKind.NOT_FOUND
            Regex("http \\d{3}").containsMatchIn(m) -> ErrorKind.REJECTED_BY_SERVER
            else -> ErrorKind.UNKNOWN
        }
    }
}

/**
 * Diagnostics log: the last [MAX] raw errors with time and place, for the developer.
 * URLs, IPs and long hex tokens are masked before anything is written.
 */
class DiagnosticsLog(private val file: File) {

    @Synchronized
    fun add(where: String, message: String?, now: Long = System.currentTimeMillis()) {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date(now))
        val line = "$stamp [$where] ${mask(message.orEmpty()).replace('\n', ' ').take(500)}"
        val lines = (read() + line).takeLast(MAX)
        val tmp = File(file.parentFile, file.name + ".tmp")
        runCatching {
            tmp.writeText(lines.joinToString("\n"))
            if (!tmp.renameTo(file)) { file.writeText(tmp.readText()); tmp.delete() }
        }
    }

    /** Oldest first. */
    @Synchronized
    fun read(): List<String> = runCatching { if (file.exists()) file.readLines().filter { it.isNotBlank() } else emptyList() }.getOrDefault(emptyList())

    @Synchronized
    fun clear() { file.delete() }

    companion object {
        const val MAX = 50
        private val URL = Regex("https?://\\S+")
        private val IP = Regex("\\b\\d{1,3}(\\.\\d{1,3}){3}(:\\d+)?\\b")
        private val TOKEN = Regex("\\b[0-9a-fA-F]{20,}\\b")
        private val SECRET = Regex("(?i)(token|key|password|secret|pin)=[^&\\s]+")

        fun mask(s: String): String = s
            .replace(URL, "<url>")
            .replace(SECRET) { it.groupValues[1] + "=<hidden>" }
            .replace(IP, "<ip>")
            .replace(TOKEN, "<hex>")
    }
}
