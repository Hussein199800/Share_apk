package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.InstalledAppInfo
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocaleHelper
import com.example.ui.theme.LtrBox
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
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header: App Icon, Title, Favorite Star, and Split Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        width = 1.dp,
                        color = BurnishedGold.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (app.iconBitmap != null) {
                    Image(
                        bitmap = app.iconBitmap.asImageBitmap(),
                        contentDescription = app.label,
                        modifier = Modifier.size(56.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = app.label,
                        tint = BurnishedGold,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (app.isSplitApk) {
                            BurnishedGold.copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (app.isSplitApk) strings.splitBadge else strings.singleApkBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (app.isSplitApk) BurnishedGold else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    LtrBox {
                        Text(
                            text = FileNameAndHashUtils.formatBytes(app.totalSizeBytes),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("detail_favorite_button")
            ) {
                Icon(
                    imageVector = if (app.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = strings.filterFavorites,
                    tint = if (app.isFavorite) BurnishedGold else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Package Name Card with one-tap Copy
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.packageNameLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LtrBox {
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(
                    onClick = {
                        copyToClipboard(context, "Package Name", app.packageName, strings.copiedToast)
                    },
                    modifier = Modifier.testTag("copy_package_name_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = strings.copyAction,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Primary Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onShareApp,
                modifier = Modifier
                    .weight(1f)
                    .testTag("detail_share_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BurnishedGold,
                    contentColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.shareAction, fontWeight = FontWeight.Bold)
            }

            FilledTonalButton(
                onClick = onSaveToDownloads,
                modifier = Modifier
                    .weight(1f)
                    .testTag("detail_save_downloads_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.saveDownloadsAction)
            }
        }

        // Secondary Action Buttons: Built-in Installer, Wi-Fi Share, System Info, Google Play
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onReinstallVerify,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("detail_reinstall_verify_button")
            ) {
                Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.reinstallVerifyAction)
            }

            OutlinedButton(
                onClick = onWifiShare,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("detail_wifi_share_button")
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.wifiShareAction)
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${app.packageName}")
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    runCatching { context.startActivity(intent) }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("detail_system_info_button")
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.systemInfoAction)
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(app.googlePlayUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("detail_play_store_button")
            ) {
                Icon(Icons.Default.Shop, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.openPlayStoreAction)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Technical Metadata Grid
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetadataRow(
                    label = strings.versionLabel,
                    ltrValue = "v${app.versionName} (${app.versionCode})"
                )
                MetadataRow(
                    label = strings.sdkRangeLabel,
                    ltrValue = "minSdk ${app.minSdk} → targetSdk ${app.targetSdk} • ABIs: ${app.supportedAbis.joinToString()}"
                )
                MetadataRow(
                    label = strings.installedDateLabel,
                    ltrValue = FileNameAndHashUtils.formatDate(app.firstInstallTime)
                )
                MetadataRow(
                    label = strings.updatedDateLabel,
                    ltrValue = FileNameAndHashUtils.formatDate(app.lastUpdateTime)
                )
                MetadataRow(
                    label = strings.installerSourceLabel,
                    ltrValue = app.installerDisplayName
                )
                MetadataRow(
                    label = strings.apkSizeLabel,
                    ltrValue = "${FileNameAndHashUtils.formatBytes(app.totalSizeBytes)} (${1 + app.splitSourceDirs.size} APK files)"
                )
            }
        }

        // Split APK Modules List (if Split APK)
        if (app.isSplitApk) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${strings.splitFilesLabel} (${app.splitSourceDirs.size + 1})",
                        style = MaterialTheme.typography.titleSmall,
                        color = BurnishedGold,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { LtrBox { Text("base.apk", style = MaterialTheme.typography.labelSmall) } },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        app.splitNames.forEach { splitName ->
                            AssistChip(
                                onClick = {},
                                label = { LtrBox { Text("$splitName.apk", style = MaterialTheme.typography.labelSmall) } },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                }
            }
        }

        // Signing Certificate & Base APK File SHA-256 Checksum Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            ),
            shape = RoundedCornerShape(14.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = BurnishedGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.signingCertSha256Label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = {
                            copyToClipboard(context, "Signing SHA-256", app.signingSha256, strings.copiedToast)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = strings.copyAction,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                LtrBox {
                    Text(
                        text = app.signingSha256,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.apkFileSha256Label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!computedSha256.isNullOrBlank()) {
                        IconButton(
                            onClick = {
                                copyToClipboard(context, "APK SHA-256", computedSha256, strings.copiedToast)
                            },
                            modifier = Modifier.testTag("copy_apk_sha256_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = strings.copyAction,
                                tint = BurnishedGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (!isComputingSha256) {
                        OutlinedButton(onClick = onComputeSha256) {
                            Text(strings.calculateSha256Btn, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (isComputingSha256) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = strings.computingSha256,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (!computedSha256.isNullOrBlank()) {
                    LtrBox {
                        Text(
                            text = computedSha256,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = BurnishedGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, ltrValue: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Box(
            modifier = Modifier.weight(0.58f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = LocaleHelper.ltr(ltrValue),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun copyToClipboard(context: Context, label: String, value: String, toastMessage: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
}
