package com.example.ui.screens.send

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
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


/** "Choose a device" sheet used by the Apps and Files tabs. */
@Composable
fun DevicePickerSheet(
    itemsCount: Int,
    itemsBytes: Long,
    peers: List<NearbyDevice>,
    scanning: Boolean,
    sendState: SendState,
    manualResult: Boolean?,
    onSendTo: (NearbyDevice) -> Unit,
    onRefresh: () -> Unit,
    onAddManual: (String) -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    var showManual by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    LaunchedEffect(manualResult) { if (manualResult == true) showManual = false }
    ShareApkBottomSheet(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(s.pickerTitle, style = MaterialTheme.typography.titleLarge, color = c.onBackground)
                LtrBox {
                    Text(s.itemsSummary.format(itemsCount, FileNameAndHashUtils.formatBytes(itemsBytes)), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
            }
            IconActionButton(Icons.Outlined.Refresh, s.refreshAction, onRefresh, Modifier.testTag("send_refresh"))
        }
        if (sendState !is SendState.Idle) SendStatusCard(sendState, onCancel, onReset)
        if (scanning) GradientProgressBar(progress = null, height = 4.dp)
        Column(
            Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ShareApkTokens.CardGap)
        ) {
            if (peers.isEmpty()) {
                Text(
                    if (scanning) s.scanningNetwork else s.nearbyEmpty,
                    style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
            }
            peers.forEach { peer -> DeviceRow(peer, enabled = itemsCount > 0 && sendState !is SendState.Sending) { onSendTo(peer) } }
        }
        if (showManual) {
            LtrBox { TextInputField(address, { address = it.trim() }, s.manualAddressHint) }
            if (manualResult == false) StatusBadge(s.manualAddressNotFound, BadgeTone.Warning)
            PrimaryButton(s.addBtn, { onAddManual(address) }, Modifier.fillMaxWidth(), enabled = address.isNotBlank())
        } else {
            SecondaryButton(s.manualAddressBtn, { showManual = true }, Modifier.fillMaxWidth())
        }
        Text(s.pickerTip, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
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
            is SendState.Failed -> ResultRow(Icons.Outlined.WarningAmber, ShareApkGradients.rose, s.sendFailed.format(state.peer.info.alias, s.errorReasons.getValue(com.example.util.ErrorClassifier.classify(state.message))), onReset)
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

