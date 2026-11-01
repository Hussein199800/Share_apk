package com.example.ui.components.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MainNavTab
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens

data class NavItem(val tab: MainNavTab, val label: String, val icon: ImageVector)

/** Floating tab bar (phones). */
@Composable
fun ShareApkBottomBar(
    items: List<NavItem>,
    current: MainNavTab,
    onSelect: (MainNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = ShareApkTheme.colors
    val shape = RoundedCornerShape(ShareApkTokens.HeroRadius)
    Row(
        modifier = modifier
            .padding(ShareApkTokens.BottomBarMargin)
            .fillMaxWidth()
            .height(ShareApkTokens.BottomBarHeight)
            .shadow(8.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
            .background(c.surface, shape)
            .border(ShareApkTokens.Hairline, c.outline, shape)
            .clip(shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            NavEntry(item, item.tab == current, onSelect, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

/** Side rail (tablets, unfolded devices). */
@Composable
fun ShareApkRail(
    items: List<NavItem>,
    current: MainNavTab,
    onSelect: (MainNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = ShareApkTheme.colors
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(92.dp)
            .background(c.surface.copy(alpha = 0.96f))
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            NavEntry(item, item.tab == current, onSelect, Modifier.fillMaxWidth().heightIn(min = 64.dp))
        }
    }
}

@Composable
private fun NavEntry(item: NavItem, active: Boolean, onSelect: (MainNavTab) -> Unit, modifier: Modifier) {
    val c = ShareApkTheme.colors
    Column(
        modifier = modifier
            .testTag("nav_${item.tab.name}")
            .selectable(selected = active, role = Role.Tab, onClick = { onSelect(item.tab) }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
    ) {
        val pill = RoundedCornerShape(15.dp)
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 30.dp)
                .then(
                    if (active) Modifier
                        .shadow(6.dp, pill, spotColor = ShareApkGradients.primary.end)
                        .clip(pill)
                        .background(ShareApkGradients.primary.brush(135f))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = if (active) Color.White else c.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = if (active) c.primaryText else c.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
