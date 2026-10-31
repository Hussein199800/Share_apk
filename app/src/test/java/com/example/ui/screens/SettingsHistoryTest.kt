package com.example.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.local.HistoryEntity
import com.example.data.model.ThemePreference
import com.example.data.preferences.UserPreferencesState
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.settings.AboutScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.ArabicStrings
import com.example.ui.theme.ShareAPKTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class SettingsHistoryTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun emptyHistoryShowsEmptyState() {
        rule.setContent { ShareAPKTheme(darkTheme = true) { HistoryScreen(emptyList(), {}, {}, {}) } }
        rule.onNodeWithText(ArabicStrings.emptyHistoryMsg).assertExists()
    }

    @Test
    fun historyItemShowsTypeBadge() {
        val item = HistoryEntity(id = 1, actionType = "SHARE", appLabel = "سكينة", packageName = "a.b",
            versionName = "1.0", fileFormat = ".apk", fileSizeBytes = 10, sha256Hash = "ab".repeat(32))
        rule.setContent { ShareAPKTheme(darkTheme = true) { HistoryScreen(listOf(item), {}, {}, {}) } }
        rule.onNodeWithText("SHARE").assertExists()
        rule.onNodeWithText("سكينة").assertExists()
    }

    @Test
    fun themeOptionsAreSystemLightDark() {
        var picked: ThemePreference? = null
        rule.setContent {
            ShareAPKTheme(darkTheme = true) {
                SettingsScreen(UserPreferencesState(), 0L, { picked = it }, {}, {}, {}, {}, {})
            }
        }
        rule.onAllNodesWithText(ArabicStrings.themeSystem).onFirst().assertExists()
        rule.onNodeWithText(ArabicStrings.themeDark).assertExists()
        rule.onNodeWithText(ArabicStrings.themeLight).performClick()
        assertEquals(ThemePreference.LIGHT, picked)
    }

    @Test
    fun noDynamicColorOption() {
        rule.setContent {
            ShareAPKTheme(darkTheme = true) { SettingsScreen(UserPreferencesState(), 0L, {}, {}, {}, {}, {}, {}) }
        }
        rule.onAllNodesWithText("Material You", substring = true).assertCountEquals(0)
    }

    @Test
    fun aboutShowsVersionAndFontLicense() {
        rule.setContent { ShareAPKTheme(darkTheme = true) { AboutScreen(appVersion = "2.4.0", onBack = {}) } }
        rule.onNodeWithText(ArabicStrings.appVersionFormat.format("2.4.0"), substring = true).assertExists()
        rule.onNodeWithText("SIL Open Font License", substring = true).assertExists()
    }
}
