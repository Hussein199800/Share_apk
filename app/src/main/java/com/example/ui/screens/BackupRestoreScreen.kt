package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.backup.SpaceCheckResult
import com.example.data.model.BackupManifest
import com.example.data.model.BatchRestoreSummary
import com.example.data.model.InstalledAppInfo
import com.example.data.model.RestoreAppStatus
import com.example.data.model.RestoreCandidateApp
import com.example.service.BackupServiceProgressState
import com.example.ui.theme.AmberWarningDark
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.EmeraldVerifiedDark
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.util.FileNameAndHashUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
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
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var activeSubTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = BurnishedGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = strings.backupRestoreHeader,
                        style = MaterialTheme.typography.titleLarge,
                        color = BurnishedGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                SecondaryTabRow(selectedTabIndex = activeSubTab) {
                    Tab(
                        selected = activeSubTab == 0,
                        onClick = { activeSubTab = 0 },
                        text = { Text(strings.tabCreateBackup, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Backup, contentDescription = null) },
                        modifier = Modifier.testTag("tab_create_backup")
                    )
                    Tab(
                        selected = activeSubTab == 1,
                        onClick = { activeSubTab = 1 },
                        text = { Text(strings.tabRestoreBackup, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Restore, contentDescription = null) },
                        modifier = Modifier.testTag("tab_restore_backup")
                    )
                }
            }
        }

        if (activeSubTab == 0) {
            FlowACreateBackupContent(
                candidateApps = candidateApps,
                selectedPackageNames = selectedPackageNames,
                includeSystemApps = includeSystemApps,
                searchQuery = searchQuery,
                destinationUriString = destinationUriString,
                spaceCheckResult = spaceCheckResult,
                backupServiceState = backupServiceState,
                safetyCheck1 = safetyCheck1,
                safetyCheck2 = safetyCheck2,
                safetyCheck3 = safetyCheck3,
                onToggleIncludeSystem = onToggleIncludeSystem,
                onSearchQueryChange = onSearchQueryChange,
                onToggleAppSelection = onToggleAppSelection,
                onSelectAll = onSelectAll,
                onSelectNone = onSelectNone,
                onPickDestinationFolder = onPickDestinationFolder,
                onStartFullBackup = onStartFullBackup,
                onCancelFullBackup = onCancelFullBackup,
                onSafetyCheckChange = onSafetyCheckChange,
                onConfirmSafetyChecklist = onConfirmSafetyChecklist
            )
        } else {
            FlowBRestoreBackupContent(
                restoreManifest = restoreManifest,
                restoreCandidates = restoreCandidates,
                isBatchRestoring = isBatchRestoring,
                batchRestoreCurrentIndex = batchRestoreCurrentIndex,
                batchRestoreCurrentLabel = batchRestoreCurrentLabel,
                batchRestoreSummary = batchRestoreSummary,
                onPickRestoreFolder = onPickRestoreFolder,
                onToggleRestoreCandidate = onToggleRestoreCandidate,
                onStartSequentialRestore = onStartSequentialRestore,
                onSkipCurrentRestoreApp = onSkipCurrentRestoreApp,
                onCancelBatchRestore = onCancelBatchRestore
            )
        }
    }
}

