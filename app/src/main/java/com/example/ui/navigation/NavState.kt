package com.example.ui.navigation

import com.example.data.model.MainNavTab

/** Sub-pages hosted inside the More tab. */
enum class MoreDestination { HOME, BACKUP, HISTORY, SETTINGS, ABOUT }

data class NavSnapshot(val tab: MainNavTab, val more: MoreDestination, val hasSelection: Boolean)

sealed interface BackAction {
    data object ClearSelection : BackAction
    data object MoreHome : BackAction
    data object GoApps : BackAction
    data object Exit : BackAction
}

/** Back order: clear selection → leave a More sub-page → return to Apps → exit. */
fun resolveBack(s: NavSnapshot): BackAction = when {
    s.hasSelection -> BackAction.ClearSelection
    s.tab == MainNavTab.MORE && s.more != MoreDestination.HOME -> BackAction.MoreHome
    s.tab != MainNavTab.APPS -> BackAction.GoApps
    else -> BackAction.Exit
}
