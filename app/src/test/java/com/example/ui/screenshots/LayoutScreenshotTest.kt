package com.example.ui.screenshots

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.data.preferences.UserPreferencesState
import com.example.receive.ReceiveState
import com.example.receive.ReceivedItem
import com.example.receive.ReceiverStatus
import com.example.receive.VersionStatus
import com.example.ui.components.design.AdaptiveScale
import com.example.ui.screens.receive.ReceiveScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.ShareAPKTheme
import com.example.ui.theme.ShareApkTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders key screens at the standard's sizes into build/screens/ for eye review (phase 8c grows this).
 * Not an assertion test: it fails only if a screen crashes while laying out.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LayoutScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val status = ReceiverStatus(running = true, alias = "Galaxy S25 Ultra", ip = "192.168.1.17", port = 53317)
    private val items = List(6) { i ->
        ReceivedItem(File("/x/$i.apk"), listOf("سكينة", "طاقتي", "سَيْل: منزّل الفيديو والصوت من أي موقع", "WhatsApp")[i % 4], "com.example.app$i", "1.$i.0", i.toLong(), VersionStatus.values()[i % VersionStatus.values().size], null, 0)
    }

    private fun shoot(name: String, dark: Boolean, textScale: Float = 1f, content: @androidx.compose.runtime.Composable () -> Unit) {
        rule.setContent {
            AdaptiveScale(textScale) { _, _ ->
                ShareAPKTheme(darkTheme = dark) {
                    Box(Modifier.fillMaxSize().background(ShareApkTheme.colors.background)) { content() }
                }
            }
        }
        rule.waitForIdle()
        // PixelCopy (captureToImage) hangs under Robolectric: draw the view tree directly.
        val view = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bmp))
        val dir = File("build/screens").apply { mkdirs() }
        File(dir, "$name-${if (dark) "dark" else "light"}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun receive(dark: Boolean, tag: String) = shoot("receive-$tag", dark) {
        ReceiveScreen(status, true, ReceiveState.Idle, items, {}, {}, {}, {}, {})
    }

    private fun settings(dark: Boolean, tag: String, scale: Float = 1f) = shoot("settings-$tag", dark, scale) {
        SettingsScreen(UserPreferencesState(textScale = scale), 12_345_678, {}, {}, {}, {}, {}, {}, deviceAlias = "Galaxy")
    }

    private fun screenTab(dark: Boolean, tag: String, sharing: Boolean) = shoot("screen-$tag", dark) {
        val peer = com.example.receive.discovery.NearbyDevice(com.example.receive.protocol.DeviceInfoDto(alias = "هاتف أمي", fingerprint = "f"), "192.168.1.30", 0)
        com.example.ui.screens.screen.ScreenTab(
            share = if (sharing) com.example.screen.ScreenShareState(true, "192.168.1.17:53321", "0420", "هاتف أحمد") else com.example.screen.ScreenShareState(),
            peers = listOf(peer), sharingHosts = setOf("192.168.1.30"),
            viewerState = com.example.screen.ViewerState.WrongPin, viewer = com.example.screen.ScreenViewer(),
            onProbe = {}, onWatch = { _, _ -> }, onLeave = {}
        )
    }

    @Config(sdk = [36], qualifiers = "w390dp-h1400dp-port")
    @Test fun backupCreate() = shoot("backup-create", false) {
        fun app(i: Int, sys: Boolean = false) = com.example.data.model.InstalledAppInfo(
            "com.example.app$i", listOf("واتساب", "سكينة", "طاقتي", "Telegram")[i % 4], "1.$i", i.toLong(), 31, 35, sys, i == 1,
            "/x", emptyList(), emptyList(), 25_000_000L * (i + 1), 0, 0, null, "", "", emptyList()
        )
        val apps = List(4) { app(it) }
        com.example.ui.screens.backup.BackupScreen(
            candidateApps = apps, selectedPackageNames = setOf("com.example.app0", "com.example.app2"), includeSystemApps = false,
            searchQuery = "", destinationUriString = "content://com.android.externalstorage.documents/tree/primary%3ABackups",
            spaceCheckResult = com.example.backup.SpaceCheckResult(50_000_000, 100_000_000, false),
            backupServiceState = com.example.service.BackupServiceProgressState(errorMessage = "ENOSPC no space left"),
            safetyCheck1 = false, safetyCheck2 = false, safetyCheck3 = false,
            restoreManifest = null, restoreCandidates = emptyList(), isBatchRestoring = false,
            batchRestoreCurrentIndex = 0, batchRestoreCurrentLabel = "", batchRestoreSummary = null,
            onToggleIncludeSystem = {}, onSearchQueryChange = {}, onToggleAppSelection = {},
            onSelectAll = {}, onSelectNone = {}, onPickDestinationFolder = {}, onStartFullBackup = {},
            onCancelFullBackup = {}, onSafetyCheckChange = { _, _ -> }, onConfirmSafetyChecklist = {},
            onPickRestoreFolder = {}, onToggleRestoreCandidate = {}, onStartSequentialRestore = {},
            onSkipCurrentRestoreApp = {}, onCancelBatchRestore = {}, onBack = {},
            selectedTotals = 2 to 100_000_000L
        )
    }

    @Config(sdk = [36], qualifiers = "w390dp-h844dp-port")
    @Test fun screenTabIdle() { screenTab(false, "idle", false) }

    @Config(sdk = [36], qualifiers = "w390dp-h844dp-port")
    @Test fun screenTabSharing() { screenTab(true, "sharing", true) }

    @Config(sdk = [36], qualifiers = "w390dp-h844dp-port")
    @Test fun phone() { receive(false, "phone"); }

    @Config(sdk = [36], qualifiers = "w390dp-h844dp-port")
    @Test fun phoneSettingsLargest() { settings(true, "phone-1.3", 1.3f) }

    @Config(sdk = [36], qualifiers = "w320dp-h640dp-port")
    @Test fun smallPhone() { receive(true, "small") }

    @Config(sdk = [36], qualifiers = "w844dp-h390dp-land")
    @Test fun phoneLandscape() { receive(false, "phone-land") }

    @Config(sdk = [36], qualifiers = "w800dp-h1280dp-port")
    @Test fun tabletPortrait() { settings(false, "tablet-port") }

    @Config(sdk = [36], qualifiers = "w1280dp-h800dp-land")
    @Test fun tabletLandscape() { receive(true, "tablet-land") }
}