@Composable
private fun FlowACreateBackupContent(
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
    onConfirmSafetyChecklist: () -> Unit
) {
    val strings = LocalAppStrings.current
    val totalSelectedBytes = candidateApps
        .filter { selectedPackageNames.contains(it.packageName) }
        .sumOf { it.totalSizeBytes }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Foreground Service Active Progress OR Mandatory Pre-Flash Safety Checklist
        if (backupServiceState.isRunning || backupServiceState.isVerifying) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, BurnishedGold, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (backupServiceState.isVerifying) {
                                strings.verifyBackupIntegrityBtn
                            } else {
                                "ForegroundService: ${backupServiceState.currentAppIndex} / ${backupServiceState.totalAppsCount}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = BurnishedGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = backupServiceState.currentAppLabel,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (backupServiceState.skippedFilesCount > 0) {
                            Text(
                                text = "Resumed / Skipped matching SHA-256 files: ${backupServiceState.skippedFilesCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldVerifiedDark
                            )
                        }
                        LinearProgressIndicator(
                            progress = {
                                if (backupServiceState.totalAppsCount > 0) {
                                    backupServiceState.currentAppIndex.toFloat() / backupServiceState.totalAppsCount
                                } else 0f
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(
                            onClick = onCancelFullBackup,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(strings.cancelBtn)
                        }
                    }
                }
            }
        }

        // Mandatory 3-Item Pre-Flash Safety Checklist (Cannot be skipped before completion)
        if (backupServiceState.awaitingSafetyChecklist || backupServiceState.isFullyCompleted) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color = if (backupServiceState.isFullyCompleted) EmeraldVerifiedDark else BurnishedGold,
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Post-Write SHA-256 Verification Report Banner
                        backupServiceState.verificationReport?.let { report ->
                            Surface(
                                color = if (report.isAllVerified) {
                                    EmeraldVerifiedDark.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (report.isAllVerified) Icons.Default.VerifiedUser else Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = if (report.isAllVerified) EmeraldVerifiedDark else MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (report.isAllVerified) {
                                                strings.backupVerifiedSuccess
                                            } else {
                                                "Corrupt/Mismatched files: ${report.corruptOrMissingFiles.joinToString()}"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (report.isAllVerified) EmeraldVerifiedDark else MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        LtrBox {
                                            Text(
                                                text = "Folder: ${backupServiceState.completedFolderName} • Verified ${report.verifiedMatchCount}/${report.totalFilesChecked} APK files + 00_Install_ShareAPK_First.apk + manifest.json + apps_list.html",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = BurnishedGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.safetyChecklistTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BurnishedGold
                            )
                        }

                        SafetyCheckboxRow(
                            checked = safetyCheck1,
                            text = strings.safetyCheck1,
                            onCheckedChange = { onSafetyCheckChange(1, it) },
                            tag = "safety_check_1"
                        )
                        SafetyCheckboxRow(
                            checked = safetyCheck2,
                            text = strings.safetyCheck2,
                            onCheckedChange = { onSafetyCheckChange(2, it) },
                            tag = "safety_check_2"
                        )
                        SafetyCheckboxRow(
                            checked = safetyCheck3,
                            text = strings.safetyCheck3,
                            onCheckedChange = { onSafetyCheckChange(3, it) },
                            tag = "safety_check_3"
                        )

                        Button(
                            onClick = onConfirmSafetyChecklist,
                            enabled = safetyCheck1 && safetyCheck2 && safetyCheck3 && !backupServiceState.isFullyCompleted,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (backupServiceState.isFullyCompleted) EmeraldVerifiedDark else BurnishedGold,
                                contentColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_safety_checklist_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (backupServiceState.isFullyCompleted) {
                                    "✓ Pre-Flash Backup Verified & Ready"
                                } else {
                                    strings.confirmChecklistAndFinish
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. SAF Destination & Free Space Check Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.chooseDestinationBtn,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            LtrBox {
                                Text(
                                    text = destinationUriString ?: "Not selected yet (Pick USB OTG / SD Card / Folder)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = onPickDestinationFolder,
                            modifier = Modifier.testTag("pick_backup_destination_button")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SAF")
                        }
                    }

                    spaceCheckResult?.let { space ->
                        Surface(
                            color = if (space.isSufficient) {
                                EmeraldVerifiedDark.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            LtrBox {
                                Text(
                                    text = "Required: ${FileNameAndHashUtils.formatBytes(space.requiredBytes)} • Available Free Space: ${FileNameAndHashUtils.formatBytes(space.availableBytes)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (space.isSufficient) EmeraldVerifiedDark else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onStartFullBackup(null) },
                            enabled = !destinationUriString.isNullOrBlank() &&
                                selectedPackageNames.isNotEmpty() &&
                                !backupServiceState.isRunning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BurnishedGold,
                                contentColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_full_backup_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.startFullBackupBtn, fontWeight = FontWeight.Bold)
                        }

                        if (!backupServiceState.completedFolderName.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = { onStartFullBackup(backupServiceState.completedFolderName) },
                                enabled = !backupServiceState.isRunning
                            ) {
                                Text(strings.resumeBackupBtn)
                            }
                        }
                    }
                }
            }
        }

        // 3. App Selection Controls (Include System Apps Toggle, Select All / None, Live Total Size)
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = String.format(
                                strings.selectedCountFormat,
                                selectedPackageNames.size,
                                FileNameAndHashUtils.formatBytes(totalSelectedBytes)
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = BurnishedGold,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = onSelectAll,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(strings.selectAllBtn, style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = onSelectNone,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(strings.selectNoneBtn, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.includeSystemAppsToggle,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = includeSystemApps,
                            onCheckedChange = onToggleIncludeSystem
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(strings.searchHint) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 4. App Selection Rows
        items(candidateApps, key = { it.packageName }) { app ->
            val checked = selectedPackageNames.contains(app.packageName)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleAppSelection(app.packageName) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggleAppSelection(app.packageName) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (app.iconBitmap != null) {
                        Image(
                            bitmap = app.iconBitmap.asImageBitmap(),
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = null,
                            tint = BurnishedGold,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        LtrBox {
                            Text(
                                text = "${app.packageName} • v${app.versionName} • ${FileNameAndHashUtils.formatBytes(app.totalSizeBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (app.isSplitApk) {
                        Surface(
                            color = BurnishedGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = ".apks",
                                style = MaterialTheme.typography.labelSmall,
                                color = BurnishedGold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowBRestoreBackupContent(
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
    onCancelBatchRestore: () -> Unit
) {
    val strings = LocalAppStrings.current
    val selectedRestoreCount = restoreCandidates.count { it.isSelected }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onPickRestoreFolder,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BurnishedGold,
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_restore_folder_button")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.selectBackupFolderToRestore, fontWeight = FontWeight.Bold)
                    }

                    if (restoreManifest != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        LtrBox {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Device: ${restoreManifest.deviceModel} (Android ${restoreManifest.androidRelease})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Backup Date: ${restoreManifest.backupDateIso} • ${restoreManifest.appCount} Apps (${FileNameAndHashUtils.formatBytes(restoreManifest.totalSizeBytes)})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = BurnishedGold
                                )
                            }
                        }

                        Button(
                            onClick = onStartSequentialRestore,
                            enabled = selectedRestoreCount > 0 && !isBatchRestoring,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_batch_restore_button")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${strings.startBatchRestoreBtn} ($selectedRestoreCount)")
                        }
                    }
                }
            }
        }

        // Active Sequential Batch Restore Queue Progress Card
        if (isBatchRestoring) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, BurnishedGold, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Sequential Batch Restore (#${batchRestoreCurrentIndex + 1}) — $batchRestoreCurrentLabel",
                            style = MaterialTheme.typography.titleMedium,
                            color = BurnishedGold,
                            fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onSkipCurrentRestoreApp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Skip / تخطي")
                            }
                            OutlinedButton(
                                onClick = onCancelBatchRestore,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(strings.cancelBtn)
                            }
                        }
                    }
                }
            }
        }

        // Batch Restore Final Summary Report
        if (batchRestoreSummary != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, EmeraldVerifiedDark, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "تقرير الاسترجاع النهائي / Restore Summary Report",
                            style = MaterialTheme.typography.titleMedium,
                            color = BurnishedGold,
                            fontWeight = FontWeight.Bold
                        )
                        LtrBox {
                            Text(
                                text = "Installed: ${batchRestoreSummary.installedCount} • Skipped: ${batchRestoreSummary.skippedCount} • Failed: ${batchRestoreSummary.failedCount}",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldVerifiedDark
                            )
                        }
                        batchRestoreSummary.details.forEach { line ->
                            LtrBox {
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Restore Candidates List categorized by Status
        items(restoreCandidates, key = { it.entry.packageName }) { item ->
            val (statusLabel, badgeColor) = when (item.status) {
                RestoreAppStatus.NOT_INSTALLED -> strings.statusNotInstalled to EmeraldVerifiedDark
                RestoreAppStatus.OLDER_VERSION_INSTALLED -> strings.statusUpdateAvailable to BurnishedGold
                RestoreAppStatus.ALREADY_INSTALLED -> strings.statusAlreadyInstalled to MaterialTheme.colorScheme.onSurfaceVariant
                RestoreAppStatus.INCOMPATIBLE -> strings.statusIncompatible to AmberWarningDark
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = item.status != RestoreAppStatus.INCOMPATIBLE) {
                        onToggleRestoreCandidate(item.entry.packageName)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.isSelected,
                        enabled = item.status != RestoreAppStatus.INCOMPATIBLE && item.integrityOk,
                        onCheckedChange = { onToggleRestoreCandidate(item.entry.packageName) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.entry.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        LtrBox {
                            Text(
                                text = "${item.entry.packageName} • Backup v${item.entry.versionName} (${FileNameAndHashUtils.formatBytes(item.entry.totalSizeBytes)})",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = badgeColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.incompatibilityReason ?: statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SafetyCheckboxRow(
    checked: Boolean,
    text: String,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
