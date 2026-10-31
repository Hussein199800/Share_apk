package com.example.ui.screens.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AppFilter
import com.example.data.model.AppSortOption
import com.example.data.model.InstalledAppInfo
import com.example.ui.components.AppDetailPanel
import com.example.ui.components.design.FilterChipRow
import com.example.ui.components.design.GradientProgressBar
import com.example.ui.components.design.IconActionButton
import com.example.ui.components.design.ScreenScaffold
import com.example.ui.components.design.SearchField
import com.example.ui.components.design.ShareApkBottomSheet
import com.example.ui.components.design.SurfaceCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens
import com.example.ui.viewmodel.QuickZipBackupProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    apps: List<InstalledAppInfo>,
    isLoading: Boolean,
    searchQuery: String,
    selectedFilter: AppFilter?,
    selectedSort: AppSortOption,
    isGridView: Boolean,
    isExpandedScreen: Boolean,
    selectedPackageNames: Set<String>,
    focusedApp: InstalledAppInfo?,
    computedSha256Map: Map<String, String>,
    isComputingSha256: Boolean,
    quickZipProgress: QuickZipBackupProgress,
    onSearchChange: (String) -> Unit,
    onFilterChange: (AppFilter?) -> Unit,
    onSortChange: (AppSortOption) -> Unit,
    onToggleGridView: () -> Unit,
    onRefreshApps: () -> Unit,
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
    onSendSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val selectedApps = remember(apps, selectedPackageNames) { apps.filter { it.packageName in selectedPackageNames } }

    val detailPanel: @Composable (InstalledAppInfo) -> Unit = { app ->
        AppDetailPanel(
            app = app,
            computedSha256 = computedSha256Map[app.packageName],
            isComputingSha256 = isComputingSha256,
            onToggleFavorite = { onToggleFavorite(app.packageName) },
            onShareApp = { onShareSingleApp(app) },
            onSaveToDownloads = { onSaveSingleAppToDownloads(app) },
            onReinstallVerify = { onReinstallVerifyApp(app) },
            onWifiShare = { onWifiShareApps(listOf(app)) },
            onComputeSha256 = { onComputeSha256(app) }
        )
    }

    ScreenScaffold(
        title = s.navApps,
        subtitle = s.appsCountFormat.format(apps.size),
        modifier = modifier,
        action = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconActionButton(
                    if (isGridView) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                    s.toggleViewAction,
                    onToggleGridView,
                    Modifier.testTag("toggle_grid_list_button")
                )
                SortMenu(selectedSort, onSortChange)
            }
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                SearchField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = s.searchHint,
                    modifier = Modifier
                        .padding(horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp)
                        .testTag("search_apps_input")
                )
                FilterChipRow(
                    allLabel = s.filterAll,
                    options = listOf(
                        AppFilter.USER_APPS to s.filterUserApps,
                        AppFilter.SYSTEM_APPS to s.filterSystemApps,
                        AppFilter.FAVORITES to s.filterFavorites,
                        AppFilter.SPLIT_ONLY to s.filterSplitOnly
                    ),
                    selected = selectedFilter,
                    onSelect = onFilterChange,
                    modifier = Modifier.padding(horizontal = ShareApkTokens.ScreenPadding)
                )
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = onRefreshApps,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    val bottomPad = if (selectedPackageNames.isNotEmpty()) 150.dp else 16.dp
                    if (isExpandedScreen) {
                        Row(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(0.55f).fillMaxHeight()) {
                                AppsCollection(apps, true, true, selectedPackageNames, focusedApp?.packageName, bottomPad, onSelectAppForDetails, onToggleAppMultiSelect)
                            }
                            Box(Modifier.weight(0.45f).fillMaxHeight().padding(12.dp)) {
                                val active = focusedApp ?: apps.firstOrNull()
                                SurfaceCard(Modifier.fillMaxSize(), contentPadding = PaddingValues(0.dp)) {
                                    if (active != null) detailPanel(active)
                                    else Text(
                                        s.selectAppPrompt,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ShareApkTheme.colors.onSurfaceVariant,
                                        modifier = Modifier.padding(24.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        AppsCollection(apps, isGridView, false, selectedPackageNames, focusedApp?.packageName, bottomPad, onSelectAppForDetails, onToggleAppMultiSelect)
                    }
                }
            }
            if (selectedPackageNames.isNotEmpty()) {
                SelectionBar(
                    count = selectedPackageNames.size,
                    totalBytes = selectedApps.sumOf { it.totalSizeBytes },
                    onClear = onClearMultiSelect,
                    onShare = onShareSelectedApps,
                    onZip = onBackupSelectedToZip,
                    onWifi = { onWifiShareApps(selectedApps) },
                    onSend = onSendSelected,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    if (!isExpandedScreen && focusedApp != null) {
        ShareApkBottomSheet(onDismiss = { onSelectAppForDetails(null) }) { detailPanel(focusedApp) }
    }

    if (quickZipProgress.isRunning) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = ShareApkTheme.colors.surface,
            title = { Text(s.backupSelectedZipBtn) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${quickZipProgress.currentIndex} / ${quickZipProgress.totalCount}: ${quickZipProgress.currentAppLabel}")
                    GradientProgressBar(
                        if (quickZipProgress.totalCount > 0) quickZipProgress.currentIndex.toFloat() / quickZipProgress.totalCount else null
                    )
                }
            },
            confirmButton = { TextButton(onClick = onCancelQuickZipBackup) { Text(s.cancelBtn) } }
        )
    }
}

