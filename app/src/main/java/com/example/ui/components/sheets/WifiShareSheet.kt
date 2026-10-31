package com.example.ui.components.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.ui.components.copyToClipboard
import com.example.ui.components.design.BadgeTone
import com.example.ui.components.design.GradientIconBox
import com.example.ui.components.design.IconActionButton
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.ShareApkBottomSheet
import com.example.ui.components.design.StatusBadge
import com.example.ui.components.design.SurfaceCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens
import com.example.wifi.WifiServerState

/** Share-via-link sheet: QR, URL, auto-stop countdown and download count. Dismissing stops the server. */
@Composable
fun WifiShareServerDialog(state: WifiServerState, onStopServer: () -> Unit) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val context = LocalContext.current
    val countdown = "%02d:%02d".format(state.remainingSeconds / 60, state.remainingSeconds % 60)

    ShareApkBottomSheet(onDismiss = onStopServer) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientIconBox(ShareApkGradients.violet) { Icon(Icons.Outlined.Wifi, null, Modifier.size(24.dp)) }
            Column(Modifier.weight(1f)) {
                Text(s.wifiServerTitle, style = MaterialTheme.typography.titleMedium, color = c.onBackground)
                Text(s.wifiServerSubtitle, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
        }
        state.qrBitmap?.let { bmp ->
            val image = remember(bmp) { bmp.asImageBitmap() }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                // QR stays on white for reliable scanning in both themes.
                Box(Modifier.clip(RoundedCornerShape(ShareApkTokens.CardRadius)).background(Color.White).padding(12.dp)) {
                    Image(image, contentDescription = state.url, modifier = Modifier.size(196.dp))
                }
            }
        }
        SurfaceCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    LtrBox {
                        Text(state.url, style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace), color = c.primaryText)
                    }
                }
                IconActionButton(Icons.Outlined.ContentCopy, s.copyAction, {
                    copyToClipboard(context, "Wi-Fi Server URL", state.url, s.copiedToast)
                })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.autoStopCountdown, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, modifier = Modifier.weight(1f))
                LtrBox { StatusBadge(countdown, BadgeTone.Warning) }
            }
            Text(s.wifiDownloadsFormat.format(state.downloadCount), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        }
        PrimaryButton(s.stopServerBtn, onStopServer, Modifier.fillMaxWidth(), icon = Icons.Outlined.StopCircle, height = ShareApkTokens.SheetButtonHeight)
    }
}
