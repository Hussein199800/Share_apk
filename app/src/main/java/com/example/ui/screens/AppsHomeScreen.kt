package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.data.model.AppFilter
import com.example.data.model.AppSortOption
import com.example.data.model.InstalledAppInfo
import com.example.ui.components.AppDetailPanel
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.ui.viewmodel.QuickZipBackupProgress
import com.example.util.FileNameAndHashUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsHomeScreen(
    apps: List<InstalledAppInfo>,
    isLoading: Boolean,
    searchQuery: String,
    selectedFilter: AppFilter,
    selectedSort: AppSortOption,
    isGridView: Boolean,
    isExpandedScreen: Boolean,
    selectedPackageNames: Set<String>,
    focusedApp: InstalledAppInfo?,
    computedSha256Map: Map<String, String>,
    isComputingSha256: Boolean,
    cacheSizeBytes: Long,
    quickZipProgress: QuickZipBackupProgress,
    onSearchChange: (String) -> Unit,
    onFilterChange: (AppFilter) -> Unit,
    onSortChange: (AppSortOption) -> Unit,
    onToggleGridView: () -> Unit,
    onRefreshApps: () -> Unit,
    onPickExternalApkToInstall: () -> Unit,
    onSelectAppForDetails: (InstalledAppInfo?) -> Unit,
    onToggleAppMultiSelect: (String) -> Unit,
    onClearMultiSelect: () -> Unit,
    onShareSingleApp: (InstalledAppInfo) -> Unit,
    onSaveSingleAppToDownloads: (InstalledAppInfo) -> Unit,
    onReinstallVerifyApp: (InstalledAppInfo) -> Unit,
    onWifiShareApps: (List<InstalledAppInfo>) -> Unit,
    onShareSelectedApps: () -> Unit,
    onBackupSelectedToZip: () -> Unit,
    onCancelQuickZipBackup: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onComputeSha256: (InstalledAppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val selectedApps = remember(apps, selectedPackageNames) {
        apps.filter { selectedPackageNames.contains(it.packageName) }
    }
    val selectedTotalBytes = remember(selectedApps) {
        selectedApps.sumOf { it.totalSizeBytes }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header Bar with Install External APK/APKS Button, Grid Toggle, and Cache Status Badge
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.appName,
                                style = MaterialTheme.typography.titleLarge,
                                color = BurnishedGold,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${apps.size} ${strings.navApps}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Storage,
                                            contentDescription = null,
                                            tint = BurnishedGold,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        LtrBox {
                                            Text(
                                                text = "Cache: ${FileNameAndHashUtils.formatBytes(cacheSizeBytes)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Prominent "Install APK / APKS" Button
                            Button(
                                onClick = onPickExternalApkToInstall,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BurnishedGold,
                                    contentColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("install_external_apk_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InstallMobile,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.installExternalBtn,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = onToggleGridView,
                                modifier = Modifier.testTag("toggle_grid_list_button")
                            ) {
                                Icon(
                                    imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                    contentDescription = "Toggle View",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = onRefreshApps,
                                modifier = Modifier.testTag("refresh_apps_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = strings.refreshApps,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text(strings.searchHint) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = BurnishedGold)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = strings.cancelBtn)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BurnishedGold,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_apps_input")
                    )

                    // Filter Chips & Sort Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf(
                            AppFilter.USER_APPS to strings.filterUserApps,
                            AppFilter.SYSTEM_APPS to strings.filterSystemApps,
                            AppFilter.SPLIT_ONLY to strings.filterSplitOnly,
                            AppFilter.FAVORITES to strings.filterFavorites
                        )

                        filters.forEach { (filter, label) ->
                            val selected = selectedFilter == filter
                            FilterChip(
                                selected = selected,
                                onClick = { onFilterChange(filter) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BurnishedGold,
                                    selectedLabelColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                            )
                        }

                        Box {
                            FilledTonalButton(
                                onClick = { sortMenuExpanded = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("sort_dropdown_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (selectedSort) {
                                        AppSortOption.NAME_ASC -> strings.sortName
                                        AppSortOption.SIZE_DESC -> strings.sortSize
                                        AppSortOption.INSTALLED_DATE -> strings.sortInstalled
                                        AppSortOption.RECENTLY_UPDATED -> strings.sortUpdated
                                    },
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(strings.sortName) },
                                    onClick = {
                                        onSortChange(AppSortOption.NAME_ASC)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.sortSize) },
                                    onClick = {
                                        onSortChange(AppSortOption.SIZE_DESC)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.sortInstalled) },
                                    onClick = {
                                        onSortChange(AppSortOption.INSTALLED_DATE)
                                        sortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.sortUpdated) },
                                    onClick = {
                                        onSortChange(AppSortOption.RECENTLY_UPDATED)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Main Content Area: Responsive Master-Detail on Medium/Expanded vs List/Grid + BottomSheet on Compact
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = onRefreshApps,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isExpandedScreen) {
                    // Persistent Two-Pane Master-Detail Layout for Tablets, Foldables & Landscape
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(0.55f)
                                .fillMaxHeight()
                        ) {
                            AppsCollectionContent(
                                apps = apps,
                                isGridView = true, // Multi-column adaptive grid on tablets
                                isExpandedScreen = true,
                                selectedPackageNames = selectedPackageNames,
                                focusedPackageName = focusedApp?.packageName,
                                onAppClick = { onSelectAppForDetails(it) },
                                onToggleSelect = onToggleAppMultiSelect,
                                onQuickShare = onShareSingleApp
                            )
                        }

                        // Side-by-side Detail Pane
                        Surface(
                            modifier = Modifier
                                .weight(0.45f)
                                .fillMaxHeight(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            tonalElevation = 1.dp
                        ) {
                            val activeApp = focusedApp ?: apps.firstOrNull()
                            if (activeApp != null) {
                                AppDetailPanel(
                                    app = activeApp,
                                    computedSha256 = computedSha256Map[activeApp.packageName],
                                    isComputingSha256 = isComputingSha256,
                                    onToggleFavorite = { onToggleFavorite(activeApp.packageName) },
                                    onShareApp = { onShareSingleApp(activeApp) },
                                    onSaveToDownloads = { onSaveSingleAppToDownloads(activeApp) },
                                    onReinstallVerify = { onReinstallVerifyApp(activeApp) },
                                    onWifiShare = { onWifiShareApps(listOf(activeApp)) },
                                    onComputeSha256 = { onComputeSha256(activeApp) }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.selectAppPrompt,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Compact Width (Phone) Layout
                    AppsCollectionContent(
                        apps = apps,
                        isGridView = isGridView,
                        isExpandedScreen = false,
                        selectedPackageNames = selectedPackageNames,
                        focusedPackageName = focusedApp?.packageName,
                        onAppClick = { onSelectAppForDetails(it) },
                        onToggleSelect = onToggleAppMultiSelect,
                        onQuickShare = onShareSingleApp
                    )
                }
            }
        }

        // Floating Multi-Select Action Bar
        AnimatedVisibility(
            visible = selectedPackageNames.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, BurnishedGold, RoundedCornerShape(18.dp))
                    .testTag("multi_select_action_bar")
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
                                FileNameAndHashUtils.formatBytes(selectedTotalBytes)
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = BurnishedGold,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onClearMultiSelect,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = strings.clearSelectionBtn)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onShareSelectedApps,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BurnishedGold,
                                contentColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("multi_share_selected_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.shareSelectedBtn, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = onBackupSelectedToZip,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("multi_backup_zip_button")
                        ) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.backupSelectedZipBtn)
                        }

                        FilledTonalButton(
                            onClick = { onWifiShareApps(selectedApps) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("multi_wifi_share_button")
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.wifiShareAction)
                        }
                    }
                }
            }
        }
    }

    // Compact Phone ModalBottomSheet for App Details
    if (!isExpandedScreen && focusedApp != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onSelectAppForDetails(null) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AppDetailPanel(
                app = focusedApp,
                computedSha256 = computedSha256Map[focusedApp.packageName],
                isComputingSha256 = isComputingSha256,
                onToggleFavorite = { onToggleFavorite(focusedApp.packageName) },
                onShareApp = { onShareSingleApp(focusedApp) },
                onSaveToDownloads = { onSaveSingleAppToDownloads(focusedApp) },
                onReinstallVerify = { onReinstallVerifyApp(focusedApp) },
                onWifiShare = { onWifiShareApps(listOf(focusedApp)) },
                onComputeSha256 = { onComputeSha256(focusedApp) }
            )
        }
    }

    // Multi-App Quick Backup (.zip) Progress Dialog
    if (quickZipProgress.isRunning) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(strings.backupSelectedZipBtn) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${quickZipProgress.currentIndex} / ${quickZipProgress.totalCount}: ${quickZipProgress.currentAppLabel}")
                    LinearProgressIndicator(
                        progress = {
                            if (quickZipProgress.totalCount > 0) {
                                quickZipProgress.currentIndex.toFloat() / quickZipProgress.totalCount
                            } else 0f
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onCancelQuickZipBackup) {
                    Text(strings.cancelBtn)
                }
            }
        )
    }
}

