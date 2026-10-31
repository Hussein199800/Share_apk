package com.example.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.HistoryEntity
import com.example.ui.components.copyToClipboard
import com.example.ui.components.design.BadgeTone
import com.example.ui.components.design.GradientIconBox
import com.example.ui.components.design.IconActionButton
import com.example.ui.components.design.ScreenScaffold
import com.example.ui.components.design.StatusBadge
import com.example.ui.components.design.SurfaceCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens
import com.example.util.FileNameAndHashUtils

internal fun historyTone(actionType: String): BadgeTone = when (actionType) {
    "INSTALL", "BACKUP" -> BadgeTone.Success
    "SAVE_DOWNLOADS", "WIFI_SHARE" -> BadgeTone.Info
    else -> BadgeTone.Primary
}

@Composable
fun HistoryScreen(
    historyItems: List<HistoryEntity>,
    onDeleteItem: (Long) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val context = LocalContext.current
    val mono = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace)

    ScreenScaffold(
        title = s.tileHistoryTitle,
        subtitle = s.historyTitle,
        onBack = onBack,
        modifier = modifier,
        action = if (historyItems.isNotEmpty()) ({ IconActionButton(Icons.Outlined.DeleteSweep, s.clearHistoryBtn, onClearAll) }) else null
    ) {
        if (historyItems.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GradientIconBox(ShareApkGradients.sky, size = 56.dp) { Icon(Icons.Outlined.History, null) }
                    Text(s.emptyHistoryMsg, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            return@ScreenScaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(ShareApkTokens.CardGap),
            modifier = Modifier.fillMaxSize()
        ) {
            items(historyItems, key = { it.id }) { item ->
                SurfaceCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatusBadge(item.actionType, historyTone(item.actionType))
                            Text(item.appLabel, style = MaterialTheme.typography.bodyLarge, color = c.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconActionButton(Icons.Outlined.DeleteOutline, s.deleteItemAction, { onDeleteItem(item.id) })
                    }
                    LtrBox {
                        Text(
                            "${item.packageName} • v${item.versionName} (${item.fileFormat}) • ${FileNameAndHashUtils.formatBytes(item.fileSizeBytes)}",
                            style = mono, color = c.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (item.sha256Hash.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                LtrBox { Text("SHA-256: ${item.sha256Hash.take(24)}...", style = mono, color = c.primaryText) }
                                LtrBox { Text(FileNameAndHashUtils.formatDate(item.timestampMillis), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant) }
                            }
                            IconActionButton(Icons.Outlined.ContentCopy, s.copyAction, {
                                copyToClipboard(context, "SHA-256", item.sha256Hash, s.copiedToast)
                            })
                        }
                    } else {
                        LtrBox { Text(FileNameAndHashUtils.formatDate(item.timestampMillis), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}
