package com.example.ui.screens.send

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.WifiFind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.InstalledAppInfo
import com.example.ui.components.design.AppIcon
import com.example.ui.components.design.GradientIconBox
import com.example.ui.components.design.HeroCard
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.ScreenScaffold
import com.example.ui.components.design.SecondaryButton
import com.example.ui.components.design.SurfaceCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens
import com.example.util.FileNameAndHashUtils

/** Send tab. Direct discovery arrives in phase 3; for now it offers share-via-link. */
@Composable
fun SendScreen(
    selectedApps: List<InstalledAppInfo>,
    onPickApps: () -> Unit,
    onShareViaLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    ScreenScaffold(title = s.sendTitle, subtitle = s.sendSubtitle, modifier = modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(ShareApkTokens.SectionGap)
        ) {
            if (selectedApps.isEmpty()) {
                SurfaceCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GradientIconBox(ShareApkGradients.indigo) { Icon(Icons.Outlined.Apps, null, Modifier.size(24.dp)) }
                    Text(s.sendPickAppsTitle, style = MaterialTheme.typography.titleMedium, color = c.onBackground)
                    Text(s.sendPickAppsBody, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    PrimaryButton(s.sendPickAppsBtn, onPickApps, Modifier.fillMaxWidth())
                }
            } else {
                HeroCard(Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-14).dp)) {
                        selectedApps.take(3).forEach { AppIcon(it.label, it.iconBitmap, size = 40.dp) }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            selectedApps.joinToString("، ") { it.label },
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            s.sendSelectedSummary.format(
                                selectedApps.size,
                                FileNameAndHashUtils.formatBytes(selectedApps.sumOf { it.totalSizeBytes })
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Text(s.nearbyDevicesTitle, style = MaterialTheme.typography.titleMedium, color = c.onBackground)
            SurfaceCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    GradientIconBox(ShareApkGradients.sky, size = 56.dp) { Icon(Icons.Outlined.WifiFind, null, Modifier.size(28.dp)) }
                }
                Text(
                    s.sendDiscoveryComing,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                SecondaryButton(s.tileLinkTitle, onShareViaLink, Modifier.fillMaxWidth(), icon = Icons.Outlined.Link)
            }
        }
    }
}