@Composable
private fun AppsCollectionContent(
    apps: List<InstalledAppInfo>,
    isGridView: Boolean,
    isExpandedScreen: Boolean,
    selectedPackageNames: Set<String>,
    focusedPackageName: String?,
    onAppClick: (InstalledAppInfo) -> Unit,
    onToggleSelect: (String) -> Unit,
    onQuickShare: (InstalledAppInfo) -> Unit
) {
    val strings = LocalAppStrings.current

    if (apps.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = strings.emptyAppsList,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    if (isGridView) {
        val gridCells = if (isExpandedScreen) {
            GridCells.Adaptive(minSize = 260.dp)
        } else {
            GridCells.Fixed(2)
        }
        LazyVerticalGrid(
            columns = gridCells,
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 12.dp,
                bottom = if (selectedPackageNames.isNotEmpty()) 120.dp else 24.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppCardItem(
                    app = app,
                    isSelected = selectedPackageNames.contains(app.packageName),
                    isFocused = focusedPackageName == app.packageName,
                    onClick = { onAppClick(app) },
                    onLongClick = { onToggleSelect(app.packageName) },
                    onCheckboxToggle = { onToggleSelect(app.packageName) },
                    onQuickShare = { onQuickShare(app) }
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 10.dp,
                bottom = if (selectedPackageNames.isNotEmpty()) 120.dp else 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppCardItem(
                    app = app,
                    isSelected = selectedPackageNames.contains(app.packageName),
                    isFocused = focusedPackageName == app.packageName,
                    onClick = { onAppClick(app) },
                    onLongClick = { onToggleSelect(app.packageName) },
                    onCheckboxToggle = { onToggleSelect(app.packageName) },
                    onQuickShare = { onQuickShare(app) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCardItem(
    app: InstalledAppInfo,
    isSelected: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCheckboxToggle: () -> Unit,
    onQuickShare: () -> Unit
) {
    val strings = LocalAppStrings.current
    val borderColor = when {
        isSelected -> BurnishedGold
        isFocused -> BurnishedGold.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isSelected || isFocused) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("app_card_${app.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onCheckboxToggle() },
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (app.iconBitmap != null) {
                    Image(
                        bitmap = app.iconBitmap.asImageBitmap(),
                        contentDescription = app.label,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = app.label,
                        tint = BurnishedGold,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (app.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = BurnishedGold,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                LtrBox {
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Split APK / Single APK Badge
                    Surface(
                        color = if (app.isSplitApk) {
                            BurnishedGold.copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (app.isSplitApk) strings.splitBadge else strings.singleApkBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (app.isSplitApk) BurnishedGold else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    LtrBox {
                        Text(
                            text = "v${app.versionName} • ${FileNameAndHashUtils.formatBytes(app.totalSizeBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(
                onClick = onQuickShare,
                modifier = Modifier.testTag("quick_share_${app.packageName}")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = strings.shareAction,
                    tint = BurnishedGold
                )
            }
        }
    }
}
