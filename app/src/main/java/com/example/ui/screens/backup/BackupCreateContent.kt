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
    onCompressChange: (Boolean) -> Unit = {},
    /** (count, bytes) of the whole selection; null falls back to what is shown. */
    selectedTotals: Pair<Int, Long>? = null
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val mono = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace)
    val totalSelectedBytes = selectedTotals?.second ?: candidateApps.filter { it.packageName in selectedPackageNames }.sumOf { it.totalSizeBytes }
    val totalSelectedCount = selectedTotals?.first ?: selectedPackageNames.size
    val shownSelected = candidateApps.count { it.packageName in selectedPackageNames }
    val busy = backupServiceState.isRunning || backupServiceState.isVerifying

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
                        else s.backupProgressFormat.format(backupServiceState.currentAppIndex, backupServiceState.totalAppsCount),
                        style = MaterialTheme.typography.titleMedium, color = c.primaryText
                    )
                    Text(backupServiceState.currentAppLabel, style = MaterialTheme.typography.bodyMedium, color = c.onBackground)
                    if (backupServiceState.skippedFilesCount > 0) {
                        Text(
                            s.backupSkippedFormat.format(backupServiceState.skippedFilesCount),
                            style = MaterialTheme.typography.bodySmall, color = c.success
                        )
                    }
                    // The app being copied isn't done yet: count the ones before it. Verifying has no known length.
                    GradientProgressBar(
                        if (!backupServiceState.isVerifying && backupServiceState.totalAppsCount > 0)
                            (backupServiceState.currentAppIndex - 1).coerceAtLeast(0).toFloat() / backupServiceState.totalAppsCount
                        else null
                    )
                    SecondaryButton(s.cancelBtn, onCancelFullBackup, Modifier.align(Alignment.End))
                }
            }
        }

        // 1b. A stopped or failed backup must say so.
        backupServiceState.errorMessage?.takeIf { !busy }?.let { raw ->
            item {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.WarningAmber, null, tint = c.error)
                        Text(s.backupFailedTitle, style = MaterialTheme.typography.titleSmall, color = c.error)
                    }
                    Text(
                        if (raw.isBlank()) s.backupStoppedMsg else s.errorReasons.getValue(com.example.util.ErrorClassifier.classify(raw)),
                        style = MaterialTheme.typography.bodySmall, color = c.onBackground
                    )
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
                                    if (ok) s.backupVerifiedSuccess else s.backupCorruptFormat.format(report.corruptOrMissingFiles.joinToString("، ")),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (ok) c.success else c.error
                                )
                                Text(
                                    s.backupVerifiedDetailFormat.format(
                                        com.example.ui.theme.LocaleHelper.ltr(backupServiceState.completedFolderName.orEmpty()),
                                        report.verifiedMatchCount, report.totalFilesChecked
                                    ),
                                    style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant
                                )
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
                        text = if (backupServiceState.isFullyCompleted) s.backupDoneLabel else s.confirmChecklistAndFinish,
                        onClick = onConfirmSafetyChecklist,
                        // A backup with damaged files can't be confirmed as safe.
                        enabled = safetyCheck1 && safetyCheck2 && safetyCheck3 && !backupServiceState.isFullyCompleted &&
                            backupServiceState.verificationReport?.isAllVerified != false,
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
                        if (destinationUriString.isNullOrBlank()) {
                            Text(s.destinationNotChosen, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                        } else LtrBox {
                            Text(
                                android.net.Uri.decode(destinationUriString).substringAfterLast("/tree/"),
                                style = mono, color = c.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    SecondaryButton(s.pickBtn, onPickDestinationFolder, Modifier.testTag("pick_backup_destination_button"), icon = Icons.Outlined.FolderOpen)
                }
                // Free space is only meaningful once a destination is chosen and readable.
                spaceCheckResult?.takeIf { !destinationUriString.isNullOrBlank() && it.availableBytes != Long.MAX_VALUE }?.let { space ->
                    // Label and number are separate texts: one mixed RTL line scrambles "95.4 MB".
                    val tint = if (space.isSufficient) c.success else c.error
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        listOf(s.spaceNeededLabel to space.requiredBytes, s.spaceFreeLabel to space.availableBytes).forEach { (label, bytes) ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(label, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                                LtrBox { Text(FileNameAndHashUtils.formatBytes(bytes), style = MaterialTheme.typography.labelLarge, color = tint) }
                            }
                        }
                    }
                    if (!space.isSufficient) Text(s.spaceInsufficient, style = MaterialTheme.typography.bodySmall, color = c.error)
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
                        enabled = !destinationUriString.isNullOrBlank() && totalSelectedCount > 0 && !busy,
                        icon = Icons.Outlined.PlayArrow,
                        modifier = Modifier.weight(1f).testTag("start_full_backup_button")
                    )
                    // Only folder backups can resume; a ZIP is always rebuilt.
                    if (!compress && !backupServiceState.completedFolderName.isNullOrBlank()) {
                        SecondaryButton(
                            s.resumeBackupBtn,
                            { onStartFullBackup(backupServiceState.completedFolderName) },
                            enabled = !busy
                        )
                    }
                }
            }
        }

        // 4. Selection controls
        item {
            SurfaceCard(Modifier.fillMaxWidth()) {
                Text(
                    s.selectedCountFormat.format(
                        com.example.util.ArabicText.count(totalSelectedCount, s.appForms, s.isArabic),
                        com.example.ui.theme.LocaleHelper.ltr(FileNameAndHashUtils.formatBytes(totalSelectedBytes))
                    ),
                    style = MaterialTheme.typography.titleSmall, color = c.primaryText
                )
                // One checkbox for everything shown: checked = all, dash = some, empty = none.
                val allShown = candidateApps.isNotEmpty() && shownSelected == candidateApps.size
                val toggleAll = { if (allShown) onSelectNone() else onSelectAll() }
                Row(
                    Modifier.fillMaxWidth()
                        .toggleable(value = allShown, enabled = candidateApps.isNotEmpty(), role = androidx.compose.ui.semantics.Role.Checkbox, onValueChange = { toggleAll() })
                        .testTag("backup_select_all"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    androidx.compose.material3.TriStateCheckbox(
                        state = when {
                            allShown -> androidx.compose.ui.state.ToggleableState.On
                            shownSelected == 0 -> androidx.compose.ui.state.ToggleableState.Off
                            else -> androidx.compose.ui.state.ToggleableState.Indeterminate
                        },
                        onClick = null,
                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = c.primary, uncheckedColor = c.onSurfaceVariant)
                    )
                    Text(
                        if (searchQuery.isBlank()) s.selectAllCheck else s.selectAllShownCheck,
                        style = MaterialTheme.typography.bodyLarge, color = c.onBackground, modifier = Modifier.weight(1f)
                    )
                    LtrBox { Text("$shownSelected / ${candidateApps.size}", style = MaterialTheme.typography.labelLarge, color = c.onSurfaceVariant) }
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
