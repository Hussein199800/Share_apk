package com.example.ui.screens.receive

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.receive.IncomingRequest
import com.example.receive.ReceiveState
import com.example.receive.ReceivedItem
import com.example.receive.ReceiverStatus
import com.example.receive.VersionStatus
import com.example.receive.protocol.DeviceInfoDto
import com.example.receive.protocol.FileDto
import com.example.receive.protocol.ShareApkMetaDto
import com.example.ui.theme.ArabicStrings
import com.example.ui.theme.ShareAPKTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class ReceiveUiTest {
    @get:Rule val rule = createComposeRule()
    private val status = ReceiverStatus(running = true, alias = "Pixel 8", ip = "192.168.1.17", port = 53317)

    private fun screen(items: List<ReceivedItem>, onInstall: (ReceivedItem) -> Unit = {}) = rule.setContent {
        ShareAPKTheme(darkTheme = true) {
            ReceiveScreen(status, true, ReceiveState.Idle, items, {}, {}, {}, onInstall, {})
        }
    }

    @Test fun emptyListShowsHint() {
        screen(emptyList())
        rule.onNodeWithText(ArabicStrings.receivedEmpty).assertExists()
        rule.onNodeWithText("Pixel 8").assertExists()
    }

    @Test fun installButtonInvokesAction() {
        var installed: ReceivedItem? = null
        val item = ReceivedItem(File("/x/a.apk"), "سكينة", "com.sakina", "1.3.0", 130, VersionStatus.UPDATE, null, 0)
        screen(listOf(item)) { installed = it }
        rule.onNodeWithText(ArabicStrings.vsUpdate).assertExists()
        rule.onNodeWithText(ArabicStrings.installNowShort).performClick()
        assertEquals(item, installed)
    }

    @Test fun incomingSheetShowsOlderWarningAndAccepts() {
        var accepted: Boolean? = null
        val req = IncomingRequest(
            DeviceInfoDto(alias = "هاتف أحمد", fingerprint = "f"), "10.0.0.2",
            listOf(FileDto("a", "Taqati.apk", 10, shareApk = ShareApkMetaDto("com.taqati", "2.0.1", 201))), rejectedCount = 1
        )
        rule.setContent {
            ShareAPKTheme(darkTheme = true) {
                IncomingRequestSheet(req, statusFor = { VersionStatus.OLDER }, onDecide = { accepted = it })
            }
        }
        rule.onNodeWithText(ArabicStrings.vsOlder).assertExists()
        rule.onNodeWithText(ArabicStrings.incomingOlderWarning).assertExists()
        rule.onNodeWithText(ArabicStrings.incomingRejectedFiles.format(1)).assertExists()
        rule.onNodeWithText(ArabicStrings.acceptReceiveBtn).performClick()
        assertEquals(true, accepted)
    }
}
