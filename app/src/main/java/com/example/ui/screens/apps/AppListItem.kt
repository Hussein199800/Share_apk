package com.example.ui.screens.apps

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.InstalledAppInfo
import com.example.ui.components.design.AppIcon
import com.example.ui.components.design.BadgeTone
import com.example.ui.components.design.StatusBadge
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens
import com.example.util.FileNameAndHashUtils

private fun InstalledAppInfo.metaLine() = "v$versionName · ${FileNameAndHashUtils.formatBytes(totalSizeBytes)}"

@Composable
private fun itemModifier(
    app: InstalledAppInfo,
    isSelected: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
): Modifier {
    val c = ShareApkTheme.colors
    val shape = RoundedCornerShape(ShareApkTokens.CardRadius)
    val (width, color) = when {
        isSelected -> 1.5.dp to c.primaryText
        isFocused -> 1.dp to c.primaryText.copy(alpha = 0.55f)
        else -> ShareApkTokens.Hairline to c.outline
    }
    @OptIn(ExperimentalFoundationApi::class)
    return Modifier
        .fillMaxWidth()
        .testTag("app_card_${app.packageName}")
        .clip(shape)
        .background(c.surface)
        .border(width, color, shape)
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
}

/** Square check box with a 44dp touch target. */
@Composable
private fun SelectBox(isSelected: Boolean, onToggle: () -> Unit, label: String) {
    val c = ShareApkTheme.colors
    val shape = RoundedCornerShape(9.dp)
    Box(
        Modifier
            .size(ShareApkTokens.MinTouch)
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .semantics {
                contentDescription = label
                toggleableState = if (isSelected) ToggleableState.On else ToggleableState.Off
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(shape)
                .then(
                    if (isSelected) Modifier.background(ShareApkGradients.primary.brush(135f))
                    else Modifier.border(1.5.dp, c.onSurfaceVariant.copy(alpha = 0.45f), shape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun TitleRow(app: InstalledAppInfo) {
    val c = ShareApkTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = c.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (app.isFavorite) Icon(Icons.Filled.Star, null, tint = c.warning, modifier = Modifier.size(15.dp))
        if (app.isSplitApk) StatusBadge(LocalAppStrings.current.splitBadge, BadgeTone.Info)
    }
}

/** One app in list mode. */
@Composable
fun AppListItem(
    app: InstalledAppInfo,
    isSelected: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit,
    onToggleSelect: () -> Unit
) {
    val c = ShareApkTheme.colors
    Row(
        modifier = itemModifier(app, isSelected, isFocused, onClick, onToggleSelect).padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppIcon(app.label, app.iconBitmap)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            TitleRow(app)
            LtrBox {
                Text(app.metaLine(), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        SelectBox(isSelected, onToggleSelect, app.label)
    }
}

/** One app in grid mode. */
@Composable
fun AppGridCard(
    app: InstalledAppInfo,
    isSelected: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit,
    onToggleSelect: () -> Unit
) {
    val c = ShareApkTheme.colors
    Column(
        modifier = itemModifier(app, isSelected, isFocused, onClick, onToggleSelect).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            AppIcon(app.label, app.iconBitmap)
            Box(Modifier.weight(1f))
            SelectBox(isSelected, onToggleSelect, app.label)
        }
        TitleRow(app)
        LtrBox {
            Text(app.metaLine(), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
