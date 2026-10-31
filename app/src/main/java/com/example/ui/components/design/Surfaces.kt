package com.example.ui.components.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GradientPair
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens

/** Plain surface card with a hairline border. */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val c = ShareApkTheme.colors
    val shape = RoundedCornerShape(ShareApkTokens.CardRadius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(c.surface)
            .border(ShareApkTokens.Hairline, c.outline, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content
    )
}

/** Prominent gradient card (indigo → violet) with white content. */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(ShareApkTokens.HeroRadius)
    Row(
        modifier = modifier
            .shadow(14.dp, shape, ambientColor = ShareApkGradients.heroShadow, spotColor = ShareApkGradients.heroShadow)
            .clip(shape)
            .background(ShareApkGradients.hero)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) { content() }
    }
}

/** Fully coloured tile used on the More hub. */
@Composable
fun GradientTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradient: GradientPair,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badge: String? = null
) {
    val shape = RoundedCornerShape(ShareApkTokens.CardRadius)
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.6f)
            .shadow(12.dp, shape, ambientColor = gradient.end, spotColor = gradient.end)
            .clip(shape)
            .background(gradient.brush())
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 126.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(ShareApkTokens.SmallRadius))
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(ShareApkTokens.SmallRadius)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.weight(1f))
                if (badge != null) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.88f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Rounded gradient square holding an icon or glyph. */
@Composable
fun GradientIconBox(
    gradient: GradientPair,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(ShareApkTokens.SmallRadius)
    Box(
        modifier = modifier
            .size(size)
            .shadow(8.dp, shape, ambientColor = gradient.end, spotColor = gradient.end)
            .clip(shape)
            .background(gradient.brush()),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) { content() }
    }
}

/** Deterministic palette index for a label, so an app keeps its colour across sessions. */
fun letterGradientIndex(label: String): Int = label.trim().hashCode().mod(ShareApkGradients.tiles.size)

/** Fallback app icon: first letter of the label on a deterministic gradient. */
@Composable
fun LetterIconBox(label: String, modifier: Modifier = Modifier, size: Dp = 46.dp) {
    val letter = label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    GradientIconBox(ShareApkGradients.tiles[letterGradientIndex(label)], modifier, size) {
        Text(letter, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.4f).sp)
    }
}

/** Screen background: base colour plus two soft radial halos (indigo top-end, sky mid-start). */
@Composable
fun HaloBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = ShareApkTheme.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.background)
            .drawWithCache {
                fun x(f: Float) = if (rtl) size.width * (1f - f) else size.width * f
                val a = Brush.radialGradient(
                    colors = listOf(c.haloA, Color.Transparent),
                    center = Offset(x(0.95f), -size.height * 0.04f),
                    radius = size.width * 1.1f
                )
                val b = Brush.radialGradient(
                    colors = listOf(c.haloB, Color.Transparent),
                    center = Offset(x(-0.12f), size.height * 0.4f),
                    radius = size.width * 1.0f
                )
                onDrawBehind { drawRect(a); drawRect(b) }
            }
    ) { content() }
}

/** The app's brand mark: launcher glyph on the sky → indigo → violet brand gradient. */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 46.dp) {
    val shape = RoundedCornerShape(size * 0.32f)
    Box(
        modifier = modifier
            .size(size)
            .shadow(10.dp, shape, ambientColor = ShareApkGradients.heroShadow, spotColor = ShareApkGradients.heroShadow)
            .clip(shape)
            .background(ShareApkGradients.brand),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            androidx.compose.ui.res.painterResource(com.example.R.drawable.ic_brand_glyph),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.58f)
        )
    }
}
