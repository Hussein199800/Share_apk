package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backup.PreFlashBackupManager
import com.example.backup.SpaceCheckResult
import com.example.data.local.AppDatabase
import com.example.data.local.HistoryEntity
import com.example.data.local.HistoryRepository
import com.example.data.model.AppFilter
import com.example.data.model.AppSortOption
import com.example.data.model.BackupManifest
import com.example.data.model.BatchRestoreSummary
import com.example.data.model.FileNamingFormat
import com.example.data.model.InstalledAppInfo
import com.example.data.model.LanguagePreference
import com.example.data.model.MainNavTab
import com.example.receive.ReceiveController
import com.example.update.ReleaseInfo
import com.example.update.UpdateState
import com.example.update.Updater
import com.example.receive.discovery.NearbyDevice
import com.example.receive.protocol.ShareApkMetaDto
import com.example.send.OutgoingFile
import com.example.send.PickedItem
import com.example.send.SendState
import com.example.receive.ReceiveEvent
import com.example.receive.ReceiveState
import com.example.receive.ReceivedItem
import com.example.receive.ReceiverStatus
import com.example.receive.VersionStatus
import com.example.receive.compareWithInstalled
import com.example.receive.protocol.FileDto
import com.example.ui.navigation.BackAction
import com.example.ui.navigation.MoreDestination
import com.example.ui.navigation.NavSnapshot
import com.example.ui.navigation.resolveBack
import com.example.data.model.PreInstallValidationInfo
import com.example.data.model.RestoreAppStatus
import com.example.data.model.RestoreCandidateApp
import com.example.data.model.ThemePreference
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserPreferencesState
import com.example.data.repository.ApkRepository
import com.example.installer.InstallStatusEventBus
import com.example.installer.PackageInstallerManager
import com.example.service.BackupForegroundService
import com.example.service.BackupJobOrchestrator
import com.example.ui.theme.LocaleHelper
import com.example.util.FileNameAndHashUtils
import com.example.wifi.HostedWifiFile
import com.example.wifi.LocalWifiHttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class PendingSplitActionType {
    SHARE,
    SAVE_DOWNLOADS
}

/** Filter predicate for the apps list; `null` matches every app. */
fun matchesFilter(app: InstalledAppInfo, filter: AppFilter?): Boolean = when (filter) {
    null -> true
    AppFilter.USER_APPS -> !app.isSystemApp
    AppFilter.SYSTEM_APPS -> app.isSystemApp
    AppFilter.SPLIT_ONLY -> app.isSplitApk
    AppFilter.FAVORITES -> app.isFavorite
}

data class QuickZipBackupProgress(
    val isRunning: Boolean = false,
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val currentAppLabel: String = ""
)

class ShareApkViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val apkRepository = ApkRepository(context)
    val prefsRepository = UserPreferencesRepository(context)
    private val historyRepository = HistoryRepository(AppDatabase.getInstance(context).historyDao())
    val packageInstallerManager = PackageInstallerManager(context, apkRepository)

    // ---- Direct receive (LocalSend-compatible) ----
    private val receiver = ReceiveController.get(context)
    val receiveState: StateFlow<ReceiveState> = receiver.sessions.state
    val receiverStatus: StateFlow<ReceiverStatus> = receiver.status
    val receivedItems: StateFlow<List<ReceivedItem>> = receiver.received
    val receiveEvent: StateFlow<ReceiveEvent?> = receiver.sessions.lastEvent

    fun decideIncoming(accept: Boolean) = receiver.sessions.decide(accept)

    // ---- "From browser" page ----
    val webShare: StateFlow<com.example.web.WebShareState> = receiver.web.state
    fun startWebShare() {
        receiver.web.installedApps = { _allApps.value }
        receiver.web.pickedFiles = { _pickedFiles.value }
        receiver.web.start()
    }
    fun stopWebShare() = receiver.web.stop()

    // ---- In-app updates (GitHub releases) ----
    private val updater = Updater(context)
    val updateState: StateFlow<UpdateState> = updater.state
    fun checkForUpdates() { viewModelScope.launch { updater.check() } }
    fun downloadUpdate(release: ReleaseInfo) { viewModelScope.launch { updater.download(release) } }
    fun dismissUpdate() = updater.dismiss()
    fun installUpdate(ready: UpdateState.Ready) {
        inspectAndPrepareInstall(androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", ready.apk))
    }

    // ---- Direct send ----
    val sendState: StateFlow<SendState> = receiver.sender.state
    val nearbyPeers: StateFlow<List<NearbyDevice>> = receiver.peers.peers
    val scanningPeers: StateFlow<Boolean> = receiver.sender.scanning
    private val _manualPeerResult = MutableStateFlow<Boolean?>(null)
    /** null = idle, true = device found, false = nothing answered. */
    val manualPeerResult: StateFlow<Boolean?> = _manualPeerResult.asStateFlow()

    fun refreshPeers() = receiver.refreshPeers()

    /** Shows a one-off hint in the snackbar. */
    fun showHint(message: String) { _snackbarMessage.value = message }
    fun cancelSend() = receiver.sender.cancel()
    fun resetSend() = receiver.sender.reset()

    fun addManualPeer(address: String) {
        _manualPeerResult.value = null
        viewModelScope.launch { _manualPeerResult.value = receiver.addManualPeer(address) != null }
    }

    // ---- Device picker (sending from the Apps or Files tab) ----
    enum class SendTarget { APPS, FILES }
    private val _pickerTarget = MutableStateFlow<SendTarget?>(null)
    val pickerTarget: StateFlow<SendTarget?> = _pickerTarget.asStateFlow()

    fun openDevicePicker(target: SendTarget) {
        if (!receiver.sender.isBusy()) receiver.sender.reset()
        _pickerTarget.value = target
        if (receiver.peers.peers.value.isEmpty()) refreshPeers()
    }

    fun closeDevicePicker() { _pickerTarget.value = null }

    /** From an app's details: send just that app (it becomes the selection) to a nearby device. */
    fun sendAppToDevice(app: InstalledAppInfo) {
        clearSelection()
        toggleAppSelection(app.packageName)
        selectAppForDetails(null)
        openDevicePicker(SendTarget.APPS)
    }

    /** Sends the current selection (apps or picked files) to [peer]. */
    fun sendToPeer(peer: NearbyDevice) {
        when (_pickerTarget.value) {
            SendTarget.FILES -> {
                val items = _pickedFiles.value
                if (items.isEmpty()) return
                receiver.sender.send(peer) { items.map(::toOutgoing) }
            }
            else -> {
                val apps = getSelectedAppsList()
                if (apps.isEmpty()) return
                val format = userPrefs.value.fileNamingFormat
                receiver.sender.send(peer) {
                    apps.map { app ->
                        val file = apkRepository.prepareAppInCache(app, format)
                        OutgoingFile(file, file.name, ShareApkMetaDto(app.packageName, app.versionName, app.versionCode), app.label)
                    }
                }
            }
        }
    }

    // ---- Files tab ----
    private val _pickedFiles = MutableStateFlow<List<PickedItem>>(emptyList())
    val pickedFiles: StateFlow<List<PickedItem>> = _pickedFiles.asStateFlow()
    private var pickedSeq = 0L

    fun addPickedUris(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val added = uris.mapNotNull { uri -> describe(uri, null) }
            _pickedFiles.value = _pickedFiles.value + added
        }
    }

    /** Adds every file in a picked folder; names keep the folder path so the receiver recreates it. */
    fun addPickedFolder(tree: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val root = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, tree) ?: return@launch
            val found = mutableListOf<PickedItem>()
            fun walk(dir: androidx.documentfile.provider.DocumentFile, path: String) {
                dir.listFiles().forEach { f ->
                    val name = f.name ?: return@forEach
                    if (f.isDirectory) walk(f, "$path/$name")
                    else if (found.size < 2_000) found += PickedItem(++pickedSeq, "$path/$name", f.length(), f.type, f.uri)
                }
            }
            walk(root, root.name ?: "folder")
            _pickedFiles.value = _pickedFiles.value + found
        }
    }

    fun addPickedText(text: String) {
        if (text.isBlank()) return
        _pickedFiles.value = _pickedFiles.value + PickedItem(++pickedSeq, "", text.toByteArray().size.toLong(), "text/plain", text = text)
    }

    /** Adds the clipboard text; returns false when the clipboard has no text. */
    fun pastePickedText(): Boolean {
        val cm = context.getSystemService(android.content.ClipboardManager::class.java)
        val text = cm?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
        if (text.isNullOrBlank()) return false
        addPickedText(text)
        return true
    }

    fun removePicked(item: PickedItem) { _pickedFiles.value = _pickedFiles.value - item }
    fun clearPicked() { _pickedFiles.value = emptyList() }

    private fun describe(uri: Uri, nameOverride: String?): PickedItem? = runCatching {
        var name = nameOverride
        var size = -1L
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME, android.provider.OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                if (name == null) name = c.getString(0)
                if (!c.isNull(1)) size = c.getLong(1)
            }
        }
        PickedItem(++pickedSeq, name ?: uri.lastPathSegment ?: "file", size.coerceAtLeast(0), context.contentResolver.getType(uri), uri)
    }.getOrNull()

    private fun toOutgoing(item: PickedItem): OutgoingFile =
        if (item.text != null) OutgoingFile.text(item.text)
        else OutgoingFile(
            fileName = item.name,
            size = item.size,
            mimeType = item.mimeType ?: "application/octet-stream",
            open = { context.contentResolver.openInputStream(item.uri!!) ?: error("cannot open ${item.name}") }
        )

    // ---- Received non-app files and messages ----
    val receivedOther: StateFlow<List<com.example.receive.ReceivedEntry>> = receiver.receivedOther
    fun removeReceivedEntry(entry: com.example.receive.ReceivedEntry) = receiver.removeReceivedEntry(entry)
    fun cancelReceive() = receiver.sessions.cancelActive()
    fun acknowledgeReceive() = receiver.sessions.acknowledge()
    fun deleteReceived(item: ReceivedItem) = receiver.deleteReceived(item)

    fun setReceiveEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setReceiveEnabled(enabled) }
    }

    fun setDeviceAlias(alias: String) {
        viewModelScope.launch { prefsRepository.setDeviceAlias(alias) }
    }

    /** Opens the existing pre-install sheet for a received package. */
    fun installReceived(item: ReceivedItem) {
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", item.file)
        inspectAndPrepareInstall(uri)
    }

    /** Installed versionCode of a package, or null if not installed (for pre-accept comparison). */
    fun incomingStatus(file: FileDto): VersionStatus? {
        val meta = file.shareApk ?: return null
        val installed = runCatching { context.packageManager.getPackageInfo(meta.packageName, 0).longVersionCode }.getOrNull()
        return compareWithInstalled(meta.versionCode, installed)
    }
    private val preFlashBackupManager = PreFlashBackupManager(context)
    private val wifiHttpServer = LocalWifiHttpServer()

    val userPrefs: StateFlow<UserPreferencesState> = prefsRepository.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserPreferencesState()
    )

    /** True once DataStore has emitted, so first-run UI doesn't flash for returning users. */
    val prefsLoaded: StateFlow<Boolean> = prefsRepository.preferencesFlow.map { true }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val historyItems: StateFlow<List<HistoryEntity>> = historyRepository.allHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val wifiServerState = wifiHttpServer.state
    val backupServiceState = BackupJobOrchestrator.state
    val latestInstallResult = InstallStatusEventBus.latestResult

    private val _currentTab = MutableStateFlow(MainNavTab.APPS)
    val currentTab: StateFlow<MainNavTab> = _currentTab.asStateFlow()

    private val _allApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(true)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow<AppFilter?>(AppFilter.USER_APPS)
    /** `null` means "all apps". */
    val selectedFilter: StateFlow<AppFilter?> = _selectedFilter.asStateFlow()

    private val _selectedSort = MutableStateFlow(AppSortOption.NAME_ASC)
    val selectedSort: StateFlow<AppSortOption> = _selectedSort.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _selectedPackageNames = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackageNames: StateFlow<Set<String>> = _selectedPackageNames.asStateFlow()

    private val _focusedApp = MutableStateFlow<InstalledAppInfo?>(null)
    val focusedApp: StateFlow<InstalledAppInfo?> = _focusedApp.asStateFlow()

    private val _computedApkSha256Map = MutableStateFlow<Map<String, String>>(emptyMap())
    val computedApkSha256Map: StateFlow<Map<String, String>> = _computedApkSha256Map.asStateFlow()

    private val _isComputingSha256 = MutableStateFlow(false)
    val isComputingSha256: StateFlow<Boolean> = _isComputingSha256.asStateFlow()

    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _showOpenDownloadsAction = MutableStateFlow(false)
    val showOpenDownloadsAction: StateFlow<Boolean> = _showOpenDownloadsAction.asStateFlow()

    private val _pendingSplitAction = MutableStateFlow<Pair<InstalledAppInfo, PendingSplitActionType>?>(null)
    val pendingSplitAction: StateFlow<Pair<InstalledAppInfo, PendingSplitActionType>?> = _pendingSplitAction.asStateFlow()

    private val _quickZipProgress = MutableStateFlow(QuickZipBackupProgress())
    val quickZipProgress: StateFlow<QuickZipBackupProgress> = _quickZipProgress.asStateFlow()
    private var quickZipJob: Job? = null

    private val _showWifiDialog = MutableStateFlow(false)
    val showWifiDialog: StateFlow<Boolean> = _showWifiDialog.asStateFlow()

    // Built-in PackageInstaller state
    private val _preInstallValidation = MutableStateFlow<PreInstallValidationInfo?>(null)
    val preInstallValidation: StateFlow<PreInstallValidationInfo?> = _preInstallValidation.asStateFlow()

    private val _isInspectingPackage = MutableStateFlow(false)
    val isInspectingPackage: StateFlow<Boolean> = _isInspectingPackage.asStateFlow()

    private val _isCommittingInstall = MutableStateFlow(false)
    val isCommittingInstall: StateFlow<Boolean> = _isCommittingInstall.asStateFlow()

    private val _showUnknownSourcesDialog = MutableStateFlow(false)
    val showUnknownSourcesDialog: StateFlow<Boolean> = _showUnknownSourcesDialog.asStateFlow()

    // Pre-Flash Backup & Restore state (Flow A & Flow B)
    private val _backupIncludeSystemApps = MutableStateFlow(false)
    val backupIncludeSystemApps: StateFlow<Boolean> = _backupIncludeSystemApps.asStateFlow()

    private val _backupSearchQuery = MutableStateFlow("")
    val backupSearchQuery: StateFlow<String> = _backupSearchQuery.asStateFlow()

    private val _backupSelectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val backupSelectedPackages: StateFlow<Set<String>> = _backupSelectedPackages.asStateFlow()

    private val _spaceCheckResult = MutableStateFlow<SpaceCheckResult?>(null)
    val spaceCheckResult: StateFlow<SpaceCheckResult?> = _spaceCheckResult.asStateFlow()

    // Mandatory 3 Pre-Flash Safety Checkboxes
    private val _safetyCheck1 = MutableStateFlow(false)
    val safetyCheck1: StateFlow<Boolean> = _safetyCheck1.asStateFlow()
    private val _safetyCheck2 = MutableStateFlow(false)
    val safetyCheck2: StateFlow<Boolean> = _safetyCheck2.asStateFlow()
    private val _safetyCheck3 = MutableStateFlow(false)
    val safetyCheck3: StateFlow<Boolean> = _safetyCheck3.asStateFlow()

    // Flow B: Restore from folder
    private var activeRestoreBackupDir: DocumentFile? = null
    private val _restoreManifest = MutableStateFlow<BackupManifest?>(null)
    val restoreManifest: StateFlow<BackupManifest?> = _restoreManifest.asStateFlow()

    private val _restoreCandidates = MutableStateFlow<List<RestoreCandidateApp>>(emptyList())
    val restoreCandidates: StateFlow<List<RestoreCandidateApp>> = _restoreCandidates.asStateFlow()

    private val _isBatchRestoring = MutableStateFlow(false)
    val isBatchRestoring: StateFlow<Boolean> = _isBatchRestoring.asStateFlow()

    private val _batchRestoreCurrentIndex = MutableStateFlow(0)
    val batchRestoreCurrentIndex: StateFlow<Int> = _batchRestoreCurrentIndex.asStateFlow()

    private val _batchRestoreCurrentLabel = MutableStateFlow("")
    val batchRestoreCurrentLabel: StateFlow<String> = _batchRestoreCurrentLabel.asStateFlow()

    private val _batchRestoreSummary = MutableStateFlow<BatchRestoreSummary?>(null)
    val batchRestoreSummary: StateFlow<BatchRestoreSummary?> = _batchRestoreSummary.asStateFlow()
    private var batchRestoreQueue: List<RestoreCandidateApp> = emptyList()
    private var batchInstalledCount = 0
    private var batchSkippedCount = 0
    private var batchFailedCount = 0
    private val batchDetailsLog = mutableListOf<String>()

    val filteredAndSortedApps: StateFlow<List<InstalledAppInfo>> = combine(
        _allApps,
        _searchQuery,
        _selectedFilter,
        _selectedSort,
        userPrefs
    ) { apps, query, filter, sort, prefs ->
        val favSet = prefs.favoritePackages
        val withFavs = apps.map { it.copy(isFavorite = favSet.contains(it.packageName)) }

        val filtered = withFavs.filter { app ->
            val matchesFilter = matchesFilter(app, filter)
            val matchesSearch = if (query.isBlank()) {
                true
            } else {
                app.label.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }

        when (sort) {
            AppSortOption.NAME_ASC -> filtered.sortedBy { it.label.lowercase() }
            AppSortOption.SIZE_DESC -> filtered.sortedByDescending { it.totalSizeBytes }
            AppSortOption.INSTALLED_DATE -> filtered.sortedByDescending { it.firstInstallTime }
            AppSortOption.RECENTLY_UPDATED -> filtered.sortedByDescending { it.lastUpdateTime }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val backupCandidateApps: StateFlow<List<InstalledAppInfo>> = combine(
        _allApps,
        _backupIncludeSystemApps,
        _backupSearchQuery
    ) { apps, includeSystem, query ->
        apps.filter { app ->
            val sysMatch = if (includeSystem) true else !app.isSystemApp
            val qMatch = query.isBlank() ||
                app.label.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
            sysMatch && qMatch
        }.sortedBy { it.label.lowercase() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch { updater.autoCheck() }
        // The receiver lives app-wide; keep it in sync with the user's settings.
        viewModelScope.launch {
            // The repository flow (not userPrefs) so the receiver never sees the pre-load defaults.
            prefsRepository.preferencesFlow.collect { p ->
                receiver.setAlias(p.deviceAlias)
                receiver.setEnabled(p.receiveEnabled)
            }
        }
        refreshInstalledApps()
        refreshCacheSize()

        // Observe install status results for logging and sequential batch restore continuation
        viewModelScope.launch {
            InstallStatusEventBus.latestResult.collect { result ->
                if (result != null) {
                    _isCommittingInstall.value = false
                    if (_isBatchRestoring.value) {
                        onBatchRestorePackageCompleted(result.status == android.content.pm.PackageInstaller.STATUS_SUCCESS, result.messageAr)
                    } else if (result.status == android.content.pm.PackageInstaller.STATUS_SUCCESS) {
                        refreshInstalledApps()
                    }
                }
            }
        }
    }

    private val _moreDestination = MutableStateFlow(MoreDestination.HOME)
    val moreDestination: StateFlow<MoreDestination> = _moreDestination.asStateFlow()

    fun selectTab(tab: MainNavTab) {
        _currentTab.value = tab
    }

    /** Opens a More sub-page (switching to the More tab). */
    fun openMore(dest: MoreDestination) {
        _currentTab.value = MainNavTab.MORE
        _moreDestination.value = dest
        if (dest == MoreDestination.SETTINGS) {
            refreshCacheSize()
        }
    }

    /** Applies the back policy. Returns false when the app should close. */
    fun onBack(): Boolean {
        val snapshot = NavSnapshot(_currentTab.value, _moreDestination.value, _selectedPackageNames.value.isNotEmpty())
        return when (resolveBack(snapshot)) {
            BackAction.ClearSelection -> { clearSelection(); true }
            BackAction.MoreHome -> { _moreDestination.value = MoreDestination.HOME; true }
            BackAction.GoApps -> { _currentTab.value = MainNavTab.APPS; true }
            BackAction.Exit -> false
        }
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val favs = userPrefs.value.favoritePackages
            val loaded = runCatching { apkRepository.loadInstalledApps(favs) }.getOrDefault(emptyList())
            _allApps.value = loaded
            // Default select all USER apps for Pre-Flash Backup if not initialized yet
            if (_backupSelectedPackages.value.isEmpty()) {
                _backupSelectedPackages.value = loaded.filter { !it.isSystemApp }.map { it.packageName }.toSet()
            }
            // Refresh focused app if present
            _focusedApp.value?.let { current ->
                _focusedApp.value = loaded.find { it.packageName == current.packageName }
            }
            _isLoadingApps.value = false
            updateFreeSpaceCheck()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: AppFilter?) {
        _selectedFilter.value = filter
    }

    fun setSortOption(sort: AppSortOption) {
        _selectedSort.value = sort
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    fun selectAppForDetails(app: InstalledAppInfo?) {
        _focusedApp.value = app
        if (app != null && !_computedApkSha256Map.value.containsKey(app.packageName)) {
            computeBaseApkSha256(app)
        }
    }

    fun computeBaseApkSha256(app: InstalledAppInfo) {
        viewModelScope.launch {
            _isComputingSha256.value = true
            val hash = withContext(Dispatchers.IO) {
                FileNameAndHashUtils.computeFileSha256(File(app.baseApkPath))
            }
            _computedApkSha256Map.value = _computedApkSha256Map.value + (app.packageName to hash)
            _isComputingSha256.value = false
        }
    }

    fun toggleFavorite(packageName: String) {
        viewModelScope.launch {
            prefsRepository.toggleFavorite(packageName)
            _focusedApp.value?.let {
                if (it.packageName == packageName) {
                    _focusedApp.value = it.copy(isFavorite = !it.isFavorite)
                }
            }
        }
    }

    // Multi-Select Mode
    fun toggleAppSelection(packageName: String) {
        val current = _selectedPackageNames.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedPackageNames.value = current
    }

    fun clearSelection() {
        _selectedPackageNames.value = emptySet()
    }

    fun getSelectedAppsList(): List<InstalledAppInfo> {
        val selectedIds = _selectedPackageNames.value
        return _allApps.value.filter { selectedIds.contains(it.packageName) }
    }

    // Share & Save Actions (with Split APK warning check)
    fun requestShareApp(app: InstalledAppInfo, onLaunchIntent: (Intent) -> Unit) {
        if (app.isSplitApk) {
            _pendingSplitAction.value = app to PendingSplitActionType.SHARE
        } else {
            executeShareApps(listOf(app), onLaunchIntent)
        }
    }

    fun requestSaveAppToDownloads(app: InstalledAppInfo) {
        if (app.isSplitApk) {
            _pendingSplitAction.value = app to PendingSplitActionType.SAVE_DOWNLOADS
        } else {
            executeSaveToDownloads(app)
        }
    }

    fun dismissSplitWarning() {
        _pendingSplitAction.value = null
    }

    fun confirmSplitWarningAction(onLaunchIntent: (Intent) -> Unit) {
        val pending = _pendingSplitAction.value ?: return
        _pendingSplitAction.value = null
        when (pending.second) {
            PendingSplitActionType.SHARE -> executeShareApps(listOf(pending.first), onLaunchIntent)
            PendingSplitActionType.SAVE_DOWNLOADS -> executeSaveToDownloads(pending.first)
        }
    }

    fun executeShareApps(apps: List<InstalledAppInfo>, onLaunchIntent: (Intent) -> Unit) {
        if (apps.isEmpty()) return
        viewModelScope.launch {
            val format = userPrefs.value.fileNamingFormat
            val preparedFiles = apps.map { app ->
                val file = apkRepository.prepareAppInCache(app, format)
                val sha = withContext(Dispatchers.IO) { FileNameAndHashUtils.computeFileSha256(file) }
                historyRepository.logOperation(
                    actionType = "SHARE",
                    appLabel = app.label,
                    packageName = app.packageName,
                    versionName = app.versionName,
                    fileFormat = ".${app.fileExtension}",
                    fileSizeBytes = file.length(),
                    sha256Hash = sha
                )
                file
            }
            refreshCacheSize()
            val chooser = apkRepository.createShareChooserIntent(
                files = preparedFiles,
                chooserTitle = if (apps.size == 1) apps.first().label else "Share ${apps.size} APKs"
            )
            onLaunchIntent(chooser)
        }
    }

    private fun executeSaveToDownloads(app: InstalledAppInfo) {
        viewModelScope.launch {
            runCatching {
                val format = userPrefs.value.fileNamingFormat
                val (_, sha256) = apkRepository.saveAppToDownloads(app, format)
                historyRepository.logOperation(
                    actionType = "SAVE_DOWNLOADS",
                    appLabel = app.label,
                    packageName = app.packageName,
                    versionName = app.versionName,
                    fileFormat = ".${app.fileExtension}",
                    fileSizeBytes = app.totalSizeBytes,
                    sha256Hash = sha256
                )
                refreshCacheSize()
                _showOpenDownloadsAction.value = true
                _snackbarMessage.value = if (userPrefs.value.languagePreference == LanguagePreference.ENGLISH) {
                    "Saved ${app.label} (.${app.fileExtension}) to Downloads/ShareAPK"
                } else {
                    "تم حفظ ${app.label} (.${app.fileExtension}) في التنزيلات/ShareAPK"
                }
            }.onFailure { e ->
                _showOpenDownloadsAction.value = false
                _snackbarMessage.value = "Error: ${e.localizedMessage}"
            }
        }
    }

    // Multi-App Quick Backup (.zip) to Downloads
    fun startQuickMultiAppBackupToDownloads(apps: List<InstalledAppInfo>) {
        if (apps.isEmpty()) return
        quickZipJob?.cancel()
        quickZipJob = viewModelScope.launch {
            runCatching {
                _quickZipProgress.value = QuickZipBackupProgress(
                    isRunning = true,
                    currentIndex = 0,
                    totalCount = apps.size,
                    currentAppLabel = apps.first().label
                )
                val (zipName, sha256) = apkRepository.createQuickMultiAppZipInDownloads(apps) { idx, total, label ->
                    _quickZipProgress.value = QuickZipBackupProgress(
                        isRunning = true,
                        currentIndex = idx,
                        totalCount = total,
                        currentAppLabel = label
                    )
                }
                val totalBytes = apps.sumOf { it.totalSizeBytes }
                historyRepository.logOperation(
                    actionType = "BACKUP",
                    appLabel = "$zipName (${apps.size} apps)",
                    packageName = "multi.backup.zip",
                    versionName = "${apps.size} apps",
                    fileFormat = ".zip",
                    fileSizeBytes = totalBytes,
                    sha256Hash = sha256
                )
                _quickZipProgress.value = QuickZipBackupProgress(isRunning = false)
                clearSelection()
                _showOpenDownloadsAction.value = true
                _snackbarMessage.value = if (userPrefs.value.languagePreference == LanguagePreference.ENGLISH) {
                    "Created $zipName in Downloads/ShareAPK"
                } else {
                    "تم إنشاء الأرشيف $zipName في التنزيلات/ShareAPK"
                }
            }.onFailure {
                _quickZipProgress.value = QuickZipBackupProgress(isRunning = false)
            }
        }
    }

    fun cancelQuickMultiAppBackup() {
        quickZipJob?.cancel()
        _quickZipProgress.value = QuickZipBackupProgress(isRunning = false)
    }

    // Local Wi-Fi HTTP Sharing
    fun startWifiShareForApps(apps: List<InstalledAppInfo>) {
        if (apps.isEmpty()) return
        viewModelScope.launch {
            _showWifiDialog.value = true
            val format = userPrefs.value.fileNamingFormat
            val hosted = apps.mapIndexed { idx, app ->
                val prepared = apkRepository.prepareAppInCache(app, format)
                val sha = withContext(Dispatchers.IO) { FileNameAndHashUtils.computeFileSha256(prepared) }
                historyRepository.logOperation(
                    actionType = "WIFI_SHARE",
                    appLabel = app.label,
                    packageName = app.packageName,
                    versionName = app.versionName,
                    fileFormat = ".${app.fileExtension}",
                    fileSizeBytes = prepared.length(),
                    sha256Hash = sha
                )
                HostedWifiFile(
                    index = idx,
                    appLabel = app.label,
                    packageName = app.packageName,
                    versionName = app.versionName,
                    file = prepared,
                    sizeFormatted = FileNameAndHashUtils.formatBytes(prepared.length()),
                    sha256Short = sha.take(16)
                )
            }
            refreshCacheSize()
            wifiHttpServer.startServer(hosted)
        }
    }

    fun stopWifiShareServer() {
        wifiHttpServer.stopServer()
        _showWifiDialog.value = false
    }

    // Built-in PackageInstaller Flow
    fun inspectAndPrepareInstall(uri: Uri) {
        viewModelScope.launch {
            _isInspectingPackage.value = true
            val info = packageInstallerManager.inspectExternalPackage(uri)
            _preInstallValidation.value = info
            _isInspectingPackage.value = false
        }
    }

    fun inspectInstalledAppForReinstall(app: InstalledAppInfo) {
        viewModelScope.launch {
            _isInspectingPackage.value = true
            val format = userPrefs.value.fileNamingFormat
            val preparedFile = apkRepository.prepareAppInCache(app, format)
            val uri = Uri.fromFile(preparedFile)
            val info = packageInstallerManager.inspectExternalPackage(uri)
            _preInstallValidation.value = info
            _isInspectingPackage.value = false
        }
    }

    fun dismissPreInstallSheet() {
        _preInstallValidation.value = null
    }

    fun dismissInstallResultDialog() {
        InstallStatusEventBus.clearResult()
    }

    fun dismissUnknownSourcesDialog() {
        _showUnknownSourcesDialog.value = false
    }

    fun commitPreValidatedInstall() {
        val validation = _preInstallValidation.value ?: return
        if (!validation.isValid) return

        if (!packageInstallerManager.canRequestPackageInstalls()) {
            _showUnknownSourcesDialog.value = true
            return
        }

        viewModelScope.launch {
            _isCommittingInstall.value = true
            runCatching {
                packageInstallerManager.installFromValidatedUri(validation)
                historyRepository.logOperation(
                    actionType = "INSTALL",
                    appLabel = validation.appLabel,
                    packageName = validation.packageName,
                    versionName = validation.versionName,
                    fileFormat = if (validation.isApksBundle) ".apks" else ".apk",
                    fileSizeBytes = validation.fileSizeBytes,
                    sha256Hash = validation.sha256Hash
                )
                _preInstallValidation.value = null
            }.onFailure { e ->
                _isCommittingInstall.value = false
                _snackbarMessage.value = "Install Error: ${e.localizedMessage}"
            }
        }
    }

    // Flow A: Pre-Flash Backup Controls
    fun setBackupIncludeSystemApps(include: Boolean) {
        _backupIncludeSystemApps.value = include
        val candidates = _allApps.value.filter { if (include) true else !it.isSystemApp }
        _backupSelectedPackages.value = candidates.map { it.packageName }.toSet()
        updateFreeSpaceCheck()
    }

    fun setBackupSearchQuery(query: String) {
        _backupSearchQuery.value = query
    }

    fun toggleBackupAppSelection(packageName: String) {
        val current = _backupSelectedPackages.value.toMutableSet()
        if (current.contains(packageName)) current.remove(packageName) else current.add(packageName)
        _backupSelectedPackages.value = current
        updateFreeSpaceCheck()
    }

    fun selectAllBackupApps() {
        _backupSelectedPackages.value = backupCandidateApps.value.map { it.packageName }.toSet()
        updateFreeSpaceCheck()
    }

    fun selectNoneBackupApps() {
        _backupSelectedPackages.value = emptySet()
        updateFreeSpaceCheck()
    }

    fun onBackupDestinationPicked(treeUri: Uri) {
        viewModelScope.launch {
            prefsRepository.setLastBackupTreeUri(treeUri.toString())
            updateFreeSpaceCheck(treeUri)
        }
    }

    private fun updateFreeSpaceCheck(overrideUri: Uri? = null) {
        val uriStr = overrideUri?.toString() ?: userPrefs.value.lastBackupTreeUri
        val uri = uriStr?.let { runCatching { Uri.parse(it) }.getOrNull() }
        val selectedSet = _backupSelectedPackages.value
        val requiredBytes = _allApps.value.filter { selectedSet.contains(it.packageName) }
            .sumOf { it.totalSizeBytes }
        _spaceCheckResult.value = preFlashBackupManager.checkDestinationFreeSpace(uri, requiredBytes)
    }

    fun startFullPreFlashBackup(resumeFolderName: String? = null) {
        val uriStr = userPrefs.value.lastBackupTreeUri ?: return
        val treeUri = Uri.parse(uriStr)
        val selectedIds = _backupSelectedPackages.value
        val appsToBackup = _allApps.value.filter { selectedIds.contains(it.packageName) }
        if (appsToBackup.isEmpty()) return

        _safetyCheck1.value = false
        _safetyCheck2.value = false
        _safetyCheck3.value = false

        BackupForegroundService.startBackupService(
            context = context,
            treeUri = treeUri,
            apps = appsToBackup,
            resumeFolderName = resumeFolderName
        )

        viewModelScope.launch {
            historyRepository.logOperation(
                actionType = "BACKUP",
                appLabel = "Pre-Flash Backup (${appsToBackup.size} Apps)",
                packageName = "com.shareapk.preflash",
                versionName = "${appsToBackup.size} packages",
                fileFormat = "SAF Folder",
                fileSizeBytes = appsToBackup.sumOf { it.totalSizeBytes },
                sha256Hash = "MANIFEST_SHA256_VERIFIED"
            )
        }
    }

    fun cancelFullPreFlashBackup() {
        BackupForegroundService.cancelBackupService(context)
    }

    fun setSafetyCheck(index: Int, checked: Boolean) {
        when (index) {
            1 -> _safetyCheck1.value = checked
            2 -> _safetyCheck2.value = checked
            3 -> _safetyCheck3.value = checked
        }
    }

    fun completePreFlashSafetyChecklist() {
        if (_safetyCheck1.value && _safetyCheck2.value && _safetyCheck3.value) {
            BackupJobOrchestrator.updateState {
                it.copy(
                    awaitingSafetyChecklist = false,
                    isFullyCompleted = true
                )
            }
        }
    }

    // Flow B: Restore from ShareAPK_Backup_* Folder
    fun loadBackupFolderForRestore(treeUri: Uri) {
        viewModelScope.launch {
            runCatching {
                val (backupDir, manifest, candidates) = preFlashBackupManager.parseBackupFolderForRestore(treeUri)
                activeRestoreBackupDir = backupDir
                _restoreManifest.value = manifest
                _restoreCandidates.value = candidates
                _batchRestoreSummary.value = null
            }.onFailure { e ->
                _snackbarMessage.value = "Restore Error: ${e.localizedMessage}"
            }
        }
    }

    fun toggleRestoreCandidate(packageName: String) {
        _restoreCandidates.value = _restoreCandidates.value.map { item ->
            if (item.entry.packageName == packageName && item.status != RestoreAppStatus.INCOMPATIBLE) {
                item.copy(isSelected = !item.isSelected)
            } else {
                item
            }
        }
    }

    fun startSequentialBatchRestore() {
        val backupDir = activeRestoreBackupDir ?: return
        if (!packageInstallerManager.canRequestPackageInstalls()) {
            _showUnknownSourcesDialog.value = true
            return
        }

        val selected = _restoreCandidates.value.filter {
            it.isSelected && it.status != RestoreAppStatus.INCOMPATIBLE && it.integrityOk
        }
        if (selected.isEmpty()) return

        batchRestoreQueue = selected
        batchInstalledCount = 0
        batchSkippedCount = 0
        batchFailedCount = 0
        batchDetailsLog.clear()
        _batchRestoreSummary.value = null
        _isBatchRestoring.value = true
        _batchRestoreCurrentIndex.value = 0

        installNextInBatchRestoreQueue(backupDir)
    }

    private fun installNextInBatchRestoreQueue(backupDir: DocumentFile) {
        val idx = _batchRestoreCurrentIndex.value
        if (idx >= batchRestoreQueue.size) {
            _isBatchRestoring.value = false
            _batchRestoreSummary.value = BatchRestoreSummary(
                installedCount = batchInstalledCount,
                skippedCount = batchSkippedCount,
                failedCount = batchFailedCount,
                details = batchDetailsLog.toList()
            )
            refreshInstalledApps()
            return
        }

        val currentCandidate = batchRestoreQueue[idx]
        _batchRestoreCurrentLabel.value = currentCandidate.entry.label

        viewModelScope.launch {
            runCatching {
                val safeFolder = FileNameAndHashUtils.sanitizeFileName(currentCandidate.entry.packageName, 90)
                val appFolder = backupDir.findFile(safeFolder)
                    ?: throw IllegalStateException("Folder missing for ${currentCandidate.entry.packageName}")
                packageInstallerManager.installFromBackupAppFolder(
                    appFolder = appFolder,
                    packageName = currentCandidate.entry.packageName
                )
            }.onFailure { e ->
                batchFailedCount++
                batchDetailsLog.add("✗ ${currentCandidate.entry.label}: ${e.localizedMessage}")
                _batchRestoreCurrentIndex.value = idx + 1
                installNextInBatchRestoreQueue(backupDir)
            }
        }
    }

    private fun onBatchRestorePackageCompleted(success: Boolean, statusMessage: String) {
        val backupDir = activeRestoreBackupDir ?: return
        val idx = _batchRestoreCurrentIndex.value
        val candidate = batchRestoreQueue.getOrNull(idx)
        if (candidate != null) {
            if (success) {
                batchInstalledCount++
                batchDetailsLog.add("✓ ${candidate.entry.label} (v${candidate.entry.versionName})")
            } else {
                batchFailedCount++
                batchDetailsLog.add("✗ ${candidate.entry.label}: $statusMessage")
            }
        }
        _batchRestoreCurrentIndex.value = idx + 1
        installNextInBatchRestoreQueue(backupDir)
    }

    fun skipCurrentBatchRestoreItem() {
        val backupDir = activeRestoreBackupDir ?: return
        val idx = _batchRestoreCurrentIndex.value
        val candidate = batchRestoreQueue.getOrNull(idx)
        if (candidate != null) {
            batchSkippedCount++
            batchDetailsLog.add("⏭ ${candidate.entry.label} (Skipped)")
        }
        _batchRestoreCurrentIndex.value = idx + 1
        installNextInBatchRestoreQueue(backupDir)
    }

    fun cancelBatchRestore() {
        _isBatchRestoring.value = false
        _batchRestoreSummary.value = BatchRestoreSummary(
            installedCount = batchInstalledCount,
            skippedCount = batchSkippedCount + (batchRestoreQueue.size - _batchRestoreCurrentIndex.value).coerceAtLeast(0),
            failedCount = batchFailedCount,
            details = batchDetailsLog.toList()
        )
    }

    // History & Settings
    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch { historyRepository.deleteById(id) }
    }

    fun clearAllHistory() {
        viewModelScope.launch { historyRepository.clearAll() }
    }

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch { prefsRepository.setThemePreference(theme) }
    }


    fun setLanguagePreference(language: LanguagePreference) {
        viewModelScope.launch {
            prefsRepository.setLanguagePreference(language)
            LocaleHelper.applyLanguage(context, language)
        }
    }

    fun setFileNamingFormat(format: FileNamingFormat) {
        viewModelScope.launch { prefsRepository.setFileNamingFormat(format) }
    }

    fun setAutoClearCacheOnExit(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setAutoClearCacheOnExit(enabled) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { prefsRepository.setOnboardingCompleted(true) }
    }

    fun refreshCacheSize() {
        _cacheSizeBytes.value = apkRepository.getSharedCacheSizeBytes()
    }

    fun clearSharedCacheNow() {
        viewModelScope.launch {
            val freed = apkRepository.clearSharedCache()
            refreshCacheSize()
            _showOpenDownloadsAction.value = false
            _snackbarMessage.value = "Cleared ${FileNameAndHashUtils.formatBytes(freed)} from cacheDir/shared"
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
        _showOpenDownloadsAction.value = false
    }

    override fun onCleared() {
        super.onCleared()
        wifiHttpServer.stopServer()
        viewModelScope.launch {
            if (prefsRepository.preferencesFlow.first().autoClearCacheOnExit) {
                apkRepository.clearSharedCache()
            }
        }
    }
}
