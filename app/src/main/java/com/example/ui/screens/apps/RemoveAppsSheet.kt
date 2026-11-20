package com.example.ui.screens.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.InstalledAppInfo
import com.example.installer.RemoveMode
import com.example.installer.RemovedApp
import com.example.installer.ShellCommands
import com.example.ui.components.design.AdaptiveList
import com.example.ui.components.design.AppIcon
import com.example.ui.components.design.BadgeTone
import com.example.ui.components.design.ChoiceChipRow
import com.example.ui.components.design.GradientProgressBar
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.ScreenScaffold
import com.example.ui.components.design.SecondaryButton
import com.example.ui.components.design.ShareApkBottomSheet
import com.example.ui.components.design.StatusBadge
import com.example.ui.components.design.SurfaceCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens
import com.example.util.ArabicText
import com.example.util.FileNameAndHashUtils

/**
 * Confirms what will happen to each selected app before anything is removed: user apps are deleted
 * (permanent), system apps are removed for this user or disabled (reversible), protected ones are left alone.
 */
@Composable
fun RemoveAppsSheet(
    apps: List<InstalledAppInfo>,
    shizukuReady: Boolean,
    running: Boolean,
    result: Pair<Int, Int>?,
    onConfirm: (List<Pair<InstalledAppInfo, RemoveMode>>) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val modes = remember { mutableStateMapOf<String, RemoveMode>() }
    fun modeOf(a: InstalledAppInfo) = modes[a.packageName] ?: if (a.isSystemApp) RemoveMode.UNINSTALL_FOR_USER else RemoveMode.UNINSTALL
    val removable = apps.filter { !ShellCommands.isProtected(it.packageName) && (shizukuReady || !it.isSystemApp) }

    ShareApkBottomSheet(onDismiss = onDismiss, allowSwipeDismiss = !running) {
        Text(s.removeTitle, style = MaterialTheme.typography.titleLarge, color = c.onBackground)
        if (apps.any { !it.isSystemApp }) Text(s.removeUserHint, style = MaterialTheme.typography.bodySmall, color = c.error)
        if (apps.any { it.isSystemApp }) Text(if (shizukuReady) s.removeSystemHint else s.removeNeedsShizuku, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            apps.forEach { app ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppIcon(app.label, app.iconBitmap, size = 40.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(app.label, style = MaterialTheme.typography.bodyLarge, color = c.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        when {
                            ShellCommands.isProtected(app.packageName) -> StatusBadge(s.removeProtected, BadgeTone.Warning)
                            app.isSystemApp && !shizukuReady -> StatusBadge(s.removeNeedsShizuku.substringBefore('.'), BadgeTone.Warning)
                            app.isSystemApp -> ChoiceChipRow(
                                listOf(RemoveMode.UNINSTALL_FOR_USER to s.removeModeForUser, RemoveMode.DISABLE to s.removeModeDisable),
                                modeOf(app), onSelect = { modes[app.packageName] = it }
                            )
                            else -> Text("${s.removeModeUninstall} · ${FileNameAndHashUtils.formatBytes(app.totalSizeBytes)}", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        when {
            running -> GradientProgressBar(progress = null, height = 6.dp)
            result != null -> {
                StatusBadge(
                    s.removeDoneFormat.format(ArabicText.count(result.first, s.appForms, s.isArabic), ArabicText.count(result.second, s.appForms, s.isArabic)),
                    if (result.second == 0) BadgeTone.Success else BadgeTone.Warning
                )
                SecondaryButton(s.doneBtn, onDismiss, Modifier.fillMaxWidth())
            }
            else -> PrimaryButton(
                s.removeConfirmFormat.format(ArabicText.count(removable.size, s.appForms, s.isArabic)),
                { onConfirm(removable.map { it to modeOf(it) }) },
                Modifier.fillMaxWidth(), icon = Icons.Outlined.DeleteOutline, enabled = removable.isNotEmpty(),
                height = ShareApkTokens.SheetButtonHeight
            )
        }
    }
}

/** More → Removed apps: system apps this app removed or disabled, each with Restore. */
@Composable
fun RemovedAppsScreen(items: List<RemovedApp>, shizukuReady: Boolean, onRestore: (RemovedApp) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    ScreenScaffold(title = s.tileRemovedTitle, onBack = onBack, modifier = modifier) {
        AdaptiveList(
            contentPadding = PaddingValues(horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp),
            gap = ShareApkTokens.CardGap,
            modifier = Modifier.fillMaxSize()
        ) {
            if (!shizukuReady) item { SurfaceCard(Modifier.fillMaxWidth()) { Text(s.shizukuNotRunning, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant) } }
            if (items.isEmpty()) item { SurfaceCard(Modifier.fillMaxWidth()) { Text(s.removedEmpty, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant) } }
            items.forEach { app ->
                item(key = app.packageName) {
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(app.label.ifBlank { app.packageName }, style = MaterialTheme.typography.bodyLarge, color = c.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (app.mode == RemoveMode.DISABLE) s.removedDisabledLabel else s.removedForUserLabel, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                            }
                            SecondaryButton(s.restoreAppBtn, { onRestore(app) }, enabled = shizukuReady)
                        }
                    }
                }
            }
        }
    }
}
