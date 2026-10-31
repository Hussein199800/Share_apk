package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** ShareAPK colour roles (spec §3.1). Hex values live only in ui/theme. */
@Immutable
data class ShareApkColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val input: Color,
    val outline: Color,
    val onBackground: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val primaryText: Color,
    val success: Color,
    val warning: Color,
    val info: Color,
    val error: Color,
    val haloA: Color,
    val haloB: Color,
    val isDark: Boolean
)

val DarkShareApkColors = ShareApkColors(
    background = Color(0xFF070D1D),
    surface = Color(0xFF111C38),
    surfaceElevated = Color(0xFF1A2648),
    input = Color(0xFF0A1328),
    outline = Color.White.copy(alpha = 0.08f),
    onBackground = Color(0xFFEEF2FF),
    onSurfaceVariant = Color(0xFF9AA6C8),
    primary = Color(0xFF4F46E5),
    primaryText = Color(0xFFA5B4FC),
    success = Color(0xFF34D399),
    warning = Color(0xFFFBBF24),
    info = Color(0xFF38BDF8),
    error = Color(0xFFF87171),
    haloA = Color(0xFF6366F1).copy(alpha = 0.24f),
    haloB = Color(0xFF38BDF8).copy(alpha = 0.12f),
    isDark = true
)

val LightShareApkColors = ShareApkColors(
    background = Color(0xFFEEF1F8),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF1F4FA),
    input = Color(0xFFFFFFFF),
    outline = Color(0xFF0F172A).copy(alpha = 0.09f),
    onBackground = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF5B6577),
    primary = Color(0xFF4F46E5),
    primaryText = Color(0xFF4338CA),
    success = Color(0xFF047857),
    warning = Color(0xFF9A4508),
    info = Color(0xFF0369A1),
    error = Color(0xFFB91C1C),
    haloA = Color(0xFF6366F1).copy(alpha = 0.16f),
    haloB = Color(0xFF38BDF8).copy(alpha = 0.12f),
    isDark = false
)

/** WCAG 2.x contrast ratio between two opaque colours. */
fun contrastRatio(fg: Color, bg: Color): Double {
    fun channel(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    fun luminance(c: Color) = 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
    val a = luminance(fg)
    val b = luminance(bg)
    return (max(a, b) + 0.05) / (min(a, b) + 0.05)
}
