package com.example.ui.screens.send

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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.InstalledAppInfo
import com.example.receive.discovery.NearbyDevice
import com.example.send.SendState
import com.example.ui.components.design.AppIcon
import com.example.ui.components.design.BadgeTone
import com.example.ui.components.design.GradientIconBox
import com.example.ui.components.design.GradientProgressBar
import com.example.ui.components.design.HelpCard
import com.example.ui.components.design.HeroCard
import com.example.ui.components.design.IconActionButton
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.ScreenScaffold
import com.example.ui.components.design.SecondaryButton
import com.example.ui.components.design.ShareApkBottomSheet
import com.example.ui.components.design.StatusBadge
import com.example.ui.components.design.SurfaceCard
import com.example.ui.components.design.TextInputField
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens
import com.example.util.FileNameAndHashUtils

@Composable
fun SendScreen(
    selectedApps: List<InstalledAppInfo>,
    onPickApps: () -> Unit,
    onShareViaLink: () -> Unit,
    modifier: Modifier = Modifier,
    peers: List<NearbyDevice> = emptyList(),
    scanning: Boolean = false,
    sendState: SendState = SendState.Idle,
    manualResult: Boolean? = null,
    onSendTo: (NearbyDevice) -> Unit = {},
    onRefresh: () -> Unit = {},
    onAddManual: (String) -> Unit = {},
    onCancel: () -> Unit = {},
    onReset: () -> Unit = {}
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    var showManual by remember { mutableStateOf(false) }
    LaunchedEffect(manualResult) { if (manualResult == true) showManual = false }
    ScreenScaffold(
        title = s.sendTitle,
        subtitle = s.sendSubtitle,
        modifier = modifier,
        action = { IconActionButton(Icons.Outlined.Refresh, s.refreshAction, onRefresh, Modifier.testTag("send_refresh")) }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(ShareApkTokens.CardGap),
            modifier = Modifier.fillMaxSize()
        ) {
            item { SelectionCard(selectedApps, onPickApps) }
            if (sendState !is SendState.Idle) item { SendStatusCard(sendState, onCancel, onReset) }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.nearbyDevicesTitle, style = MaterialTheme.typography.titleMedium, color = c.onBackground, modifier = Modifier.weight(1f))
                    if (scanning) Text(s.scanningNetwork, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
            }
            if (scanning) item { GradientProgressBar(progress = null, height = 4.dp) }
            if (peers.isEmpty()) {
                item {
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        Text(s.nearbyEmpty, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            items(peers, key = { it.info.fingerprint + it.ip }) { peer ->
                DeviceRow(peer, enabled = selectedApps.isNotEmpty()) { onSendTo(peer) }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(s.manualAddressBtn, { showManual = true }, Modifier.weight(1f))
                    SecondaryButton(s.tileLinkTitle, onShareViaLink, Modifier.weight(1f))
                }
            }
            item {
                HelpCard(
                    title = s.helpSendTitle,
                    steps = s.helpSendDirectSteps.split('|'),
                    tips = s.helpSendOtherSteps.split('|'),
                    initiallyExpanded = peers.isEmpty()
                )
            }
        }
    }

    if (showManual) {
        ManualAddressSheet(notFound = manualResult == false, onAdd = onAddManual, onDismiss = { showManual = false })
    }
}

