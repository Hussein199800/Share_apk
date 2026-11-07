package com.example.ui.components.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 1 on phones and portrait tablets, 2 in landscape: lists then flow into two balanced columns. */
val LocalLanes = compositionLocalOf { 1 }

/** Width the phone design is drawn at on a portrait tablet before scaling up. */
const val PHONE_LOGICAL_WIDTH = 560f

/**
 * How much to enlarge the phone design so a tablet shows it bigger, not stretched.
 * Portrait: screen width / 560 (1…1.6). Landscape: each of the two columns ≈ a phone (1…1.6).
 */
fun screenScale(widthDp: Float, heightDp: Float): Float {
    val landscape = widthDp > heightDp
    val raw = if (landscape) widthDp / (2 * PHONE_LOGICAL_WIDTH) else widthDp / PHONE_LOGICAL_WIDTH
    return raw.coerceIn(1f, 1.6f)
}

/** Two columns when, after scaling, the window is landscape and wide enough for two phone-sized columns. */
fun lanesFor(logicalWidthDp: Float, logicalHeightDp: Float): Int =
    if (logicalWidthDp > logicalHeightDp && logicalWidthDp >= 600f) 2 else 1

/**
 * Applies the text-size setting and the tablet scale to everything inside by enlarging the
 * density uniformly (dp and sp alike), recomputed on rotation/resize without losing state.
 */
@Composable
fun AdaptiveScale(textScale: Float, content: @Composable (logicalWidth: Dp, lanes: Int) -> Unit) {
    BoxWithConstraints {
        val base = LocalDensity.current
        val w = maxWidth.value
        val h = maxHeight.value
        val k = screenScale(w, h) * textScale
        val lw = w / k
        val lh = h / k
        val lanes = lanesFor(lw, lh)
        CompositionLocalProvider(
            LocalDensity provides Density(base.density * k, base.fontScale),
            LocalLanes provides lanes
        ) {
            content(lw.dp, lanes)
        }
    }
}

/**
 * A screen's scrolling list: one column on phones, two balanced columns in landscape.
 * Use [fullLine] for headings that should span both columns.
 */
@Composable
fun AdaptiveList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    gap: Dp = 12.dp,
    content: LazyStaggeredGridScope.() -> Unit
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(LocalLanes.current),
        modifier = modifier,
        contentPadding = contentPadding,
        verticalItemSpacing = gap,
        horizontalArrangement = Arrangement.spacedBy(gap),
        content = content
    )
}

fun LazyStaggeredGridScope.fullLine(key: Any? = null, content: @Composable () -> Unit) =
    item(key = key, span = StaggeredGridItemSpan.FullLine) { content() }
