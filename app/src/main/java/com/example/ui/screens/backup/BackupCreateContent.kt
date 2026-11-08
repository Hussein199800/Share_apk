package com.example.ui.screens.backup

import androidx.compose.foundation.selection.toggleable
import com.example.ui.components.design.AdaptiveList
import com.example.ui.components.design.fullLine
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.backup.SpaceCheckResult
import com.example.data.model.InstalledAppInfo
import com.example.service.BackupServiceProgressState
import com.example.ui.components.design.AppIcon
import com.example.ui.components.design.BadgeTone
import com.example.ui.components.design.GradientProgressBar
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.SearchField
import com.example.ui.components.design.SecondaryButton
import com.example.ui.components.design.StatusBadge
import com.example.ui.components.design.SurfaceCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens
import com.example.util.FileNameAndHashUtils

@Composable
internal fun BackupCreateContent(
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
    compress: Boolean = true,
    onCompressChange: (Boolean) -> Unit = {}
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val mono = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace)
    val totalSelectedBytes = candidateApps.filter { it.packageName in selectedPackageNames }.sumOf { it.totalSizeBytes }

    AdaptiveList(
        contentPadding = PaddingValues(horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp),
        gap = ShareApkTokens.CardGap,
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Running / verifying progress
        if (backupServiceState.isRunning || backupServiceState.isVerifying) {
            item {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Text(
                        if (backupServiceState.isVerifying) s.verifyBackupIntegrityBtn
                        else "ForegroundService: ${backupServiceState.currentAppIndex} / ${backupServiceState.totalAppsCount}",
                        style = MaterialTheme.typography.titleMedium, color = c.primaryText
                    )
                    Text(backupServiceState.currentAppLabel, style = MaterialTheme.typography.bodyMedium, color = c.onBackground)
                    if (backupServiceState.skippedFilesCount > 0) {
                        Text(
                            "Resumed / Skipped matching SHA-256 files: ${backupServiceState.skippedFilesCount}",
                            style = MaterialTheme.typography.bodySmall, color = c.success
                        )
                    }
                    GradientProgressBar(
                        if (backupServiceState.totalAppsCount > 0)
                            backupServiceState.currentAppIndex.toFloat() / backupServiceState.totalAppsCount
                        else null
                    )
                    SecondaryButton(s.cancelBtn, onCancelFullBackup, Modifier.align(Alignment.End))
                }
            }
        }

        // 2. Mandatory safety checklist / completion
        if (backupServiceState.awaitingSafetyChecklist || backupServiceState.isFullyCompleted) {
            item {
                SurfaceCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    backupServiceState.verificationReport?.let { report ->
                        val ok = report.isAllVerified
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                if (ok) Icons.Outlined.VerifiedUser else Icons.Outlined.WarningAmber, null,
                                tint = if (ok) c.success else c.error
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (ok) s.backupVerifiedSuccess else "Corrupt/Mismatched files: ${report.corruptOrMissingFiles.joinToString()}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (ok) c.success else c.error
                                )
                                LtrBox {
                                    Text(
                                        "Folder: ${backupServiceState.completedFolderName} • Verified ${report.verifiedMatchCount}/${report.totalFilesChecked} APK files + 00_Install_ShareAPK_First.apk + manifest.json + apps_list.html",
                                        style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.WarningAmber, null, tint = c.warning)
                        Text(s.safetyChecklistTitle, style = MaterialTheme.typography.titleMedium, color = c.onBackground)
                    }
                    CheckRow(safetyCheck1, s.safetyCheck1, { onSafetyCheckChange(1, it) }, Modifier.testTag("safety_check_1"))
                    CheckRow(safetyCheck2, s.safetyCheck2, { onSafetyCheckChange(2, it) }, Modifier.testTag("safety_check_2"))
                    CheckRow(safetyCheck3, s.safetyCheck3, { onSafetyCheckChange(3, it) }, Modifier.testTag("safety_check_3"))
                    PrimaryButton(
                        text = if (backupServiceState.isFullyCompleted) "✓ Pre-Flash Backup Verified & Ready" else s.confirmChecklistAndFinish,
                        onClick = onConfirmSafetyChecklist,
                        enabled = safetyCheck1 && safetyCheck2 && safetyCheck3 && !backupServiceState.isFullyCompleted,
                        icon = Icons.Outlined.CheckCircle,
                        modifier = Modifier.fillMaxWidth().testTag("confirm_safety_checklist_button")
                    )
                }
            }
        }

        // 3. Destination + free space + start
        item {
            SurfaceCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(s.chooseDestinationBtn, style = MaterialTheme.typography.titleSmall, color = c.onBackground)
                        LtrBox {
                            Text(
                                destinationUriString ?: "Not selected yet (Pick USB OTG / SD Card / Folder)",
                                style = mono, color = c.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    SecondaryButton("SAF", onPickDestinationFolder, Modifier.testTag("pick_backup_destination_button"), icon = Icons.Outlined.FolderOpen)
                }
                spaceCheckResult?.let { space ->
                    LtrBox {
                        StatusBadge(
                            "Required: ${FileNameAndHashUtils.formatBytes(space.requiredBytes)} • Available Free Space: ${FileNameAndHashUtils.formatBytes(space.availableBytes)}",
                            if (space.isSufficient) BadgeTone.Success else BadgeTone.Error
                        )
                    }
                }
                // Whole row is the toggle, so TalkBack reads the label with the switch.
                Row(
                    Modifier.toggleable(value = compress, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = onCompressChange)
                        .testTag("backup_compress_toggle"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(s.backupCompressTitle, style = MaterialTheme.typography.titleSmall, color = c.onBackground)
                        Text(s.backupCompressHint, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    }
                    androidx.compose.material3.Switch(checked = compress, onCheckedChange = null)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(
                        text = s.startFullBackupBtn,
                        onClick = { onStartFullBackup(null) },
                        enabled = !destinationUriString.isNullOrBlank() && selectedPackageNames.isNotEmpty() && !backupServiceState.isRunning,
                        icon = Icons.Outlined.PlayArrow,
                        modifier = Modifier.weight(1f).testTag("start_full_backup_button")
                    )
                    if (!backupServiceState.completedFolderName.isNullOrBlank()) {
                        SecondaryButton(
                            s.resumeBackupBtn,
                            { onStartFullBackup(backupServiceState.completedFolderName) },
                            enabled = !backupServiceState.isRunning
                        )
                    }
                }
            }
        }

        // 4. Selection controls
        item {
            SurfaceCard(Modifier.fillMaxWidth()) {
                Text(
                    s.selectedCountFormat.format(com.example.util.ArabicText.count(selectedPackageNames.size, s.appForms, s.isArabic), FileNameAndHashUtils.formatBytes(totalSelectedBytes)),
                    style = MaterialTheme.typography.titleSmall, color = c.primaryText
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(s.selectAllBtn, onSelectAll, Modifier.weight(1f))
                    SecondaryButton(s.selectNoneBtn, onSelectNone, Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.includeSystemAppsToggle, style = MaterialTheme.typography.bodyMedium, color = c.onBackground, modifier = Modifier.weight(1f))
                    Switch(
                        checked = includeSystemApps,
                        onCheckedChange = onToggleIncludeSystem,
                        colors = SwitchDefaults.colors(checkedTrackColor = c.primary)
                    )
                }
                SearchField(searchQuery, onSearchQueryChange, s.searchHint)
            }
        }

        // 5. App rows
        items(candidateApps, key = { it.packageName }) { app ->
            SurfaceCard(
                Modifier.fillMaxWidth(),
                onClick = { onToggleAppSelection(app.packageName) },
                contentPadding = PaddingValues(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.Checkbox(
                        checked = app.packageName in selectedPackageNames,
                        onCheckedChange = { onToggleAppSelection(app.packageName) },
                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = c.primary, uncheckedColor = c.onSurfaceVariant)
                    )
                    AppIcon(app.label, app.iconBitmap, size = 36.dp)
                    Column(Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.bodyLarge, color = c.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        LtrBox {
                            Text(
                                "${app.packageName} • v${app.versionName} • ${FileNameAndHashUtils.formatBytes(app.totalSizeBytes)}",
                                style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (app.isSplitApk) StatusBadge(".apks", BadgeTone.Info)
                }
            }
        }
    }
}
