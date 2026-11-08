package com.example.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlin.random.Random

class BackupArchiveTest {
    @get:Rule val tmp = TemporaryFolder()

    private val base = Random(1).nextBytes(300_000) + ByteArray(200_000) // part random, part compressible
    private val split = Random(2).nextBytes(50_000)

    private fun sha(b: ByteArray) = BackupArchive.hex(MessageDigest.getInstance("SHA-256").digest(b))

    private fun build(): Pair<File, Map<String, Pair<Long, String>>> {
        val f = tmp.newFile("apps.zip")
        val expected = mutableMapOf<String, Pair<Long, String>>()
        BackupArchive.open(f.outputStream()).use { zos ->
            listOf("com.a/base.apk" to base, "com.a/split_config.arm64_v8a.apk" to split).forEach { (name, bytes) ->
                expected[name] = BackupArchive.addEntry(zos, name, bytes.inputStream())
            }
        }
        return f to expected
    }

    @Test fun recordsOriginalSizeAndShaAndCompresses() {
        val (f, expected) = build()
        assertEquals(base.size.toLong() to sha(base), expected["com.a/base.apk"])
        assertTrue("archive should be smaller than its content", f.length() < base.size + split.size)
        assertTrue(BackupArchive.verify(f.inputStream(), expected).isEmpty())
    }

    @Test fun verifyCatchesDamageAndMissingFiles() {
        val (f, expected) = build()
        val withMissing = expected + ("com.b/base.apk" to (1L to "00"))
        assertEquals(listOf("com.b/base.apk (missing)"), BackupArchive.verify(f.inputStream(), withMissing))
        val bytes = f.readBytes()
        bytes[bytes.size / 3] = (bytes[bytes.size / 3].toInt() xor 0x55).toByte() // flip bits inside compressed data
        val problems = BackupArchive.verify(bytes.inputStream(), expected)
        assertTrue("damage must be reported: $problems", problems.isNotEmpty())
    }

    @Test fun readerStreamsEntriesFromFileAndFromStream() {
        val (f, _) = build()
        listOf(BackupArchive.Reader.of(f, { f.inputStream() }), BackupArchive.Reader.of(null, { f.inputStream() })).forEach { r ->
            r.use {
                assertArrayEquals(split, r.stream("com.a/split_config.arm64_v8a.apk")!!.use { it.readBytes() })
                assertNull(r.stream("nope"))
                assertEquals(setOf("com.a/base.apk", "com.a/split_config.arm64_v8a.apk"), r.names())
            }
        }
        BackupArchive.Reader.of(f, { f.inputStream() }).use { assertEquals(base.size.toLong(), it.size("com.a/base.apk")) }
    }

    @Test fun checkedStreamPassesGoodDataAndThrowsOnBad() {
        val ok = BackupArchive.checked(split.inputStream(), split.size.toLong(), sha(split)).use { it.readBytes() }
        assertArrayEquals(split, ok)
        val bad = split.copyOf().also { it[10] = (it[10] + 1).toByte() }
        try {
            BackupArchive.checked(bad.inputStream(), split.size.toLong(), sha(split)).use { it.readBytes() }
            fail("damaged data must not pass")
        } catch (e: IOException) {
            assertNotNull(e.message)
        }
    }
}
