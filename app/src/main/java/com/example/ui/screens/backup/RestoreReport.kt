package com.example.ui.screens.backup

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BatchRestoreSummary
import com.example.data.model.RestoreCandidateApp
import com.example.installer.ShizukuStatus
import com.example.ui.components.design.BadgeTone
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.SecondaryButton
import com.example.ui.components.design.StatusBadge
import com.example.ui.components.design.SurfaceCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ShareApkTheme
import com.example.util.ArabicText

/** Three lists after a batch install: installed, failed (retry), skipped (install / ignore / store). */
@Composable
internal fun RestoreReport(summary: BatchRestoreSummary, onRetry: (Set<String>) -> Unit, onIgnore: (Set<String>) -> Unit) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val context = LocalContext.current
    fun n(k: Int) = ArabicText.count(k, s.appForms, s.isArabic)
    fun openStore(pkg: String) = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    SurfaceCard(Modifier.fillMaxWidth()) {
        Text(s.restoreReportTitle, style = MaterialTheme.typography.titleMedium, color = c.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusBadge(s.restoreInstalledFormat.format(n(summary.installedCount)), BadgeTone.Success)
            if (summary.failedCount > 0) StatusBadge(s.restoreFailedFormat.format(n(summary.failedCount)), BadgeTone.Error)
            if (summary.skippedCount > 0) StatusBadge(s.restoreSkippedFormat.format(n(summary.skippedCount)), BadgeTone.Warning)
        }
    }

    if (summary.failed.isNotEmpty()) SurfaceCard(Modifier.fillMaxWidth()) {
        Header(Icons.Outlined.ErrorOutline, s.restoreFailedFormat.format(n(summary.failedCount)), c.error)
        summary.failed.forEach { (app, reason) ->
            AppLine(app, reason) { SecondaryButton(s.retryBtn, { onRetry(setOf(app.entry.packageName)) }) }
        }
        PrimaryButton(s.retryAllBtn, { onRetry(summary.failed.map { it.first.entry.packageName }.toSet()) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Refresh)
    }

    if (summary.skipped.isNotEmpty()) SurfaceCard(Modifier.fillMaxWidth()) {
        Header(Icons.Outlined.SkipNext, s.restoreSkippedFormat.format(n(summary.skippedCount)), c.warning)
        summary.skipped.forEach { app ->
            AppLine(app, null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SecondaryButton(s.restoreInstallNowBtn, { onRetry(setOf(app.entry.packageName)) })
                    SecondaryButton(s.ignoreBtn, { onIgnore(setOf(app.entry.packageName)) })
                    SecondaryButton(s.openStoreBtn, { openStore(app.entry.packageName) })
                }
            }
        }
        val all = summary.skipped.map { it.entry.packageName }.toSet()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(s.installAllBtn, { onRetry(all) }, Modifier.weight(1f))
            SecondaryButton(s.ignoreAllBtn, { onIgnore(all) }, Modifier.weight(1f))
        }
    }

    if (summary.installed.isNotEmpty()) SurfaceCard(Modifier.fillMaxWidth()) {
        Header(Icons.Outlined.CheckCircle, s.restoreInstalledFormat.format(n(summary.installedCount)), c.success)
        summary.installed.forEach { app -> AppLine(app, null) {} }
    }
}

@Composable
private fun Header(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = tint)
        Text(text, style = MaterialTheme.typography.titleSmall, color = ShareApkTheme.colors.onBackground)
    }
}

@Composable
private fun AppLine(app: RestoreCandidateApp, reason: String?, actions: @Composable () -> Unit) {
    val c = ShareApkTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(app.entry.label, style = MaterialTheme.typography.bodyLarge, color = c.onBackground, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (reason != null) Text(reason, style = MaterialTheme.typography.bodySmall, color = c.error)
        actions()
    }
}

/** Shizuku status and the one action that moves it forward. */
@Composable
internal fun ShizukuCard(status: ShizukuStatus, onAllow: () -> Unit) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val context = LocalContext.current
    SurfaceCard(Modifier.fillMaxWidth()) {
        Text(s.shizukuTitle, style = MaterialTheme.typography.titleSmall, color = c.onBackground)
        Text(
            when (status) {
                ShizukuStatus.READY -> s.shizukuReady
                ShizukuStatus.NOT_INSTALLED -> s.shizukuNotInstalled
                ShizukuStatus.NOT_RUNNING -> s.shizukuNotRunning
                ShizukuStatus.NO_PERMISSION -> s.shizukuNoPermission
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (status == ShizukuStatus.READY) c.success else c.onSurfaceVariant
        )
        when (status) {
            ShizukuStatus.NO_PERMISSION -> SecondaryButton(s.shizukuAllowBtn, onAllow, Modifier.fillMaxWidth())
            ShizukuStatus.NOT_INSTALLED -> SecondaryButton(s.shizukuGetBtn, {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }, Modifier.fillMaxWidth())
            ShizukuStatus.NOT_RUNNING -> SecondaryButton(s.shizukuOpenBtn, {
                context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")?.let { runCatching { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
            }, Modifier.fillMaxWidth())
            ShizukuStatus.READY -> Unit
        }
    }
}
