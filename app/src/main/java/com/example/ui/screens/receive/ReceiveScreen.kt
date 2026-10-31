package com.example.ui.screens.receive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.design.GradientIconBox
import com.example.ui.components.design.HeroCard
import com.example.ui.components.design.ScreenScaffold
import com.example.ui.components.design.SurfaceCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens

/** Receive tab. Direct receiving arrives in phase 2. */
@Composable
fun ReceiveScreen(deviceName: String, modifier: Modifier = Modifier) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    ScreenScaffold(title = s.receiveTitle, subtitle = s.receiveSubtitle, modifier = modifier) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(ShareApkTokens.SectionGap)
        ) {
            HeroCard(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GradientIconBox(ShareApkGradients.green, size = 64.dp) { Icon(Icons.Outlined.Wifi, null, Modifier.size(28.dp)) }
                    Text(deviceName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            SurfaceCard(Modifier.fillMaxWidth()) {
                Text(
                    s.receiveComing,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
