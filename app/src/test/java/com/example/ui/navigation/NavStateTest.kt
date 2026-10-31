package com.example.ui.navigation

import com.example.data.model.MainNavTab
import org.junit.Assert.assertEquals
import org.junit.Test

class NavStateTest {
    @Test fun visibleSelectionClearedFirst() = assertEquals(
        BackAction.ClearSelection,
        resolveBack(NavSnapshot(MainNavTab.APPS, MoreDestination.HOME, hasSelection = true))
    )

    @Test fun hiddenSelectionDoesNotEatBackOnSubPage() = assertEquals(
        BackAction.MoreHome,
        resolveBack(NavSnapshot(MainNavTab.MORE, MoreDestination.SETTINGS, hasSelection = true))
    )

    @Test fun appsWithoutSelectionLeavesBackToSystem() =
        assertEquals(false, canHandleBack(NavSnapshot(MainNavTab.APPS, MoreDestination.HOME, hasSelection = false)))

    @Test fun subPageBackIsHandled() =
        assertEquals(true, canHandleBack(NavSnapshot(MainNavTab.MORE, MoreDestination.BACKUP, hasSelection = false)))

    @Test fun subPageReturnsToMoreHome() = assertEquals(
        BackAction.MoreHome,
        resolveBack(NavSnapshot(MainNavTab.MORE, MoreDestination.BACKUP, hasSelection = false))
    )

    @Test fun moreHomeGoesToApps() = assertEquals(
        BackAction.GoApps,
        resolveBack(NavSnapshot(MainNavTab.MORE, MoreDestination.HOME, hasSelection = false))
    )

    @Test fun otherTabGoesToApps() = assertEquals(
        BackAction.GoApps,
        resolveBack(NavSnapshot(MainNavTab.SEND, MoreDestination.HOME, hasSelection = false))
    )

    @Test fun appsTabExits() = assertEquals(
        BackAction.Exit,
        resolveBack(NavSnapshot(MainNavTab.APPS, MoreDestination.HOME, hasSelection = false))
    )
}
