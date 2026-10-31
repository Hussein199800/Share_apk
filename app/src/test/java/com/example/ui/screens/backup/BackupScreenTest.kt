package com.example.ui.screens.backup

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.service.BackupServiceProgressState
import com.example.ui.theme.ArabicStrings
import com.example.ui.theme.ShareAPKTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class BackupScreenTest {
    @get:Rule val rule = createComposeRule()

    private var backPressed = false

    private fun show(state: BackupServiceProgressState, checks: Triple<Boolean, Boolean, Boolean> = Triple(false, false, false)) =
        rule.setContent {
            ShareAPKTheme(darkTheme = true) {
                BackupScreen(
                    candidateApps = emptyList(), selectedPackageNames = emptySet(), includeSystemApps = false,
                    searchQuery = "", destinationUriString = null, spaceCheckResult = null,
                    backupServiceState = state,
                    safetyCheck1 = checks.first, safetyCheck2 = checks.second, safetyCheck3 = checks.third,
                    restoreManifest = null, restoreCandidates = emptyList(), isBatchRestoring = false,
                    batchRestoreCurrentIndex = 0, batchRestoreCurrentLabel = "", batchRestoreSummary = null,
                    onToggleIncludeSystem = {}, onSearchQueryChange = {}, onToggleAppSelection = {},
                    onSelectAll = {}, onSelectNone = {}, onPickDestinationFolder = {}, onStartFullBackup = {},
                    onCancelFullBackup = {}, onSafetyCheckChange = { _, _ -> }, onConfirmSafetyChecklist = {},
                    onPickRestoreFolder = {}, onToggleRestoreCandidate = {}, onStartSequentialRestore = {},
                    onSkipCurrentRestoreApp = {}, onCancelBatchRestore = {},
                    onBack = { backPressed = true }
                )
            }
        }

    @Test
    fun confirmChecklistDisabledUntilAllChecked() {
        show(BackupServiceProgressState(awaitingSafetyChecklist = true), Triple(true, true, false))
        rule.onNodeWithTag("confirm_safety_checklist_button").assertIsNotEnabled()
    }

    @Test
    fun confirmChecklistEnabledWhenAllChecked() {
        show(BackupServiceProgressState(awaitingSafetyChecklist = true), Triple(true, true, true))
        rule.onNodeWithTag("confirm_safety_checklist_button").assertIsEnabled()
    }

    @Test
    fun progressShownWhileRunning() {
        show(BackupServiceProgressState(isRunning = true, currentAppIndex = 2, totalAppsCount = 4))
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertExists()
    }

    @Test
    fun startBackupDisabledWithoutDestination() {
        show(BackupServiceProgressState())
        rule.onNodeWithTag("start_full_backup_button").assertIsNotEnabled()
    }

    @Test
    fun backButtonInvokesOnBack() {
        show(BackupServiceProgressState())
        rule.onNodeWithContentDescription(ArabicStrings.navBack).performClick()
        assertTrue(backPressed)
    }
}
