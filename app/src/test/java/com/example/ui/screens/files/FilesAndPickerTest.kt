package com.example.ui.screens.files

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.receive.discovery.NearbyDevice
import com.example.receive.protocol.DeviceInfoDto
import com.example.send.PickedItem
import com.example.send.SendState
import com.example.ui.screens.send.DevicePickerSheet
import com.example.ui.theme.ArabicStrings
import com.example.ui.theme.ShareAPKTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class FilesAndPickerTest {
    @get:Rule val rule = createComposeRule()

    @Test fun textSheetAddsText() {
        var added: String? = null
        rule.setContent {
            ShareAPKTheme(darkTheme = true) {
                FilesScreen(emptyList(), {}, {}, {}, { added = it }, {}, {}, {}, {})
            }
        }
        rule.onNodeWithText(ArabicStrings.filesEmpty).assertExists()
        rule.onNodeWithTag("pick_text").performClick()
        rule.onNodeWithTag("text_input").performTextInput("مرحبا")
        rule.onNodeWithText(ArabicStrings.addBtn).performClick()
        assertEquals("مرحبا", added)
    }

    @Test fun sendButtonAppearsWithItems() {
        var sent = false
        rule.setContent {
            ShareAPKTheme(darkTheme = true) {
                FilesScreen(listOf(PickedItem(1, "a.jpg", 10, "image/jpeg")), {}, {}, {}, {}, {}, {}, {}, { sent = true })
            }
        }
        rule.onNodeWithTag("files_send").performClick()
        assertTrue(sent)
    }

    private val desk = NearbyDevice(DeviceInfoDto(alias = "حاسوب المكتب", deviceType = "desktop", fingerprint = "f"), "192.168.1.10", 0)

    @Test fun pickerSendsToTappedDeviceOnlyWithItems() {
        var target: NearbyDevice? = null
        rule.setContent {
            ShareAPKTheme(darkTheme = true) {
                DevicePickerSheet(2, 100, listOf(desk), false, SendState.Idle, null, { target = it }, {}, {}, {}, {}, {})
            }
        }
        rule.onNodeWithText(ArabicStrings.badgeLocalSend).assertExists()
        rule.onNodeWithTag("peer_192.168.1.10").performClick()
        assertEquals(desk, target)
    }

    @Test fun pickerShowsRejection() {
        var target: NearbyDevice? = null
        rule.setContent {
            ShareAPKTheme(darkTheme = true) {
                DevicePickerSheet(0, 0, listOf(desk), false, SendState.Rejected(desk), null, { target = it }, {}, {}, {}, {}, {})
            }
        }
        rule.onNodeWithText(ArabicStrings.sendRejected.format("حاسوب المكتب")).assertExists()
        rule.onNodeWithTag("peer_192.168.1.10").performClick()
        assertNull(target)
    }
}
