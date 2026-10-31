package com.example.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MainNavTab
import com.example.ui.screens.send.SendScreen
import com.example.ui.theme.ArabicStrings
import com.example.ui.theme.EnglishStrings
import com.example.ui.theme.ShareAPKTheme
import com.example.ui.viewmodel.ShareApkViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RootNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun moreSubPageBackReturnsToMoreHub() {
        val vm = ShareApkViewModel(ApplicationProvider.getApplicationContext<Application>())
        vm.completeOnboarding()
        rule.setContent { ShareApkAppRoot(viewModel = vm, isExpandedScreen = false) }

        listOf("APPS", "SEND", "RECEIVE", "MORE").forEach { rule.onNodeWithTag("nav_$it").assertExists() }

        rule.onNodeWithTag("nav_MORE").performClick()
        rule.onNodeWithText(ArabicStrings.tileSettingsTitle).performClick()
        rule.onNodeWithTag("more_SETTINGS").assertExists()

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText(ArabicStrings.tileSettingsTitle).assertExists()
    }

    @Test
    fun englishShellIsLeftToRight() {
        rule.setContent {
            ShareApkShell(
                strings = EnglishStrings,
                darkTheme = true,
                isExpandedScreen = false,
                currentTab = MainNavTab.APPS,
                onSelectTab = {}
            ) {}
        }
        val apps = rule.onNodeWithTag("nav_APPS").getUnclippedBoundsInRoot().left
        val more = rule.onNodeWithTag("nav_MORE").getUnclippedBoundsInRoot().left
        assertTrue("apps=$apps more=$more", apps < more)
    }

    @Test
    fun arabicShellIsRightToLeft() {
        rule.setContent {
            ShareApkShell(
                strings = ArabicStrings,
                darkTheme = true,
                isExpandedScreen = false,
                currentTab = MainNavTab.APPS,
                onSelectTab = {}
            ) {}
        }
        val apps = rule.onNodeWithTag("nav_APPS").getUnclippedBoundsInRoot().left
        val more = rule.onNodeWithTag("nav_MORE").getUnclippedBoundsInRoot().left
        assertTrue("apps=$apps more=$more", apps > more)
    }

    @Test
    fun sendWithoutSelectionInvitesToPickApps() {
        var picked = false
        rule.setContent {
            ShareAPKTheme(darkTheme = true) {
                SendScreen(selectedApps = emptyList(), onPickApps = { picked = true }, onShareViaLink = {})
            }
        }
        rule.onNodeWithText(ArabicStrings.sendPickAppsTitle).assertExists()
        rule.onNodeWithText(ArabicStrings.sendPickAppsBtn).performClick()
        assertTrue(picked)
    }
}
