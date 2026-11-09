package com.example.ui.screens.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.backup.SpaceCheckResult
import com.example.data.model.BackupManifest
import com.example.data.model.BatchRestoreSummary
import com.example.data.model.InstalledAppInfo
import com.example.data.model.RestoreCandidateApp
import com.example.service.BackupServiceProgressState
import com.example.ui.components.design.ScreenScaffold
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens

@Composable
fun BackupScreen(
    candidateApps: List<InstalledAppInfo>,
    selectedPackageNames: Set<String>,
    includeSystemApps: Boolean,
    searchQuery: String,
    destinationUriString: String?,
    spaceCheckResult: SpaceCheckResult?,
    backupServiceState: BackupServiceProgressState,
    safetyCheck1: Boolean,
    safetyCheck2: Boolean,
    safetyCheck3: Boolean,
    restoreManifest: BackupManifest?,
    restoreCandidates: List<RestoreCandidateApp>,
    isBatchRestoring: Boolean,
    batchRestoreCurrentIndex: Int,
    batchRestoreCurrentLabel: String,
    batchRestoreSummary: BatchRestoreSummary?,
    onToggleIncludeSystem: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleAppSelection: (String) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onPickDestinationFolder: () -> Unit,
    onStartFullBackup: (resumeFolderName: String?) -> Unit,
    onCancelFullBackup: () -> Unit,
    onSafetyCheckChange: (Int, Boolean) -> Unit,
    onConfirmSafetyChecklist: () -> Unit,
    onPickRestoreFolder: () -> Unit,
    onToggleRestoreCandidate: (String) -> Unit,
    onStartSequentialRestore: () -> Unit,
    onSkipCurrentRestoreApp: () -> Unit,
    onCancelBatchRestore: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    compress: Boolean = true,
    onCompressChange: (Boolean) -> Unit = {},
    onRetryRestore: (Set<String>) -> Unit = {},
    onIgnoreRestore: (Set<String>) -> Unit = {},
    shizukuStatus: com.example.installer.ShizukuStatus = com.example.installer.ShizukuStatus.NOT_INSTALLED,
    onShizukuAllow: () -> Unit = {}
) {
    val s = LocalAppStrings.current
    var activeSubTab by rememberSaveable { mutableIntStateOf(0) }
    ScreenScaffold(title = s.tileBackupTitle, subtitle = s.backupRestoreHeader, onBack = onBack, modifier = modifier) {
        SegmentedTabs(
            labels = listOf(s.tabCreateBackup to "tab_create_backup", s.tabRestoreBackup to "tab_restore_backup"),
            selected = activeSubTab,
            onSelect = { activeSubTab = it },
            modifier = Modifier.padding(horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp)
        )
        if (activeSubTab == 0) {
            BackupCreateContent(
                candidateApps, selectedPackageNames, includeSystemApps, searchQuery, destinationUriString,
                spaceCheckResult, backupServiceState, safetyCheck1, safetyCheck2, safetyCheck3,
                onToggleIncludeSystem, onSearchQueryChange, onToggleAppSelection, onSelectAll, onSelectNone,
                onPickDestinationFolder, onStartFullBackup, onCancelFullBackup, onSafetyCheckChange, onConfirmSafetyChecklist,
                compress = compress, onCompressChange = onCompressChange
            )
        } else {
            BackupRestoreContent(
                restoreManifest, restoreCandidates, isBatchRestoring, batchRestoreCurrentIndex, batchRestoreCurrentLabel,
                batchRestoreSummary, onPickRestoreFolder, onToggleRestoreCandidate, onStartSequentialRestore,
                onSkipCurrentRestoreApp, onCancelBatchRestore,
                onRetry = onRetryRestore, onIgnore = onIgnoreRestore,
                shizukuStatus = shizukuStatus, onShizukuAllow = onShizukuAllow
            )
        }
    }
}

/** Two-option segmented control (gradient pill on the active option). */
@Composable
internal fun SegmentedTabs(
    labels: List<Pair<String, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = ShareApkTheme.colors
    val outer = RoundedCornerShape(ShareApkTokens.ButtonRadius)
    Row(
        modifier
            .fillMaxWidth()
            .clip(outer)
            .background(c.surface)
            .border(ShareApkTokens.Hairline, c.outline, outer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { i, (label, tag) ->
            val active = i == selected
            val pill = RoundedCornerShape(12.dp)
            Box(
                Modifier
                    .weight(1f)
                    .height(ShareApkTokens.MinTouch)
                    .clip(pill)
                    .then(if (active) Modifier.background(ShareApkGradients.primary.brush(135f)) else Modifier)
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(i) })
                    .testTag(tag),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (active) Color.White else c.onBackground, maxLines = 1)
            }
        }
    }
}

/** Checkbox + wrapping label; whole row toggles. */
@Composable
internal fun CheckRow(checked: Boolean, text: String, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = ShareApkTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = checked, enabled = enabled, role = Role.Checkbox, onClick = { onCheckedChange(!checked) })
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = c.primary, uncheckedColor = c.onSurfaceVariant),
            modifier = Modifier.padding(12.dp)
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.onBackground)
    }
}