@Composable
private fun SelectionCard(selectedApps: List<InstalledAppInfo>, onPickApps: () -> Unit) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    if (selectedApps.isEmpty()) {
        SurfaceCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GradientIconBox(ShareApkGradients.indigo) { Icon(Icons.Outlined.Apps, null, Modifier.size(24.dp)) }
                Column(Modifier.weight(1f)) {
                    Text(s.sendPickAppsTitle, style = MaterialTheme.typography.titleMedium, color = c.onBackground)
                    Text(s.sendPickAppsBody, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
            }
            PrimaryButton(s.sendPickAppsBtn, onPickApps, Modifier.fillMaxWidth())
        }
    } else {
        HeroCard(Modifier.fillMaxWidth(), onClick = onPickApps) {
            Row(horizontalArrangement = Arrangement.spacedBy((-14).dp)) {
                selectedApps.take(3).forEach { AppIcon(it.label, it.iconBitmap, size = 40.dp) }
            }
            Column(Modifier.weight(1f)) {
                Text(selectedApps.joinToString("، ") { it.label }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    s.sendSelectedSummary.format(selectedApps.size, FileNameAndHashUtils.formatBytes(selectedApps.sumOf { it.totalSizeBytes })),
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun DeviceRow(peer: NearbyDevice, enabled: Boolean, onClick: () -> Unit) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val icon = when (peer.info.deviceType) {
        "desktop", "server", "headless" -> Icons.Outlined.Computer
        "web" -> Icons.Outlined.Language
        "mobile" -> Icons.Outlined.Smartphone
        else -> Icons.Outlined.Devices
    }
    SurfaceCard(Modifier.fillMaxWidth().testTag("peer_${peer.ip}"), onClick = if (enabled) onClick else null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientIconBox(ShareApkGradients.sky) { Icon(icon, null, Modifier.size(22.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(peer.info.alias, style = MaterialTheme.typography.bodyLarge, color = c.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                LtrBox {
                    Text(
                        listOfNotNull(peer.info.deviceModel, peer.ip).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 1
                    )
                }
            }
            StatusBadge(if (peer.isShareApk) s.badgeShareApk else s.badgeLocalSend, if (peer.isShareApk) BadgeTone.Primary else BadgeTone.Info)
            if (enabled) Icon(Icons.AutoMirrored.Outlined.Send, s.navSend, tint = c.primaryText, modifier = Modifier.size(20.dp))
        }
        if (!enabled) Text(s.selectAppsFirst, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
    }
}

@Composable
private fun SendStatusCard(state: SendState, onCancel: () -> Unit, onReset: () -> Unit) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    SurfaceCard(Modifier.fillMaxWidth()) {
        when (state) {
            is SendState.Preparing -> {
                Text(s.sendPreparing, style = MaterialTheme.typography.titleSmall, color = c.onBackground)
                GradientProgressBar(null)
                SecondaryButton(s.cancelBtn, onCancel, Modifier.fillMaxWidth())
            }
            is SendState.Waiting -> {
                Text(s.sendWaiting.format(state.peer.info.alias), style = MaterialTheme.typography.titleSmall, color = c.onBackground)
                GradientProgressBar(null)
                SecondaryButton(s.cancelBtn, onCancel, Modifier.fillMaxWidth())
            }
            is SendState.Sending -> {
                Text(s.sendSending.format(state.peer.info.alias), style = MaterialTheme.typography.titleSmall, color = c.onBackground)
                GradientProgressBar(if (state.total > 0) state.sent.toFloat() / state.total else null)
                LtrBox {
                    Text("${FileNameAndHashUtils.formatBytes(state.sent)} / ${FileNameAndHashUtils.formatBytes(state.total)}", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
                SecondaryButton(s.cancelBtn, onCancel, Modifier.fillMaxWidth())
            }
            is SendState.Done -> ResultRow(Icons.Outlined.CheckCircle, ShareApkGradients.green, s.sendDone.format(state.count, state.peer.info.alias), onReset)
            is SendState.Rejected -> ResultRow(Icons.Outlined.WarningAmber, ShareApkGradients.amber, s.sendRejected.format(state.peer.info.alias), onReset)
            is SendState.Busy -> ResultRow(Icons.Outlined.WarningAmber, ShareApkGradients.amber, s.sendBusy.format(state.peer.info.alias), onReset)
            is SendState.Failed -> ResultRow(Icons.Outlined.WarningAmber, ShareApkGradients.rose, s.sendFailed.format(state.peer.info.alias, state.message), onReset)
            SendState.Idle -> Unit
        }
    }
}

@Composable
private fun ResultRow(icon: androidx.compose.ui.graphics.vector.ImageVector, gradient: com.example.ui.theme.GradientPair, text: String, onReset: () -> Unit) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GradientIconBox(gradient, size = 40.dp) { Icon(icon, null, Modifier.size(22.dp)) }
        Text(text, style = MaterialTheme.typography.titleSmall, color = c.onBackground, modifier = Modifier.weight(1f))
        SecondaryButton(s.doneBtn, onReset)
    }
}

@Composable
private fun ManualAddressSheet(notFound: Boolean, onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    var address by remember { mutableStateOf("") }
    ShareApkBottomSheet(onDismiss = onDismiss) {
        Text(s.manualAddressTitle, style = MaterialTheme.typography.titleMedium, color = c.onBackground)
        Box { LtrBox { TextInputField(address, { address = it.trim() }, s.manualAddressHint) } }
        if (notFound) StatusBadge(s.manualAddressNotFound, BadgeTone.Warning)
        PrimaryButton(s.addBtn, { onAdd(address) }, Modifier.fillMaxWidth(), enabled = address.isNotBlank(), height = ShareApkTokens.SheetButtonHeight)
    }
}
