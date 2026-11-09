package com.example.ui.navigation

import com.example.data.model.MainNavTab

/** Sub-pages hosted inside the More tab. */
enum class MoreDestination { HOME, BACKUP, HISTORY, SETTINGS, ABOUT, WHATS_NEW, DIAGNOSTICS, REMOVED }

data class NavSnapshot(val tab: MainNavTab, val more: MoreDestination, val hasSelection: Boolean)

sealed interface BackAction {
    data object ClearSelection : BackAction
    data object MoreHome : BackAction
    data object GoApps : BackAction
    data object Exit : BackAction
}

/**
 * Back order: clear a *visible* selection (Apps) → leave a More sub-page → return to Apps → exit.
 * A selection hidden behind another tab is kept and never swallows a back press.
 */
fun resolveBack(s: NavSnapshot): BackAction = when {
    s.hasSelection && s.tab == MainNavTab.APPS -> BackAction.ClearSelection
    s.tab == MainNavTab.MORE && s.more != MoreDestination.HOME -> BackAction.MoreHome
    s.tab != MainNavTab.APPS -> BackAction.GoApps
    else -> BackAction.Exit
}

/** False when the system should handle back (move the task to the background, keeping the app alive). */
fun canHandleBack(s: NavSnapshot): Boolean = resolveBack(s) != BackAction.Exit
