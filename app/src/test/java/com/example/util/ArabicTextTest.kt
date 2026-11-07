package com.example.util

import com.example.ui.theme.Changelog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArabicTextTest {
    private val apps = ArabicText.Forms("تطبيق واحد", "تطبيقان", "تطبيقات", "تطبيقاً", "تطبيق")

    @Test fun countsUseTheRightForm() {
        assertEquals("0 تطبيقات", ArabicText.count(0, apps))
        assertEquals("تطبيق واحد", ArabicText.count(1, apps))
        assertEquals("تطبيقان", ArabicText.count(2, apps))
        assertEquals("3 تطبيقات", ArabicText.count(3, apps))
        assertEquals("10 تطبيقات", ArabicText.count(10, apps))
        assertEquals("11 تطبيقاً", ArabicText.count(11, apps))
        assertEquals("99 تطبيقاً", ArabicText.count(99, apps))
        assertEquals("100 تطبيق", ArabicText.count(100, apps))
        assertEquals("103 تطبيقات", ArabicText.count(103, apps))
        assertEquals("1 app", ArabicText.count(1, ArabicText.Forms("app", "apps", "apps", "apps", "apps"), arabic = false))
    }

    @Test fun searchFoldsSpellings() {
        assertTrue(ArabicText.matches("إدلب", "ادلب"))
        assertTrue(ArabicText.matches("محاضرة", "محاضره"))
        assertTrue(ArabicText.matches("مُستشفى", "مستشفي"))
        assertTrue(ArabicText.matches("WhatsApp Business", "business whats"))
        assertTrue(ArabicText.matches("إصدار ٣", "اصدار 3"))
        assertFalse(ArabicText.matches("تلغرام", "واتساب"))
    }

    @Test fun notesBecomePlainText() {
        assertEquals("العنوان\n\n• **غامق** نقطة".replace("**", ""), ArabicText.plainNotes("## العنوان\n\n- **غامق** نقطة"))
    }
}

class DiagnosticsTest {
    @Test fun keepsLast50AndMasksSecrets() {
        val f = File.createTempFile("diag", ".log").apply { delete() }
        val log = DiagnosticsLog(f)
        repeat(60) { log.add("t", "err $it at http://192.168.1.5:53317/x?token=abc pin=1234 from 10.0.0.2") }
        val lines = log.read()
        assertEquals(50, lines.size)
        assertTrue(lines.last().contains("err 59"))
        assertFalse(lines.any { "192.168" in it || "10.0.0.2" in it || "1234" in it || "abc" in it })
        log.clear(); assertTrue(log.read().isEmpty())
    }

    @Test fun classifiesCommonFailures() {
        assertEquals(ErrorKind.TIMEOUT, ErrorClassifier.classify("SocketTimeoutException: timeout"))
        assertEquals(ErrorKind.UNREACHABLE, ErrorClassifier.classify("Failed to connect to /192.168.1.4:53317"))
        assertEquals(ErrorKind.IDENTITY_CHANGED, ErrorClassifier.classify("device identity changed"))
        assertEquals(ErrorKind.REJECTED_BY_SERVER, ErrorClassifier.classify("upload HTTP 500"))
        assertEquals(ErrorKind.UNKNOWN, ErrorClassifier.classify(null))
    }
}

/** The version lives in build.gradle.kts, the changelog and release-notes.md: they must agree. */
class ChangelogTest {
    @Test fun versionMatchesEverywhere() {
        val version = com.example.BuildConfig.VERSION_NAME
        assertEquals(version, Changelog.latest.version)
        val notes = File("../release-notes.md").readText()
        assertTrue("release-notes.md must start with the version", notes.lines().first().endsWith(version))
        assertFalse("release-notes.md must be plain text", notes.contains("**") || notes.contains("##"))
        Changelog.latest.ar.forEach { assertTrue("missing in release-notes.md: $it", notes.contains(it)) }
        val code = Regex("versionCode = (\\d+)").find(File("build.gradle.kts").readText())!!.groupValues[1].toInt()
        val (ma, mi, pa) = version.split('.').map { it.toInt() }
        assertEquals(ma * 10000 + mi * 100 + pa, code)
    }

    @Test fun changelogIsNewestFirstAndBilingual() {
        val versions = Changelog.entries.map { v -> v.version.split('.').map { it.toInt() }.let { it[0] * 10000 + it[1] * 100 + it[2] } }
        assertEquals(versions.sortedDescending(), versions)
        Changelog.entries.forEach { assertEquals(it.ar.size, it.en.size) }
    }
}
