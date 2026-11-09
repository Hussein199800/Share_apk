package com.example.ui.screens.backup

import com.example.ui.components.design.AdaptiveList
import com.example.ui.components.design.fullLine
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BackupManifest
import com.example.data.model.BatchRestoreSummary
import com.example.data.model.RestoreAppStatus
import com.example.data.model.RestoreCandidateApp
import com.example.ui.components.design.BadgeTone
import com.example.ui.components.design.GradientProgressBar
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.SecondaryButton
import com.example.ui.components.design.StatusBadge
import com.example.ui.components.design.SurfaceCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens
import com.example.util.FileNameAndHashUtils

@Composable
internal fun BackupRestoreContent(
    restoreManifest: BackupManifest?,
    restoreCandidates: List<RestoreCandidateApp>,
    isBatchRestoring: Boolean,
    batchRestoreCurrentIndex: Int,
    batchRestoreCurrentLabel: String,
    batchRestoreSummary: BatchRestoreSummary?,
    onPickRestoreFolder: () -> Unit,
    onToggleRestoreCandidate: (String) -> Unit,
    onStartSequentialRestore: () -> Unit,
    onSkipCurrentRestoreApp: () -> Unit,
    onCancelBatchRestore: () -> Unit,
    onRetry: (Set<String>) -> Unit = {},
    onIgnore: (Set<String>) -> Unit = {},
    shizukuStatus: com.example.installer.ShizukuStatus = com.example.installer.ShizukuStatus.NOT_INSTALLED,
    onShizukuAllow: () -> Unit = {}
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val mono = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace)
    val selectedRestoreCount = restoreCandidates.count { it.isSelected }

    AdaptiveList(
        contentPadding = PaddingValues(horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp),
        gap = ShareApkTokens.CardGap,
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "shizuku") { ShizukuCard(shizukuStatus, onShizukuAllow) }
        item {
            SurfaceCard(Modifier.fillMaxWidth()) {
                PrimaryButton(
                    s.selectBackupFolderToRestore, onPickRestoreFolder,
                    Modifier.fillMaxWidth().testTag("pick_restore_folder_button"), icon = Icons.Outlined.FolderOpen
                )
                if (restoreManifest != null) {
                    HorizontalDivider(color = c.outline)
                    LtrBox {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Device: ${restoreManifest.deviceModel} (Android ${restoreManifest.androidRelease})",
                                style = MaterialTheme.typography.titleSmall, color = c.onBackground
                            )
                            Text(
                                "Backup Date: ${restoreManifest.backupDateIso} • ${restoreManifest.appCount} Apps (${FileNameAndHashUtils.formatBytes(restoreManifest.totalSizeBytes)})",
                                style = MaterialTheme.typography.bodySmall, color = c.primaryText
                            )
                        }
                    }
                    PrimaryButton(
                        "${s.startBatchRestoreBtn} ($selectedRestoreCount)", onStartSequentialRestore,
                        Modifier.fillMaxWidth().testTag("start_batch_restore_button"),
                        icon = Icons.Outlined.Restore,
                        enabled = selectedRestoreCount > 0 && !isBatchRestoring
                    )
                }
            }
        }

        if (isBatchRestoring) {
            item {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Sequential Batch Restore (#${batchRestoreCurrentIndex + 1}) — $batchRestoreCurrentLabel",
                        style = MaterialTheme.typography.titleMedium, color = c.primaryText
                    )
                    GradientProgressBar(progress = null)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton("Skip / تخطي", onSkipCurrentRestoreApp, Modifier.weight(1f), icon = Icons.Outlined.SkipNext)
                        SecondaryButton(s.cancelBtn, onCancelBatchRestore, Modifier.weight(1f))
                    }
                }
            }
        }

        if (batchRestoreSummary != null) {
            fullLine(key = "report") {
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(ShareApkTokens.CardGap)) {
                    RestoreReport(batchRestoreSummary, onRetry, onIgnore)
                }
            }
        }

        items(restoreCandidates, key = { it.entry.packageName }) { item ->
            val (statusLabel, tone) = when (item.status) {
                RestoreAppStatus.NOT_INSTALLED -> s.statusNotInstalled to BadgeTone.Success
                RestoreAppStatus.OLDER_VERSION_INSTALLED -> s.statusUpdateAvailable to BadgeTone.Primary
                RestoreAppStatus.ALREADY_INSTALLED -> s.statusAlreadyInstalled to BadgeTone.Info
                RestoreAppStatus.INCOMPATIBLE -> s.statusIncompatible to BadgeTone.Warning
            }
            val selectable = item.status != RestoreAppStatus.INCOMPATIBLE
            SurfaceCard(
                Modifier.fillMaxWidth(),
                onClick = if (selectable) ({ onToggleRestoreCandidate(item.entry.packageName) }) else null,
                contentPadding = PaddingValues(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(
                        checked = item.isSelected,
                        enabled = selectable && item.integrityOk,
                        onCheckedChange = { onToggleRestoreCandidate(item.entry.packageName) },
                        colors = CheckboxDefaults.colors(checkedColor = c.primary, uncheckedColor = c.onSurfaceVariant)
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.entry.label, style = MaterialTheme.typography.bodyLarge, color = c.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        LtrBox {
                            Text(
                                "${item.entry.packageName} • Backup v${item.entry.versionName} (${FileNameAndHashUtils.formatBytes(item.entry.totalSizeBytes)})",
                                style = mono, color = c.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                        }
                        StatusBadge(item.incompatibilityReason ?: statusLabel, tone)
                    }
                }
            }
        }
    }
}
