package com.example.ui.screens.receive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.receive.IncomingRequest
import com.example.receive.VersionStatus
import com.example.receive.protocol.FileDto
import com.example.ui.components.design.BadgeTone
import com.example.ui.components.design.GradientIconBox
import com.example.ui.components.design.FileTypeIcon
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.SecondaryButton
import com.example.ui.components.design.ShareApkBottomSheet
import com.example.ui.components.design.StatusBadge
import com.example.ui.components.design.SurfaceCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens
import com.example.util.FileNameAndHashUtils

@Composable
fun VersionBadge(status: VersionStatus) {
    val s = LocalAppStrings.current
    when (status) {
        VersionStatus.NEW -> StatusBadge(s.vsNew, BadgeTone.Info)
        VersionStatus.UPDATE -> StatusBadge(s.vsUpdate, BadgeTone.Success, icon = Icons.Outlined.ArrowUpward)
        VersionStatus.SAME -> StatusBadge(s.vsSame, BadgeTone.Primary)
        VersionStatus.OLDER -> StatusBadge(s.vsOlder, BadgeTone.Warning, icon = Icons.Outlined.WarningAmber)
    }
}

/** Accept/decline sheet for an incoming LocalSend request. Dismissing declines. */
@Composable
fun IncomingRequestSheet(
    request: IncomingRequest,
    statusFor: (FileDto) -> VersionStatus?,
    onDecide: (Boolean) -> Unit
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val statuses = request.files.associateWith(statusFor)
    ShareApkBottomSheet(onDismiss = { onDecide(false) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientIconBox(ShareApkGradients.sky) { Icon(Icons.Outlined.PhoneAndroid, null, Modifier.size(24.dp)) }
            Column(Modifier.weight(1f)) {
                Text(
                    s.incomingTitle.format(request.sender.alias, request.files.size + request.messages.size),
                    style = MaterialTheme.typography.titleMedium, color = c.onBackground
                )
                LtrBox {
                    Text(FileNameAndHashUtils.formatBytes(request.files.sumOf { it.size }), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
            }
        }
        request.files.forEach { f ->
            SurfaceCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FileTypeIcon(f.fileName, f.fileType)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(f.fileName, style = MaterialTheme.typography.titleSmall, color = c.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        f.shareApk?.versionName?.let { v ->
                            LtrBox { Text("v$v", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant) }
                        }
                    }
                    statuses[f]?.let { VersionBadge(it) }
                }
            }
        }
        if (statuses.values.any { it == VersionStatus.OLDER }) {
            StatusBadge(s.incomingOlderWarning, BadgeTone.Warning, icon = Icons.Outlined.WarningAmber)
        }
        request.messages.forEach { msg ->
            SurfaceCard(Modifier.fillMaxWidth()) {
                Text(msg, style = MaterialTheme.typography.bodyMedium, color = c.onBackground, maxLines = 8, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(s.rejectBtn, { onDecide(false) }, Modifier.weight(1f), height = ShareApkTokens.SheetButtonHeight)
            PrimaryButton(s.acceptReceiveBtn, { onDecide(true) }, Modifier.weight(2f), height = ShareApkTokens.SheetButtonHeight)
        }
    }
}
