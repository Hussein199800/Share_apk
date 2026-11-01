package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shop
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.InstalledAppInfo
import com.example.ui.components.design.AppIcon
import com.example.ui.components.design.BadgeTone
import com.example.ui.components.design.GradientProgressBar
import com.example.ui.components.design.IconActionButton
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.SecondaryButton
import com.example.ui.components.design.StatusBadge
import com.example.ui.components.design.SurfaceCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocaleHelper
import com.example.ui.theme.LtrBox
import com.example.ui.theme.ShareApkTheme
import com.example.util.FileNameAndHashUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppDetailPanel(
    app: InstalledAppInfo,
    computedSha256: String?,
    isComputingSha256: Boolean,
    onToggleFavorite: () -> Unit,
    onShareApp: () -> Unit,
    onSaveToDownloads: () -> Unit,
    onReinstallVerify: () -> Unit,
    onWifiShare: () -> Unit,
    onComputeSha256: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val context = LocalContext.current
    val mono = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AppIcon(app.label, app.iconBitmap, size = 64.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(app.label, style = MaterialTheme.typography.titleLarge, color = c.onBackground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(
                        if (app.isSplitApk) s.splitBadge else s.singleApkBadge,
                        if (app.isSplitApk) BadgeTone.Info else BadgeTone.Primary
                    )
                    LtrBox {
                        Text(FileNameAndHashUtils.formatBytes(app.totalSizeBytes), style = MaterialTheme.typography.labelLarge, color = c.primaryText)
                    }
                }
            }
            IconActionButton(
                if (app.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                s.filterFavorites,
                onToggleFavorite,
                Modifier.testTag("detail_favorite_button")
            )
        }

        // Package name
        SurfaceCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(s.packageNameLabel, style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
                    LtrBox { Text(app.packageName, style = mono, color = c.onBackground) }
                }
                IconActionButton(
                    Icons.Outlined.ContentCopy, s.copyAction,
                    { copyToClipboard(context, "Package Name", app.packageName, s.copiedToast) },
                    Modifier.testTag("copy_package_name_button")
                )
            }
        }

        // Primary actions
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(s.shareAction, onShareApp, Modifier.weight(1f).testTag("detail_share_button"), icon = Icons.Outlined.Share)
            SecondaryButton(s.saveDownloadsAction, onSaveToDownloads, Modifier.weight(1f).testTag("detail_save_downloads_button"), icon = Icons.Outlined.Download)
        }

        // Secondary actions
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(s.reinstallVerifyAction, onReinstallVerify, Modifier.testTag("detail_reinstall_verify_button"), icon = Icons.Outlined.InstallMobile)
            SecondaryButton(s.wifiShareAction, onWifiShare, Modifier.testTag("detail_wifi_share_button"), icon = Icons.Outlined.Wifi)
            SecondaryButton(
                s.systemInfoAction,
                {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
                        .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    runCatching { context.startActivity(intent) }
                },
                Modifier.testTag("detail_system_info_button"),
                icon = Icons.Outlined.Info
            )
            SecondaryButton(
                s.openPlayStoreAction,
                {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(app.googlePlayUrl)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    runCatching { context.startActivity(intent) }
                },
                Modifier.testTag("detail_play_store_button"),
                icon = Icons.Outlined.Shop
            )
        }

        // Metadata
        SurfaceCard(Modifier.fillMaxWidth()) {
            MetadataRow(s.versionLabel, "v${app.versionName} (${app.versionCode})")
            MetadataRow(s.sdkRangeLabel, "minSdk ${app.minSdk} → targetSdk ${app.targetSdk} • ABIs: ${app.supportedAbis.joinToString()}")
            MetadataRow(s.installedDateLabel, FileNameAndHashUtils.formatDate(app.firstInstallTime))
            MetadataRow(s.updatedDateLabel, FileNameAndHashUtils.formatDate(app.lastUpdateTime))
            MetadataRow(s.installerSourceLabel, app.installerDisplayName)
            MetadataRow(s.apkSizeLabel, "${FileNameAndHashUtils.formatBytes(app.totalSizeBytes)} (${1 + app.splitSourceDirs.size} APK files)")
        }

        // Split modules
        if (app.isSplitApk) {
            SurfaceCard(Modifier.fillMaxWidth()) {
                Text("${s.splitFilesLabel} (${app.splitSourceDirs.size + 1})", style = MaterialTheme.typography.titleSmall, color = c.info)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (listOf("base") + app.splitNames).forEach { name ->
                        LtrBox { StatusBadge("$name.apk", BadgeTone.Info) }
                    }
                }
            }
        }

        // Signing certificate & APK checksum
        SurfaceCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Verified, null, tint = c.success, modifier = Modifier.size(18.dp))
                Text(s.signingCertSha256Label, style = MaterialTheme.typography.labelLarge, color = c.onBackground, modifier = Modifier.weight(1f))
                IconActionButton(Icons.Outlined.ContentCopy, s.copyAction, {
                    copyToClipboard(context, "Signing SHA-256", app.signingSha256, s.copiedToast)
                })
            }
            LtrBox { Text(app.signingSha256, style = mono, color = c.onSurfaceVariant) }
            HorizontalDivider(color = c.outline)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.apkFileSha256Label, style = MaterialTheme.typography.labelLarge, color = c.onBackground, modifier = Modifier.weight(1f))
                when {
                    !computedSha256.isNullOrBlank() -> IconActionButton(
                        Icons.Outlined.ContentCopy, s.copyAction,
                        { copyToClipboard(context, "APK SHA-256", computedSha256, s.copiedToast) },
                        Modifier.testTag("copy_apk_sha256_button")
                    )
                    !isComputingSha256 -> SecondaryButton(s.calculateSha256Btn, onComputeSha256)
                }
            }
            if (isComputingSha256) {
                GradientProgressBar(progress = null, height = 6.dp)
                Text(s.computingSha256, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            } else if (!computedSha256.isNullOrBlank()) {
                LtrBox { Text(computedSha256, style = mono, color = c.primaryText) }
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, ltrValue: String) {
    val c = ShareApkTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, modifier = Modifier.weight(0.42f))
        Box(Modifier.weight(0.58f), contentAlignment = Alignment.CenterEnd) {
            Text(LocaleHelper.ltr(ltrValue), style = MaterialTheme.typography.titleSmall, color = c.onBackground)
        }
    }
}

fun copyToClipboard(context: Context, label: String, value: String, toastMessage: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
}
