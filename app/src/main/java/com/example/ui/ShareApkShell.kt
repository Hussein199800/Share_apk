package com.example.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ScreenShare
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.model.MainNavTab
import com.example.ui.components.design.HaloBackground
import com.example.ui.components.design.LocalBottomBarClearance
import com.example.ui.components.design.NavItem
import com.example.ui.components.design.ShareApkBottomBar
import com.example.ui.components.design.ShareApkRail
import com.example.ui.theme.AppStrings
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ShareAPKTheme
import com.example.ui.theme.ShareApkTokens

/**
 * Stateless chrome: strings, direction, theme, halo background, tab bar or rail, snackbar.
 * Kept separate from [ShareApkAppRoot] so layout direction and navigation can be tested without a ViewModel.
 */
@Composable
fun ShareApkShell(
    strings: AppStrings,
    darkTheme: Boolean,
    isExpandedScreen: Boolean,
    currentTab: MainNavTab,
    onSelectTab: (MainNavTab) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    overlay: (@Composable () -> Unit)? = null,
    /** Extra lift for the snackbar (e.g. above the Apps selection bar). */
    snackbarLift: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val layoutDirection = if (strings.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(
        LocalAppStrings provides strings,
        LocalLayoutDirection provides layoutDirection
    ) {
        ShareAPKTheme(darkTheme = darkTheme) {
            val items = listOf(
                NavItem(MainNavTab.APPS, strings.navApps, Icons.Outlined.Apps),
                NavItem(MainNavTab.FILES, strings.navFiles, Icons.Outlined.FolderCopy),
                NavItem(MainNavTab.RECEIVE, strings.navReceive, Icons.Outlined.Download),
                NavItem(MainNavTab.SCREEN, strings.navScreen, Icons.Outlined.ScreenShare),
                NavItem(MainNavTab.MORE, strings.navMore, Icons.Outlined.MoreHoriz)
            )
            // With the keyboard open the IME already covers the navigation bar: don't add both.
            val navInsets = WindowInsets.navigationBars.exclude(WindowInsets.ime)
            val navInset = navInsets.asPaddingValues().calculateBottomPadding()
            val clearance = if (isExpandedScreen) navInset else ShareApkTokens.BottomBarClearance + navInset
            HaloBackground(modifier) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                        .imePadding()
                ) {
                    if (isExpandedScreen) ShareApkRail(items, currentTab, onSelectTab)
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        CompositionLocalProvider(LocalBottomBarClearance provides clearance) { content() }
                        if (!isExpandedScreen) {
                            ShareApkBottomBar(
                                items, currentTab, onSelectTab,
                                Modifier.align(Alignment.BottomCenter).windowInsetsPadding(navInsets)
                            )
                        }
                        if (snackbarHostState != null) {
                            SnackbarHost(
                                snackbarHostState,
                                Modifier.align(Alignment.BottomCenter).padding(bottom = clearance + snackbarLift)
                            )
                        }
                    }
                }
                overlay?.invoke()
            }
        }
    }
}