@Composable
private fun SortMenu(selected: AppSortOption, onSortChange: (AppSortOption) -> Unit) {
    val s = LocalAppStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconActionButton(Icons.AutoMirrored.Outlined.Sort, s.sortAction, { expanded = true }, Modifier.testTag("sort_dropdown_button"))
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                AppSortOption.NAME_ASC to s.sortName,
                AppSortOption.SIZE_DESC to s.sortSize,
                AppSortOption.INSTALLED_DATE to s.sortInstalled,
                AppSortOption.RECENTLY_UPDATED to s.sortUpdated
            ).forEach { (opt, label) ->
                DropdownMenuItem(
                    text = { Text(label, color = if (opt == selected) ShareApkTheme.colors.primaryText else ShareApkTheme.colors.onBackground) },
                    onClick = { onSortChange(opt); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun AppsCollection(
    apps: List<InstalledAppInfo>,
    isGrid: Boolean,
    isExpanded: Boolean,
    selected: Set<String>,
    focusedPackage: String?,
    bottomPad: androidx.compose.ui.unit.Dp,
    onOpen: (InstalledAppInfo) -> Unit,
    onToggle: (String) -> Unit
) {
    if (apps.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                LocalAppStrings.current.emptyAppsList,
                style = MaterialTheme.typography.bodyMedium,
                color = ShareApkTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }
    val padding = PaddingValues(start = ShareApkTokens.ScreenPadding, end = ShareApkTokens.ScreenPadding, top = 8.dp, bottom = bottomPad)
    if (isGrid) {
        LazyVerticalGrid(
            columns = if (isExpanded) GridCells.Adaptive(220.dp) else GridCells.Fixed(2),
            contentPadding = padding,
            horizontalArrangement = Arrangement.spacedBy(ShareApkTokens.CardGap),
            verticalArrangement = Arrangement.spacedBy(ShareApkTokens.CardGap),
            modifier = Modifier.fillMaxSize()
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppGridCard(app, app.packageName in selected, app.packageName == focusedPackage, { onOpen(app) }, { onToggle(app.packageName) })
            }
        }
    } else {
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(ShareApkTokens.CardGap),
            modifier = Modifier.fillMaxSize()
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppListItem(app, app.packageName in selected, app.packageName == focusedPackage, { onOpen(app) }, { onToggle(app.packageName) })
            }
        }
    }
}
