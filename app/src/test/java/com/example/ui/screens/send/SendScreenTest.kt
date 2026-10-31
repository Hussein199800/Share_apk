package com.example.ui.screens.send

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.receive.discovery.NearbyDevice
import com.example.receive.protocol.DeviceInfoDto
import com.example.send.SendState
import com.example.ui.screens.apps.fakeApp
import com.example.ui.theme.ArabicStrings
import com.example.ui.theme.ShareAPKTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class SendScreenTest {
    @get:Rule val rule = createComposeRule()
    private val desk = NearbyDevice(DeviceInfoDto(alias = "حاسوب المكتب", deviceType = "desktop", fingerprint = "f"), "192.168.1.10", 0)

    @Test fun tappingDeviceSendsWhenAppsSelected() {
        var target: NearbyDevice? = null
        rule.setContent {
            ShareAPKTheme(darkTheme = true) {
                SendScreen(listOf(fakeApp("a.b", "سكينة")), {}, {}, peers = listOf(desk), onSendTo = { target = it })
            }
        }
        rule.onNodeWithText(ArabicStrings.badgeLocalSend).assertExists()
        rule.onNodeWithTag("peer_192.168.1.10").performClick()
        assertEquals(desk, target)
    }

    @Test fun tappingDeviceWithoutSelectionDoesNothing() {
        var target: NearbyDevice? = null
        rule.setContent {
            ShareAPKTheme(darkTheme = true) { SendScreen(emptyList(), {}, {}, peers = listOf(desk), onSendTo = { target = it }) }
        }
        rule.onNodeWithText(ArabicStrings.selectAppsFirst).assertExists()
        rule.onNodeWithTag("peer_192.168.1.10").performClick()
        assertNull(target)
    }

    @Test fun rejectedResultIsShown() {
        rule.setContent {
            ShareAPKTheme(darkTheme = true) { SendScreen(emptyList(), {}, {}, sendState = SendState.Rejected(desk)) }
        }
        rule.onNodeWithText(ArabicStrings.sendRejected.format("حاسوب المكتب")).assertExists()
    }
}
