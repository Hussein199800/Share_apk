package com.example.ui

import android.Manifest
import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MainNavTab
import com.example.data.model.ThemePreference
import com.example.ui.components.sheets.InstallStatusResultDialog
import com.example.ui.components.sheets.OnboardingPagerDialog
import com.example.ui.components.sheets.PreInstallValidationSheet
import com.example.ui.components.sheets.SplitApkWarningDialog
import com.example.ui.components.sheets.UnknownSourcesPermissionDialog
import com.example.ui.components.sheets.WifiShareServerDialog
import com.example.ui.navigation.MoreDestination
import com.example.ui.navigation.NavSnapshot
import com.example.ui.navigation.canHandleBack
import com.example.ui.screens.apps.AppsScreen
import com.example.ui.screens.backup.BackupScreen
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.settings.AboutScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.more.MoreScreen
import com.example.receive.ReceiveState
import com.example.ui.screens.receive.IncomingRequestSheet
import com.example.ui.screens.receive.ReceiveScreen
import com.example.ui.screens.send.SendScreen
import com.example.ui.theme.LocaleHelper
import com.example.ui.viewmodel.ShareApkViewModel


@Composable
fun ShareApkAppRoot(
    viewModel: ShareApkViewModel,
    isExpandedScreen: Boolean
) {
    val context = LocalContext.current
    val prefs by viewModel.userPrefs.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val apps by viewModel.filteredAndSortedApps.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val selectedPackageNames by viewModel.selectedPackageNames.collectAsStateWithLifecycle()
    val focusedApp by viewModel.focusedApp.collectAsStateWithLifecycle()
    val computedSha256Map by viewModel.computedApkSha256Map.collectAsStateWithLifecycle()
    val isComputingSha256 by viewModel.isComputingSha256.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val showOpenDownloadsAction by viewModel.showOpenDownloadsAction.collectAsStateWithLifecycle()
    val pendingSplitAction by viewModel.pendingSplitAction.collectAsStateWithLifecycle()
    val quickZipProgress by viewModel.quickZipProgress.collectAsStateWithLifecycle()
    val showWifiDialog by viewModel.showWifiDialog.collectAsStateWithLifecycle()
    val wifiServerState by viewModel.wifiServerState.collectAsStateWithLifecycle()
    val preInstallValidation by viewModel.preInstallValidation.collectAsStateWithLifecycle()
    val isCommittingInstall by viewModel.isCommittingInstall.collectAsStateWithLifecycle()
    val showUnknownSourcesDialog by viewModel.showUnknownSourcesDialog.collectAsStateWithLifecycle()
    val latestInstallResult by viewModel.latestInstallResult.collectAsStateWithLifecycle()

    // Backup & Restore state
    val backupCandidates by viewModel.backupCandidateApps.collectAsStateWithLifecycle()
    val backupSelectedPackages by viewModel.backupSelectedPackages.collectAsStateWithLifecycle()
    val backupIncludeSystem by viewModel.backupIncludeSystemApps.collectAsStateWithLifecycle()
    val backupSearchQuery by viewModel.backupSearchQuery.collectAsStateWithLifecycle()
    val spaceCheckResult by viewModel.spaceCheckResult.collectAsStateWithLifecycle()
    val backupServiceState by viewModel.backupServiceState.collectAsStateWithLifecycle()
    val safetyCheck1 by viewModel.safetyCheck1.collectAsStateWithLifecycle()
    val safetyCheck2 by viewModel.safetyCheck2.collectAsStateWithLifecycle()
    val safetyCheck3 by viewModel.safetyCheck3.collectAsStateWithLifecycle()
    val restoreManifest by viewModel.restoreManifest.collectAsStateWithLifecycle()
    val restoreCandidates by viewModel.restoreCandidates.collectAsStateWithLifecycle()
    val isBatchRestoring by viewModel.isBatchRestoring.collectAsStateWithLifecycle()
    val batchRestoreCurrentIndex by viewModel.batchRestoreCurrentIndex.collectAsStateWithLifecycle()
    val batchRestoreCurrentLabel by viewModel.batchRestoreCurrentLabel.collectAsStateWithLifecycle()
    val batchRestoreSummary by viewModel.batchRestoreSummary.collectAsStateWithLifecycle()

    // History state
    val historyItems by viewModel.historyItems.collectAsStateWithLifecycle()

    val strings = remember(prefs.languagePreference) {
        LocaleHelper.resolveStrings(prefs.languagePreference)
    }
    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = when (prefs.themePreference) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> systemDark
    }

    // Document Pickers & Permissions
    val openExternalApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.inspectAndPrepareInstall(uri)
        }
    }

    val pickBackupDestinationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(treeUri, takeFlags) }
            viewModel.onBackupDestinationPicked(treeUri)
        }
    }

    val pickRestoreFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { context.contentResolver.takePersistableUriPermission(treeUri, takeFlags) }
            viewModel.loadBackupFolderForRestore(treeUri)
        }
    }

    // Android 13+ (API 33+) runtime notification permission for ForegroundService progress
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.startFullPreFlashBackup(null)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        val actionLabel = if (showOpenDownloadsAction) strings.openDownloadsBtn else null
        val result = snackbarHostState.showSnackbar(
            message = msg,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            val downloadsIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(downloadsIntent) }
        }
        viewModel.clearSnackbarMessage()
    }

    val moreDestination by viewModel.moreDestination.collectAsStateWithLifecycle()
    val prefsLoaded by viewModel.prefsLoaded.collectAsStateWithLifecycle()
    val receiveState by viewModel.receiveState.collectAsStateWithLifecycle()
    val receiverStatus by viewModel.receiverStatus.collectAsStateWithLifecycle()
    val receivedItems by viewModel.receivedItems.collectAsStateWithLifecycle()
    val receiveEvent by viewModel.receiveEvent.collectAsStateWithLifecycle()
    val sendState by viewModel.sendState.collectAsStateWithLifecycle()
    val nearbyPeers by viewModel.nearbyPeers.collectAsStateWithLifecycle()
    val scanningPeers by viewModel.scanningPeers.collectAsStateWithLifecycle()
    val manualPeerResult by viewModel.manualPeerResult.collectAsStateWithLifecycle()
    // Look for devices when the Send tab opens with an empty list.
    LaunchedEffect(currentTab) {
        if (currentTab == MainNavTab.SEND && viewModel.nearbyPeers.value.isEmpty()) viewModel.refreshPeers()
    }
    val deviceName = receiverStatus.alias
    val appVersion = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    val pickExternalApk = {
        openExternalApkLauncher.launch(
            arrayOf(
                "application/vnd.android.package-archive",
                "application/octet-stream",
                "application/zip"
            )
        )
    }
    val shareViaLink = {
        val selected = viewModel.getSelectedAppsList()
        if (selected.isNotEmpty()) viewModel.startWifiShareForApps(selected) else viewModel.selectTab(MainNavTab.APPS)
    }

    // Back order lives in ui/navigation/NavState.kt (selection → More sub-page → Apps → exit).
    // Disabled on Apps with nothing selected, so the system keeps the activity (and running work) alive.
    val backSnapshot = NavSnapshot(currentTab, moreDestination, selectedPackageNames.isNotEmpty())
    BackHandler(enabled = canHandleBack(backSnapshot)) { viewModel.onBack() }

    ShareApkShell(
        strings = strings,
        darkTheme = isDarkTheme,
        isExpandedScreen = isExpandedScreen,
        currentTab = currentTab,
        onSelectTab = viewModel::selectTab,
        snackbarHostState = snackbarHostState,
        overlay = if (prefsLoaded && !prefs.onboardingCompleted) {
            { OnboardingPagerDialog(onFinishOnboarding = viewModel::completeOnboarding) }
        } else null
    ) {
        when (currentTab) {
            MainNavTab.APPS -> AppsScreen(
                apps = apps,
                isLoading = isLoading,
                searchQuery = searchQuery,
                selectedFilter = selectedFilter,
                selectedSort = selectedSort,
                isGridView = isGridView,
                isExpandedScreen = isExpandedScreen,
                selectedPackageNames = selectedPackageNames,
                focusedApp = focusedApp,
                computedSha256Map = computedSha256Map,
                isComputingSha256 = isComputingSha256,
                quickZipProgress = quickZipProgress,
                onSearchChange = viewModel::setSearchQuery,
                onFilterChange = viewModel::setFilter,
                onSortChange = viewModel::setSortOption,
                onToggleGridView = viewModel::toggleGridView,
                onRefreshApps = viewModel::refreshInstalledApps,
                onSelectAppForDetails = viewModel::selectAppForDetails,
                onToggleAppMultiSelect = viewModel::toggleAppSelection,
                onClearMultiSelect = viewModel::clearSelection,
                onShareSingleApp = { app ->
                    viewModel.requestShareApp(app) { chooser -> runCatching { context.startActivity(chooser) } }
                },
                onSaveSingleAppToDownloads = viewModel::requestSaveAppToDownloads,
                onReinstallVerifyApp = viewModel::inspectInstalledAppForReinstall,
                onWifiShareApps = viewModel::startWifiShareForApps,
                onWifiShareSelected = { viewModel.startWifiShareForApps(viewModel.getSelectedAppsList()) },
                onShareSelectedApps = {
                    viewModel.executeShareApps(viewModel.getSelectedAppsList()) { chooser ->
                        runCatching { context.startActivity(chooser) }
                    }
                },
                onBackupSelectedToZip = {
                    viewModel.startQuickMultiAppBackupToDownloads(viewModel.getSelectedAppsList())
                },
                onCancelQuickZipBackup = viewModel::cancelQuickMultiAppBackup,
                onToggleFavorite = viewModel::toggleFavorite,
                onComputeSha256 = viewModel::computeBaseApkSha256,
                onSendSelected = { viewModel.selectTab(MainNavTab.SEND) }
            )

            MainNavTab.SEND -> SendScreen(
                selectedApps = remember(selectedPackageNames, apps) { viewModel.getSelectedAppsList() },
                onPickApps = { viewModel.selectTab(MainNavTab.APPS) },
                onShareViaLink = shareViaLink,
                peers = nearbyPeers,
                scanning = scanningPeers,
                sendState = sendState,
                manualResult = manualPeerResult,
                onSendTo = viewModel::sendToPeer,
                onRefresh = viewModel::refreshPeers,
                onAddManual = viewModel::addManualPeer,
                onCancel = viewModel::cancelSend,
                onReset = viewModel::resetSend
            )

            MainNavTab.RECEIVE -> ReceiveScreen(
                status = receiverStatus,
                enabled = prefs.receiveEnabled,
                state = receiveState,
                received = receivedItems,
                onToggleEnabled = viewModel::setReceiveEnabled,
                onCancel = viewModel::cancelReceive,
                onAcknowledge = viewModel::acknowledgeReceive,
                onInstall = viewModel::installReceived,
                onDelete = viewModel::deleteReceived,
                lastEvent = receiveEvent
            )

            MainNavTab.MORE -> when (moreDestination) {
                MoreDestination.HOME -> MoreScreen(
                    deviceName = deviceName,
                    appVersion = appVersion,
                    onOpen = viewModel::openMore,
                    onInstallFromFile = pickExternalApk,
                    onShareViaLink = shareViaLink
                )

                MoreDestination.BACKUP -> Box(Modifier.testTag("more_BACKUP")) {
                    BackupScreen(
                        candidateApps = backupCandidates,
                        selectedPackageNames = backupSelectedPackages,
                        includeSystemApps = backupIncludeSystem,
                        searchQuery = backupSearchQuery,
                        destinationUriString = prefs.lastBackupTreeUri,
                        spaceCheckResult = spaceCheckResult,
                        backupServiceState = backupServiceState,
                        safetyCheck1 = safetyCheck1,
                        safetyCheck2 = safetyCheck2,
                        safetyCheck3 = safetyCheck3,
                        restoreManifest = restoreManifest,
                        restoreCandidates = restoreCandidates,
                        isBatchRestoring = isBatchRestoring,
                        batchRestoreCurrentIndex = batchRestoreCurrentIndex,
                        batchRestoreCurrentLabel = batchRestoreCurrentLabel,
                        batchRestoreSummary = batchRestoreSummary,
                        onToggleIncludeSystem = viewModel::setBackupIncludeSystemApps,
                        onSearchQueryChange = viewModel::setBackupSearchQuery,
                        onToggleAppSelection = viewModel::toggleBackupAppSelection,
                        onSelectAll = viewModel::selectAllBackupApps,
                        onSelectNone = viewModel::selectNoneBackupApps,
                        onPickDestinationFolder = {
                            pickBackupDestinationLauncher.launch(null)
                        },
                        onStartFullBackup = { resumeFolder ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && resumeFolder == null) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.startFullPreFlashBackup(resumeFolder)
                            }
                        },
                        onCancelFullBackup = viewModel::cancelFullPreFlashBackup,
                        onSafetyCheckChange = viewModel::setSafetyCheck,
                        onConfirmSafetyChecklist = viewModel::completePreFlashSafetyChecklist,
                        onPickRestoreFolder = {
                            pickRestoreFolderLauncher.launch(null)
                        },
                        onToggleRestoreCandidate = viewModel::toggleRestoreCandidate,
                        onStartSequentialRestore = viewModel::startSequentialBatchRestore,
                        onSkipCurrentRestoreApp = viewModel::skipCurrentBatchRestoreItem,
                        onCancelBatchRestore = viewModel::cancelBatchRestore,
                        onBack = { viewModel.openMore(MoreDestination.HOME) }
                    )
                }

                MoreDestination.HISTORY -> HistoryScreen(
                    historyItems = historyItems,
                    onDeleteItem = viewModel::deleteHistoryItem,
                    onClearAll = viewModel::clearAllHistory,
                    onBack = { viewModel.openMore(MoreDestination.HOME) },
                    modifier = Modifier.testTag("more_HISTORY")
                )

                MoreDestination.SETTINGS -> SettingsScreen(
                    prefs = prefs,
                    cacheSizeBytes = cacheSizeBytes,
                    onThemeChange = viewModel::setThemePreference,
                    onLanguageChange = viewModel::setLanguagePreference,
                    onNamingFormatChange = viewModel::setFileNamingFormat,
                    onClearCacheNow = viewModel::clearSharedCacheNow,
                    onAutoClearCacheChange = viewModel::setAutoClearCacheOnExit,
                    onBack = { viewModel.openMore(MoreDestination.HOME) },
                    deviceAlias = prefs.deviceAlias.orEmpty(),
                    deviceAliasPlaceholder = Build.MODEL.orEmpty(),
                    onDeviceAliasChange = viewModel::setDeviceAlias,
                    modifier = Modifier.testTag("more_SETTINGS")
                )

                MoreDestination.ABOUT -> AboutScreen(
                    appVersion = appVersion,
                    onBack = { viewModel.openMore(MoreDestination.HOME) },
                    modifier = Modifier.testTag("more_ABOUT")
                )
            }
        }

        // Dialogs & Sheets
        (receiveState as? ReceiveState.AwaitingDecision)?.let { pending ->
            IncomingRequestSheet(pending.request, viewModel::incomingStatus, viewModel::decideIncoming)
        }
                        pendingSplitAction?.let { (splitApp, _) ->
                            SplitApkWarningDialog(
                                appLabel = splitApp.label,
                                splitCount = splitApp.splitSourceDirs.size,
                                onConfirm = {
                                    viewModel.confirmSplitWarningAction { chooser ->
                                        runCatching { context.startActivity(chooser) }
                                    }
                                },
                                onDismiss = viewModel::dismissSplitWarning
                            )
                        }

                        preInstallValidation?.let { validation ->
                            PreInstallValidationSheet(
                                validation = validation,
                                isCommitting = isCommittingInstall,
                                onConfirmInstall = viewModel::commitPreValidatedInstall,
                                onDismiss = viewModel::dismissPreInstallSheet
                            )
                        }

                        if (showUnknownSourcesDialog) {
                            UnknownSourcesPermissionDialog(
                                onOpenSettings = {
                                    viewModel.dismissUnknownSourcesDialog()
                                    val settingsIntent = viewModel.packageInstallerManager.createUnknownSourcesSettingsIntent()
                                    runCatching { context.startActivity(settingsIntent) }
                                },
                                onDismiss = viewModel::dismissUnknownSourcesDialog
                            )
                        }

                        if (!isBatchRestoring && latestInstallResult != null) {
                            latestInstallResult?.let { result ->
                                InstallStatusResultDialog(
                                    result = result,
                                    onDismiss = viewModel::dismissInstallResultDialog
                                )
                            }
                        }

                        if (showWifiDialog && wifiServerState.isRunning) {
                            WifiShareServerDialog(
                                state = wifiServerState,
                                onStopServer = viewModel::stopWifiShareServer
                            )
                        }


    }
}
