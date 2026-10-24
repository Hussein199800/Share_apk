package com.example

import com.example.data.model.FileNamingFormat
import com.example.data.model.InstalledAppInfo
import com.example.util.FileNameAndHashUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun fileNameSanitization_stripsIllegalCharsAndTransliterates() {
        val dirty = "شارك/أبك: Share*APK?\"<>|"
        val sanitized = FileNameAndHashUtils.sanitizeFileName(dirty)
        assertFalse(sanitized.contains("/"))
        assertFalse(sanitized.contains(":"))
        assertFalse(sanitized.contains("*"))

        val latin = FileNameAndHashUtils.transliterateToLatin("شارك أبك")
        assertTrue(latin.isNotBlank())
    }

    @Test
    fun exportFileName_respectsNamingFormatsAndSplitExtension() {
        val sampleApp = InstalledAppInfo(
            packageName = "com.shareapk.app",
            label = "شارك أبك",
            versionName = "2.4.0",
            versionCode = 1L,
            minSdk = 31,
            targetSdk = 35,
            isSystemApp = false,
            isSplitApk = true,
            baseApkPath = "/data/app/base.apk",
            splitSourceDirs = listOf("/data/app/split_config.arm64_v8a.apk"),
            splitNames = listOf("config.arm64_v8a"),
            totalSizeBytes = 1048576L,
            firstInstallTime = 1000L,
            lastUpdateTime = 2000L,
            installerPackageName = "com.android.vending",
            installerDisplayName = "Google Play Store",
            signingSha256 = "AA:BB",
            supportedAbis = listOf("arm64-v8a")
        )

        val pkgNamed = FileNameAndHashUtils.buildExportFileName(sampleApp, FileNamingFormat.PACKAGE_NAME)
        assertEquals("com.shareapk.app_v2.4.0.apks", pkgNamed)

        val sha = FileNameAndHashUtils.computeBytesSha256("ShareAPK".toByteArray(Charsets.UTF_8))
        assertEquals(64, sha.length)
    }
}
