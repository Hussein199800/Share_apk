package com.example.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.design.GradientTile
import com.example.ui.components.design.HeroCard
import com.example.ui.components.design.ScreenScaffold
import com.example.ui.components.design.SettingsRow
import com.example.ui.components.design.SurfaceCard
import com.example.ui.navigation.MoreDestination
import com.example.ui.theme.GradientPair
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens

private data class MoreTile(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val gradient: GradientPair,
    val enabled: Boolean = true,
    val badge: String? = null,
    val onClick: () -> Unit
)

@Composable
fun MoreScreen(
    deviceName: String,
    appVersion: String,
    onOpen: (MoreDestination) -> Unit,
    onInstallFromFile: () -> Unit,
    onShareViaLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val tiles = listOf(
        MoreTile(Icons.Outlined.Archive, s.tileBackupTitle, s.tileBackupSub, ShareApkGradients.green) { onOpen(MoreDestination.BACKUP) },
        MoreTile(Icons.Outlined.History, s.tileHistoryTitle, s.tileHistorySub, ShareApkGradients.sky) { onOpen(MoreDestination.HISTORY) },
        MoreTile(Icons.Outlined.InstallMobile, s.tileInstallTitle, s.tileInstallSub, ShareApkGradients.amber, onClick = onInstallFromFile),
        MoreTile(Icons.Outlined.Link, s.tileLinkTitle, s.tileLinkSub, ShareApkGradients.violet, onClick = onShareViaLink),
        MoreTile(Icons.Outlined.Settings, s.tileSettingsTitle, s.tileSettingsSub, ShareApkGradients.indigo) { onOpen(MoreDestination.SETTINGS) },
        MoreTile(Icons.Outlined.Sync, s.tileUpdatesTitle, s.tileUpdatesSub, ShareApkGradients.rose, enabled = false, badge = s.comingSoon) {}
    )
    ScreenScaffold(title = s.moreTitle, modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(ShareApkTokens.CardGap),
            verticalArrangement = Arrangement.spacedBy(ShareApkTokens.CardGap)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HeroCard(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(ShareApkTokens.SmallRadius))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.Smartphone, contentDescription = null, modifier = Modifier.size(26.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text(deviceName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(s.thisDeviceSub, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }
            items(tiles) { t ->
                GradientTile(
                    icon = t.icon, title = t.title, subtitle = t.subtitle, gradient = t.gradient,
                    enabled = t.enabled, badge = t.badge, onClick = t.onClick
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SurfaceCard(contentPadding = PaddingValues(0.dp)) {
                    SettingsRow(
                        icon = Icons.Outlined.Info,
                        title = s.aboutTitle,
                        onClick = { onOpen(MoreDestination.ABOUT) },
                        trailing = {
                            LtrBox {
                                Text(
                                    s.appVersionFormat.format(appVersion),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
