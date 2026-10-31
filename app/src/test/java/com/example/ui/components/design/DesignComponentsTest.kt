package com.example.ui.components.design

import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Send
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.example.data.model.MainNavTab
import com.example.ui.theme.ShareAPKTheme
import com.example.ui.theme.ShareApkGradients
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DesignComponentsTest {
    @get:Rule val rule = createComposeRule()

    private val items = listOf(
        NavItem(MainNavTab.APPS, "التطبيقات", Icons.Outlined.Apps),
        NavItem(MainNavTab.SEND, "إرسال", Icons.Outlined.Send),
        NavItem(MainNavTab.RECEIVE, "استقبال", Icons.Outlined.Download),
        NavItem(MainNavTab.MORE, "المزيد", Icons.Outlined.MoreHoriz)
    )

    @Test
    fun bottomBarInvokesSelectWithTab() {
        var picked: MainNavTab? = null
        rule.setContent {
            ShareAPKTheme(darkTheme = true) {
                ShareApkBottomBar(items = items, current = MainNavTab.APPS, onSelect = { picked = it })
            }
        }
        rule.onNodeWithTag("nav_SEND").performClick()
        assertEquals(MainNavTab.SEND, picked)
    }

    @Test
    fun filterChipAllMapsToNull() {
        var selected: String? = "x"
        rule.setContent {
            ShareAPKTheme(darkTheme = true) {
                FilterChipRow(
                    allLabel = "الكل",
                    options = listOf("a" to "أ", "b" to "ب"),
                    selected = "a",
                    onSelect = { selected = it }
                )
            }
        }
        rule.onNodeWithText("الكل").performClick()
        assertEquals(null, selected)
        rule.onNodeWithText("ب").performClick()
        assertEquals("b", selected)
    }

    @Test
    fun gradientTileDisabledDoesNotClick() {
        var clicked = false
        rule.setContent {
            ShareAPKTheme(darkTheme = true) {
                GradientTile(
                    icon = Icons.Outlined.Apps, title = "التحديثات", subtitle = "x",
                    gradient = ShareApkGradients.rose, enabled = false, badge = "قريباً",
                    onClick = { clicked = true }
                )
            }
        }
        rule.onNodeWithText("قريباً").assertExists()
        rule.onNodeWithText("التحديثات").performClick()
        assertFalse(clicked)
    }

    @Test
    fun letterIconBoxIsDeterministic() {
        assertEquals(letterGradientIndex("سكينة"), letterGradientIndex("سكينة"))
        rule.setContent { ShareAPKTheme(darkTheme = true) { LetterIconBox(label = "سكينة") } }
        rule.onNodeWithText("س").assertExists()
    }

    @Test
    fun letterIconBoxHandlesBlankLabel() {
        rule.setContent { ShareAPKTheme(darkTheme = true) { LetterIconBox(label = "  ") } }
        rule.onNodeWithText("?").assertExists()
    }

    @Test
    fun longTitleTileStaysBoundedAtLargeFont() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f, fontScale = 1.3f)) {
                ShareAPKTheme(darkTheme = true) {
                    GradientTile(
                        icon = Icons.Outlined.Apps, title = "ع".repeat(80), subtitle = "و".repeat(200),
                        gradient = ShareApkGradients.green, onClick = {},
                        modifier = Modifier.width(170.dp).testTag("tile")
                    )
                }
            }
        }
        val h = rule.onNodeWithTag("tile").getUnclippedBoundsInRoot().height
        assertTrue("height=$h", h <= 220.dp)
    }

    @Test
    fun searchFieldClearButtonEmptiesQuery() {
        var value = "abc"
        rule.setContent { ShareAPKTheme(darkTheme = true) { SearchField(value = value, onValueChange = { value = it }, placeholder = "x") } }
        rule.onNodeWithContentDescription(com.example.ui.theme.ArabicStrings.clearSearch).performClick()
        assertEquals("", value)
    }
}
