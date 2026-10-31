package com.example.ui.theme

import androidx.compose.ui.unit.dp

/** Dimension and motion tokens (spec §3.2). */
object ShareApkTokens {
    val CardRadius = 22.dp
    val HeroRadius = 24.dp
    val SmallRadius = 14.dp
    val ButtonRadius = 16.dp
    val SheetRadius = 28.dp
    val Hairline = 0.5.dp
    val ScreenPadding = 20.dp
    val CardGap = 10.dp
    val SectionGap = 16.dp
    val MinTouch = 44.dp
    val ButtonHeight = 48.dp
    val SheetButtonHeight = 52.dp
    val BottomBarHeight = 70.dp
    val BottomBarMargin = 12.dp
    /** Space content must leave at the bottom so the floating bar never covers it. */
    val BottomBarClearance = 96.dp
    const val MotionMs = 200
}
