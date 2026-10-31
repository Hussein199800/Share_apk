package com.example.ui.components.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ShareApkColors
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens

/** Translucent primary tint used behind icon buttons and soft chips. */
internal val ShareApkColors.primarySoft: Color
    get() = primary.copy(alpha = if (isDark) 0.22f else 0.10f)

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = ShareApkTokens.ButtonHeight
) {
    val shape = RoundedCornerShape(ShareApkTokens.ButtonRadius)
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(if (enabled) 8.dp else 0.dp, shape, spotColor = ShareApkGradients.primary.end)
            .clip(shape)
            .background(ShareApkGradients.primary.brush(135f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = height)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = ShareApkTokens.ButtonHeight
) {
    val c = ShareApkTheme.colors
    val shape = RoundedCornerShape(ShareApkTokens.ButtonRadius)
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .border(ShareApkTokens.Hairline, c.onSurfaceVariant.copy(alpha = 0.35f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = height)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = c.onBackground, modifier = Modifier.size(18.dp))
        Text(text, color = c.onBackground, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 44dp square icon button on a soft primary tint. */
@Composable
fun IconActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val c = ShareApkTheme.colors
    Box(
        modifier = modifier
            .size(ShareApkTokens.MinTouch)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(ShareApkTokens.SmallRadius))
            .background(c.primarySoft)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = c.primaryText, modifier = Modifier.size(20.dp))
    }
}
