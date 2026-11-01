package com.example.ui.screens.receive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.receive.ReceiveEvent
import com.example.receive.ReceivedEntry
import com.example.receive.ReceiveState
import com.example.receive.ReceivedItem
import com.example.receive.ReceiverStatus
import com.example.ui.components.design.AppIcon
import com.example.ui.components.design.BadgeTone
import com.example.ui.components.design.FileTypeIcon
import com.example.ui.components.design.GradientIconBox
import com.example.ui.components.design.HelpCard
import com.example.ui.components.design.GradientProgressBar
import com.example.ui.components.design.HeroCard
import com.example.ui.components.design.IconActionButton
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.ScreenScaffold
import com.example.ui.components.design.SecondaryButton
import com.example.ui.components.design.StatusBadge
import com.example.ui.components.design.SurfaceCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens
import com.example.util.FileNameAndHashUtils

@Composable
fun ReceiveScreen(
    status: ReceiverStatus,
    enabled: Boolean,
    state: ReceiveState,
    received: List<ReceivedItem>,
    onToggleEnabled: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onAcknowledge: () -> Unit,
    onInstall: (ReceivedItem) -> Unit,
    onDelete: (ReceivedItem) -> Unit,
    lastEvent: ReceiveEvent? = null,
    receivedOther: List<ReceivedEntry> = emptyList(),
    onOpenEntry: (ReceivedEntry) -> Unit = {},
    onCopyEntry: (ReceivedEntry) -> Unit = {},
    onRemoveEntry: (ReceivedEntry) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    ScreenScaffold(title = s.receiveTitle, subtitle = s.receiveSubtitle, modifier = modifier) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(ShareApkTokens.CardGap),
            modifier = Modifier.fillMaxSize()
        ) {
            item { DeviceCard(status, enabled, onToggleEnabled) }
            if (lastEvent != null && state is ReceiveState.Idle) item {
                StatusBadge(
                    when (lastEvent) {
                        is ReceiveEvent.TimedOut -> s.evTimedOut.format(lastEvent.sender)
                        is ReceiveEvent.Busy -> s.evBusy.format(lastEvent.sender)
                    },
                    BadgeTone.Warning
                )
            }
            item {
                HelpCard(
                    title = s.helpReceiveTitle,
                    steps = s.helpReceiveSteps.format(status.alias).split('|'),
                    tips = s.helpReceiveTips.split('|'),
                    initiallyExpanded = received.isEmpty()
                )
            }

            when (state) {
                is ReceiveState.Receiving -> item {
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        Text(s.receivingFrom.format(state.sender.alias), style = MaterialTheme.typography.titleSmall, color = c.onBackground)
                        val total = state.totalBytes.coerceAtLeast(1)
                        GradientProgressBar(state.receivedBytes.toFloat() / total)
                        LtrBox {
                            Text(
                                "${FileNameAndHashUtils.formatBytes(state.receivedBytes)} / ${FileNameAndHashUtils.formatBytes(state.totalBytes)}",
                                style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant
                            )
                        }
                        state.files.forEach { f ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(f.fileName, style = MaterialTheme.typography.bodySmall, color = c.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                if (f.done) Icon(Icons.Outlined.CheckCircle, null, tint = c.success, modifier = Modifier.size(18.dp))
                            }
                        }
                        SecondaryButton(s.cancelBtn, onCancel, Modifier.fillMaxWidth())
                    }
                }
                is ReceiveState.Done -> item {
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GradientIconBox(ShareApkGradients.green, size = 40.dp) { Icon(Icons.Outlined.CheckCircle, null, Modifier.size(22.dp)) }
                            Text(s.receivedFrom.format(state.sender.alias), style = MaterialTheme.typography.titleSmall, color = c.onBackground, modifier = Modifier.weight(1f))
                            SecondaryButton(s.doneBtn, onAcknowledge)
                        }
                    }
                }
                else -> Unit
            }

            item { Text(s.receivedTitle, style = MaterialTheme.typography.titleMedium, color = c.onBackground) }
            if (received.isEmpty()) {
                item {
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        Text(s.receivedEmpty, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            items(received, key = { it.file.path }) { item -> ReceivedRow(item, onInstall, onDelete) }
            if (receivedOther.isNotEmpty()) {
                item { Text(s.receivedFilesTitle, style = MaterialTheme.typography.titleMedium, color = c.onBackground) }
                items(receivedOther, key = { it.receivedAt.toString() + it.name + (it.text?.hashCode() ?: 0) }) { e ->
                    SurfaceCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FileTypeIcon(if (e.isText) "x.txt" else e.name, e.mimeType, size = 40.dp)
                            Column(Modifier.weight(1f)) {
                                Text(e.text ?: e.name, style = MaterialTheme.typography.bodyLarge, color = c.onBackground, maxLines = if (e.isText) 3 else 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (e.isText) s.textMessageLabel else FileNameAndHashUtils.formatBytes(e.size),
                                    style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant
                                )
                            }
                            if (e.isText) IconActionButton(Icons.Outlined.ContentCopy, s.copyBtn, { onCopyEntry(e) })
                            else IconActionButton(Icons.Outlined.OpenInNew, s.openBtn, { onOpenEntry(e) })
                            IconActionButton(Icons.Outlined.Close, s.deleteItemAction, { onRemoveEntry(e) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(status: ReceiverStatus, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val s = LocalAppStrings.current
    HeroCard(Modifier.fillMaxWidth()) {
        GradientIconBox(if (status.running) ShareApkGradients.green else ShareApkGradients.rose, size = 52.dp) {
            Icon(if (status.running) Icons.Outlined.Wifi else Icons.Outlined.WifiOff, null, Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(if (enabled) s.receiveVisibleAs else s.receiveOff, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
            Text(status.alias, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            when {
                status.error != null && enabled -> Text(s.receiveStartFailed.format(status.error), style = MaterialTheme.typography.bodySmall)
                status.running && status.ip != null -> LtrBox {
                    Text("${status.ip}:${status.port}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                // Clear on/off on top of the gradient card.
                checkedTrackColor = ShareApkGradients.green.end,
                checkedThumbColor = Color.White,
                checkedBorderColor = Color.Transparent,
                uncheckedTrackColor = Color.White.copy(alpha = 0.18f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.85f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun ReceivedRow(item: ReceivedItem, onInstall: (ReceivedItem) -> Unit, onDelete: (ReceivedItem) -> Unit) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    SurfaceCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppIcon(item.label, item.icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.label, style = MaterialTheme.typography.bodyLarge, color = c.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    item.status?.let { VersionBadge(it) }
                    LtrBox {
                        Text(
                            listOfNotNull(item.versionName?.let { "v$it" }, FileNameAndHashUtils.formatBytes(item.file.length())).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 1
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton(s.installNowShort, { onInstall(item) }, Modifier.weight(1f), icon = Icons.Outlined.InstallMobile)
            IconActionButton(Icons.Outlined.DeleteOutline, s.deleteItemAction, { onDelete(item) })
        }
    }
}
