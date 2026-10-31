package com.example

import android.Manifest
import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MainNavTab
import com.example.data.model.ThemePreference
import com.example.ui.components.InstallStatusResultDialog
import com.example.ui.components.OnboardingPagerDialog
import com.example.ui.components.PreInstallValidationSheet
import com.example.ui.components.SplitApkWarningDialog
import com.example.ui.components.UnknownSourcesPermissionDialog
import com.example.ui.components.WifiShareServerDialog
import com.example.ui.screens.AppsHomeScreen
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsAndAboutScreen
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocaleHelper
import com.example.ui.theme.ShareAPKTheme
import com.example.ui.viewmodel.ShareApkViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: ShareApkViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingPackageIntent(intent)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isExpandedScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

            ShareApkAppRoot(
                viewModel = viewModel,
                isExpandedScreen = isExpandedScreen
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingPackageIntent(intent)
    }

    private fun handleIncomingPackageIntent(incomingIntent: Intent?) {
        if (incomingIntent == null) return
        when (incomingIntent.action) {
            Intent.ACTION_VIEW -> {
                incomingIntent.data?.let { uri ->
                    viewModel.inspectAndPrepareInstall(uri)
                }
            }
            Intent.ACTION_SEND -> {
                val streamUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    incomingIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    incomingIntent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                streamUri?.let { uri ->
                    viewModel.inspectAndPrepareInstall(uri)
                }
            }
        }
    }
}

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
    val layoutDirection = if (strings.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
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

    // Predictive Back / BackHandler for sub-tabs and multi-select
    BackHandler(enabled = selectedPackageNames.isNotEmpty() || currentTab != MainNavTab.APPS) {
        when {
            selectedPackageNames.isNotEmpty() -> viewModel.clearSelection()
            currentTab != MainNavTab.APPS -> viewModel.selectTab(MainNavTab.APPS)
        }
    }

    CompositionLocalProvider(
        LocalAppStrings provides strings,
        LocalLayoutDirection provides layoutDirection
    ) {
        ShareAPKTheme(
            darkTheme = isDarkTheme
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!isExpandedScreen) {
                        ShareApkBottomNavigationBar(
                            currentTab = currentTab,
                            onSelectTab = { viewModel.selectTab(it) }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen) {
                        ShareApkNavigationRail(
                            currentTab = currentTab,
                            onSelectTab = { viewModel.selectTab(it) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        when (currentTab) {
                            MainNavTab.APPS -> AppsHomeScreen(
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
                                cacheSizeBytes = cacheSizeBytes,
                                quickZipProgress = quickZipProgress,
                                onSearchChange = viewModel::setSearchQuery,
                                onFilterChange = viewModel::setFilter,
                                onSortChange = viewModel::setSortOption,
                                onToggleGridView = viewModel::toggleGridView,
                                onRefreshApps = viewModel::refreshInstalledApps,
                                onPickExternalApkToInstall = {
                                    openExternalApkLauncher.launch(
                                        arrayOf(
                                            "application/vnd.android.package-archive",
                                            "application/octet-stream",
                                            "application/zip"
                                        )
                                    )
                                },
                                onSelectAppForDetails = viewModel::selectAppForDetails,
                                onToggleAppMultiSelect = viewModel::toggleAppSelection,
                                onClearMultiSelect = viewModel::clearSelection,
                                onShareSingleApp = { app ->
                                    viewModel.requestShareApp(app) { chooser ->
                                        runCatching { context.startActivity(chooser) }
                                    }
                                },
                                onSaveSingleAppToDownloads = viewModel::requestSaveAppToDownloads,
                                onReinstallVerifyApp = viewModel::inspectInstalledAppForReinstall,
                                onWifiShareApps = viewModel::startWifiShareForApps,
                                onShareSelectedApps = {
                                    val selectedList = viewModel.getSelectedAppsList()
                                    viewModel.executeShareApps(selectedList) { chooser ->
                                        runCatching { context.startActivity(chooser) }
                                    }
                                },
                                onBackupSelectedToZip = {
                                    val selectedList = viewModel.getSelectedAppsList()
                                    viewModel.startQuickMultiAppBackupToDownloads(selectedList)
                                },
                                onCancelQuickZipBackup = viewModel::cancelQuickMultiAppBackup,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onComputeSha256 = viewModel::computeBaseApkSha256
                            )

                            MainNavTab.BACKUP_RESTORE -> BackupRestoreScreen(
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
                                onCancelBatchRestore = viewModel::cancelBatchRestore
                            )

                            MainNavTab.HISTORY -> HistoryScreen(
                                historyItems = historyItems,
                                onDeleteItem = viewModel::deleteHistoryItem,
                                onClearAll = viewModel::clearAllHistory
                            )

                            // Replaced by the new root in Task 4.
                            MainNavTab.SEND, MainNavTab.RECEIVE, MainNavTab.MORE -> Unit

                            MainNavTab.SETTINGS -> SettingsAndAboutScreen(
                                prefs = prefs,
                                cacheSizeBytes = cacheSizeBytes,
                                onThemeChange = viewModel::setThemePreference,
                                onDynamicColorChange = viewModel::setDynamicColorEnabled,
                                onLanguageChange = viewModel::setLanguagePreference,
                                onNamingFormatChange = viewModel::setFileNamingFormat,
                                onClearCacheNow = viewModel::clearSharedCacheNow,
                                onAutoClearCacheChange = viewModel::setAutoClearCacheOnExit
                            )
                        }
                    }
                }

                // Dialogs & Sheets
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

                if (!prefs.onboardingCompleted) {
                    OnboardingPagerDialog(
                        onFinishOnboarding = viewModel::completeOnboarding
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareApkBottomNavigationBar(
    currentTab: MainNavTab,
    onSelectTab: (MainNavTab) -> Unit
) {
    val strings = LocalAppStrings.current
    val navItems = listOf(
        Triple(MainNavTab.APPS, strings.navApps, Icons.Filled.Apps to Icons.Outlined.Apps),
        Triple(MainNavTab.BACKUP_RESTORE, strings.navBackup, Icons.Filled.Backup to Icons.Outlined.Backup),
        Triple(MainNavTab.HISTORY, strings.navHistory, Icons.Filled.History to Icons.Outlined.History),
        Triple(MainNavTab.SETTINGS, strings.navSettings, Icons.Filled.Settings to Icons.Outlined.Settings)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.navigationBarsPadding()
    ) {
        navItems.forEach { (tab, label, icons) ->
            val selected = currentTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) icons.first else icons.second,
                        contentDescription = label
                    )
                },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.surface,
                    selectedTextColor = BurnishedGold,
                    indicatorColor = BurnishedGold
                ),
                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
            )
        }
    }
}

@Composable
private fun ShareApkNavigationRail(
    currentTab: MainNavTab,
    onSelectTab: (MainNavTab) -> Unit
) {
    val strings = LocalAppStrings.current
    val navItems = listOf(
        Triple(MainNavTab.APPS, strings.navApps, Icons.Filled.Apps to Icons.Outlined.Apps),
        Triple(MainNavTab.BACKUP_RESTORE, strings.navBackup, Icons.Filled.Backup to Icons.Outlined.Backup),
        Triple(MainNavTab.HISTORY, strings.navHistory, Icons.Filled.History to Icons.Outlined.History),
        Triple(MainNavTab.SETTINGS, strings.navSettings, Icons.Filled.Settings to Icons.Outlined.Settings)
    )

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxHeight()
    ) {
        navItems.forEach { (tab, label, icons) ->
            val selected = currentTab == tab
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) icons.first else icons.second,
                        contentDescription = label
                    )
                },
                label = { Text(label) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.surface,
                    selectedTextColor = BurnishedGold,
                    indicatorColor = BurnishedGold
                ),
                modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
            )
        }
    }
}
