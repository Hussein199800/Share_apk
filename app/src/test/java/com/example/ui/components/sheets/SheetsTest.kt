package com.example.ui.components.sheets

import android.content.pm.PackageInstaller
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.installer.InstallSessionResult
import com.example.ui.components.design.BadgeTone
import com.example.ui.theme.ArabicStrings
import com.example.ui.theme.ShareAPKTheme
import com.example.wifi.WifiServerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class SheetsTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun wifiSheetShowsUrlAndCountdown() {
        var stopped = false
        val state = WifiServerState(isRunning = true, url = "http://192.168.1.5:8787", remainingSeconds = 125, downloadCount = 3)
        rule.setContent { ShareAPKTheme(darkTheme = true) { WifiShareServerDialog(state = state, onStopServer = { stopped = true }) } }
        rule.onNodeWithText("http://192.168.1.5:8787", substring = true).assertExists()
        rule.onNodeWithText("02:05").assertExists()
        rule.onNodeWithText(ArabicStrings.wifiDownloadsFormat.format(3)).assertExists()
        rule.onNodeWithText(ArabicStrings.stopServerBtn).performClick()
        assertTrue(stopped)
    }

    @Test
    fun installResultSuccessUsesSuccessTone() {
        val ok = InstallSessionResult(1, "a.b", PackageInstaller.STATUS_SUCCESS, "تم", "Done", "تم التثبيت", "Installed")
        val fail = ok.copy(status = PackageInstaller.STATUS_FAILURE)
        assertEquals(BadgeTone.Success, installResultTone(ok))
        assertEquals(BadgeTone.Error, installResultTone(fail))
        rule.setContent { ShareAPKTheme(darkTheme = true) { InstallStatusResultDialog(result = ok, onDismiss = {}) } }
        rule.onNodeWithText("تم التثبيت").assertExists()
    }

    @Test
    fun onboardingLastPageExplainsTabsAndFinishes() {
        var finished = false
        rule.setContent { ShareAPKTheme(darkTheme = true) { OnboardingPagerDialog(onFinishOnboarding = { finished = true }) } }
        repeat(3) { rule.onNodeWithTag("onboarding_next").performClick(); rule.waitForIdle() }
        rule.onNodeWithText(ArabicStrings.onboardingTabsTitle).assertExists()
        rule.onNodeWithTag("onboarding_next").performClick()
        assertTrue(finished)
    }

    @Test
    fun onboardingFirstPageOffersSkip() {
        var finished = false
        rule.setContent { ShareAPKTheme(darkTheme = true) { OnboardingPagerDialog(onFinishOnboarding = { finished = true }) } }
        rule.onNodeWithText(ArabicStrings.skipBtn).performClick()
        assertTrue(finished)
    }
}
