package com.example.ui.components.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme

enum class BadgeTone { Success, Warning, Info, Error, Primary }

@Composable
fun BadgeTone.color(): Color {
    val c = ShareApkTheme.colors
    return when (this) {
        BadgeTone.Success -> c.success
        BadgeTone.Warning -> c.warning
        BadgeTone.Info -> c.info
        BadgeTone.Error -> c.error
        BadgeTone.Primary -> c.primaryText
    }
}

@Composable
fun StatusBadge(
    text: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val fg = tone.color()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(fg.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(13.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1)
    }
}

/** Green→sky progress bar. `progress == null` renders an indeterminate sweep. */
@Composable
fun GradientProgressBar(progress: Float?, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val c = ShareApkTheme.colors
    val shape = RoundedCornerShape(height / 2)
    val track = Modifier
        .fillMaxWidth()
        .height(height)
        .clip(shape)
        .background(c.primary.copy(alpha = if (c.isDark) 0.22f else 0.10f))
    if (progress != null) {
        val p = progress.coerceIn(0f, 1f)
        Box(modifier.then(track).semantics { progressBarRangeInfo = ProgressBarRangeInfo(p, 0f..1f) }) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(p)
                    .clip(shape)
                    .background(ShareApkGradients.progress.brush(90f))
            )
        }
    } else {
        val t by rememberInfiniteTransition(label = "progress").animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
            label = "sweep"
        )
        Box(
            modifier.then(track)
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
                .drawBehind {
                    val w = size.width * 0.3f
                    drawRoundRect(
                        brush = ShareApkGradients.progress.brush(90f),
                        topLeft = Offset((size.width + w) * t - w, 0f),
                        size = Size(w, size.height),
                        cornerRadius = CornerRadius(size.height / 2)
                    )
                }
        )
    }
}
