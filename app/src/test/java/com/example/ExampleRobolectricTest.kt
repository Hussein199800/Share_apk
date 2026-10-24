package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.backup.PreFlashBackupManager
import com.example.data.model.BackupAppEntry
import com.example.data.model.BackupFileEntry
import com.example.data.model.BackupManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name and verify BackupManifest roundtrip`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertTrue(appName.contains("ShareAPK"))

        val manager = PreFlashBackupManager(context)
        val manifest = BackupManifest(
            schemaVersion = 1,
            backupDateIso = "2026-10-24T14:00:00Z",
            deviceModel = "Pixel 9 Pro",
            androidRelease = "15",
            sdkInt = 35,
            appCount = 1,
            totalSizeBytes = 4096L,
            apps = listOf(
                BackupAppEntry(
                    label = "شارك أبك",
                    packageName = "com.shareapk.app",
                    versionName = "2.4.0",
                    versionCode = 1L,
                    minSdk = 31,
                    targetSdk = 35,
                    isSplitApk = true,
                    supportedAbis = listOf("arm64-v8a"),
                    installerPackageName = "com.android.vending",
                    googlePlayUrl = "https://play.google.com/store/apps/details?id=com.shareapk.app",
                    totalSizeBytes = 4096L,
                    files = listOf(
                        BackupFileEntry("base.apk", 2048L, "AABBCC"),
                        BackupFileEntry("split_config.arm64_v8a.apk", 2048L, "DDEEFF")
                    )
                )
            )
        )

        val json = manager.serializeManifestToJson(manifest)
        val parsed = manager.parseManifestFromJson(json)
        assertEquals(1, parsed.appCount)
        assertEquals("com.shareapk.app", parsed.apps.first().packageName)
        assertEquals(2, parsed.apps.first().files.size)
    }
}
