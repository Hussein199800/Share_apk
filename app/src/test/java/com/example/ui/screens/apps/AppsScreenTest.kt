package com.example.ui.screens.apps

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import com.example.data.model.AppFilter
import com.example.data.model.AppSortOption
import com.example.data.model.InstalledAppInfo
import com.example.ui.theme.ArabicStrings
import com.example.ui.theme.ShareAPKTheme
import com.example.ui.viewmodel.QuickZipBackupProgress
import com.example.ui.viewmodel.matchesFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

fun fakeApp(pkg: String, label: String = pkg, split: Boolean = false, system: Boolean = false, fav: Boolean = false) =
    InstalledAppInfo(
        packageName = pkg, label = label, versionName = "1.0", versionCode = 1, minSdk = 31, targetSdk = 36,
        isSystemApp = system, isSplitApk = split, baseApkPath = "/data/app/$pkg/base.apk",
        splitSourceDirs = if (split) listOf("/data/app/$pkg/split.apk") else emptyList(),
        splitNames = emptyList(), totalSizeBytes = 1_048_576, firstInstallTime = 0, lastUpdateTime = 0,
        installerPackageName = null, installerDisplayName = "", signingSha256 = "", supportedAbis = emptyList(),
        isFavorite = fav
    )

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class AppsScreenTest {
    @get:Rule val rule = createComposeRule()

    private var sent = false
    private var filterPicked: AppFilter? = AppFilter.USER_APPS

    private fun show(
        apps: List<InstalledAppInfo>,
        selected: Set<String> = emptySet(),
        query: String = ""
    ) = rule.setContent {
        ShareAPKTheme(darkTheme = true) {
            AppsScreen(
                apps = apps, isLoading = false, searchQuery = query, selectedFilter = AppFilter.USER_APPS,
                selectedSort = AppSortOption.NAME_ASC, isGridView = false, isExpandedScreen = false,
                selectedPackageNames = selected, focusedApp = null, computedSha256Map = emptyMap(),
                isComputingSha256 = false, quickZipProgress = QuickZipBackupProgress(),
                onSearchChange = {}, onFilterChange = { filterPicked = it }, onSortChange = {},
                onToggleGridView = {}, onRefreshApps = {}, onSelectAppForDetails = {},
                onToggleAppMultiSelect = {}, onClearMultiSelect = {}, onShareSingleApp = {},
                onSaveSingleAppToDownloads = {}, onReinstallVerifyApp = {}, onWifiShareApps = {},
                onShareSelectedApps = {}, onWifiShareSelected = {}, onBackupSelectedToZip = {}, onCancelQuickZipBackup = {},
                onToggleFavorite = {}, onComputeSha256 = {}, onSendSelected = { sent = true }
            )
        }
    }

    @Test
    fun selectionBarAppearsWithSelection() {
        show(listOf(fakeApp("a.b"), fakeApp("c.d")), selected = setOf("a.b"))
        rule.onNodeWithTag("multi_select_action_bar").assertExists()
        rule.onNodeWithTag("selection_send_button").performClick()
        assertTrue(sent)
    }

    @Test
    fun selectionBarHiddenWhenEmpty() {
        show(listOf(fakeApp("a.b")))
        rule.onAllNodesWithTag("multi_select_action_bar").assertCountEquals(0)
    }

    @Test
    fun emptySearchShowsNoResults() {
        show(emptyList(), query = "zzz")
        rule.onNodeWithText(ArabicStrings.emptyAppsList).assertExists()
    }

    @Test
    fun longAppNameEllipsizes() {
        val long = "تطبيق".repeat(30)
        show(listOf(fakeApp("a.b", label = long)))
        val node = rule.onNode(hasText(long)).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        assertEquals(1, results.first().lineCount)
    }

    @Test
    fun splitBadgeShownForSplitApps() {
        show(listOf(fakeApp("a.b", split = true)))
        rule.onNode(hasText(ArabicStrings.splitBadge) and hasAnyAncestor(hasTestTag("app_card_a.b")), useUnmergedTree = true).assertExists()
    }

    @Test
    fun filterAllClearsFilter() {
        show(listOf(fakeApp("a.b")))
        rule.onNodeWithText(ArabicStrings.filterAll).performClick()
        assertEquals(null, filterPicked)
    }

    @Test
    fun nullFilterMatchesEveryApp() {
        val apps = listOf(fakeApp("u"), fakeApp("s", system = true), fakeApp("x", split = true))
        assertEquals(3, apps.count { matchesFilter(it, null) })
        assertEquals(2, apps.count { matchesFilter(it, AppFilter.USER_APPS) })
        assertEquals(1, apps.count { matchesFilter(it, AppFilter.SYSTEM_APPS) })
    }
}
