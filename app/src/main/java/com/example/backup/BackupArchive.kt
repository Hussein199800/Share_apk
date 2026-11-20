package com.example.backup

import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Compressed backups: every app file lives in one ZIP (`apps.zip`) next to `manifest.json`,
 * under `<package>/<file>`. Pure java.* so it is unit-tested on the JVM.
 *
 * Safety: the SHA-256 recorded in the manifest is of the original bytes; it is checked after
 * writing (whole archive re-read) and again while restoring, before anything reaches the installer.
 */
object BackupArchive {
    const val NAME = "apps.zip"

    /** Already-compressed images aren't worth deflating; everything else is. */
    private const val LEVEL = Deflater.DEFAULT_COMPRESSION

    fun entryName(packageFolder: String, fileName: String) = "$packageFolder/$fileName"

    fun open(out: OutputStream): ZipOutputStream = ZipOutputStream(out).apply { setLevel(LEVEL) }

    /** Copies [input] into a new entry and returns (size, sha256) of the original bytes. */
    fun addEntry(zos: ZipOutputStream, name: String, input: InputStream): Pair<Long, String> {
        zos.putNextEntry(ZipEntry(name))
        val md = MessageDigest.getInstance("SHA-256")
        val buf = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            md.update(buf, 0, n)
            zos.write(buf, 0, n)
            total += n
        }
        zos.closeEntry()
        return total to hex(md.digest())
    }

    fun hex(b: ByteArray) = b.joinToString("") { String.format(java.util.Locale.ROOT, "%02x", it) }

    /**
     * Reads the whole archive once and checks every expected entry's size and SHA-256.
     * Returns the problems (missing, size or checksum mismatch); empty means all good.
     * ZipInputStream also checks each entry's CRC-32 and throws on a damaged archive.
     */
    fun verify(input: InputStream, expected: Map<String, Pair<Long, String>>, onEntry: (String) -> Unit = {}): List<String> {
        val seen = HashMap<String, Pair<Long, String>>()
        try {
            ZipInputStream(input).use { zis ->
                while (true) {
                    val e = zis.nextEntry ?: break
                    if (e.name !in expected) continue
                    onEntry(e.name)
                    val md = MessageDigest.getInstance("SHA-256")
                    val buf = ByteArray(64 * 1024)
                    var size = 0L
                    while (true) {
                        val n = zis.read(buf)
                        if (n < 0) break
                        md.update(buf, 0, n); size += n
                    }
                    seen[e.name] = size to hex(md.digest())
                }
            }
        } catch (e: IOException) {
            return listOf("$NAME (damaged: ${e.message})")
        }
        return expected.mapNotNull { (name, exp) ->
            val got = seen[name]
            when {
                got == null -> "$name (missing)"
                got.first != exp.first -> "$name (size mismatch)"
                !got.second.equals(exp.second, ignoreCase = true) -> "$name (SHA-256 mismatch)"
                else -> null
            }
        }
    }

    /** Wraps [input] so that reaching its end with the wrong SHA-256 or size throws, instead of finishing quietly. */
    fun checked(input: InputStream, expectedSize: Long, expectedSha: String): InputStream = object : FilterInputStream(input) {
        private val md = MessageDigest.getInstance("SHA-256")
        private var size = 0L
        private var done = false

        override fun read(): Int {
            val b = super.read()
            if (b < 0) finish() else { md.update(b.toByte()); size++ }
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = super.read(b, off, len)
            if (n < 0) finish() else { md.update(b, off, n); size += n }
            return n
        }

        private fun finish() {
            if (done) return
            done = true
            if (size != expectedSize || !hex(md.digest()).equals(expectedSha, ignoreCase = true)) {
                throw IOException("backup file is damaged (SHA-256 mismatch)")
            }
        }
    }

    /**
     * Random access to the archive when it is a real file (fast: reads the central directory),
     * else a streaming scan per lookup.
     */
    class Reader(
        private val zipFile: ZipFile?,
        private val openStream: () -> InputStream,
        private val onClose: () -> Unit = {}
    ) : AutoCloseable {
        fun size(name: String): Long? = if (zipFile != null) zipFile.getEntry(name)?.size else scan(name) { _, e -> e.size.takeIf { it >= 0 } ?: -1L }

        /** The entry's bytes, or null if absent. Caller closes the stream. */
        fun stream(name: String): InputStream? {
            zipFile?.let { z -> return z.getEntry(name)?.let { z.getInputStream(it) } }
            val zis = ZipInputStream(openStream())
            while (true) {
                val e = zis.nextEntry ?: run { zis.close(); return null }
                if (e.name == name) return zis
            }
        }

        fun names(): Set<String> = zipFile?.entries()?.toList()?.map { it.name }?.toSet()
            ?: ZipInputStream(openStream()).use { zis -> generateSequence { zis.nextEntry }.map { it.name }.toSet() }

        private fun <T> scan(name: String, read: (ZipInputStream, ZipEntry) -> T): T? =
            ZipInputStream(openStream()).use { zis ->
                generateSequence { zis.nextEntry }.firstOrNull { it.name == name }?.let { read(zis, it) }
            }

        override fun close() { runCatching { zipFile?.close() }; runCatching(onClose) }

        companion object {
            fun of(file: File?, openStream: () -> InputStream, onClose: () -> Unit = {}): Reader =
                Reader(file?.let { runCatching { ZipFile(it) }.getOrNull() }, openStream, onClose)
        }
    }
}
