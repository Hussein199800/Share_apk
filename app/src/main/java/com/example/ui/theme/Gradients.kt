package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Two-stop gradient (spec §3.1). `end` is the darker stop, used for shadows and contrast. */
@Immutable
data class GradientPair(val start: Color, val end: Color) {
    /** Linear brush at [angleDeg], CSS convention. */
    fun brush(angleDeg: Float = 145f): Brush = angledBrush(listOf(start, end), angleDeg)
}

/** CSS-style angled gradient (0° = to top, 90° = to right, 135° = to bottom-right) sized to the shape. */
internal fun angledBrush(colors: List<Color>, angleDeg: Float): Brush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val rad = angleDeg * PI.toFloat() / 180f
        val dx = sin(rad)
        val dy = -cos(rad)
        val half = (abs(size.width * dx) + abs(size.height * dy)) / 2f
        val c = size.center
        return LinearGradientShader(
            from = Offset(c.x - dx * half, c.y - dy * half),
            to = Offset(c.x + dx * half, c.y + dy * half),
            colors = colors
        )
    }
}

object ShareApkGradients {
    val primary = GradientPair(Color(0xFF6D72F3), Color(0xFF4338CA))
    val heroColors = listOf(Color(0xFF818CF8), Color(0xFF4F46E5), Color(0xFF7C3AED))
    val hero: Brush get() = angledBrush(heroColors, 135f)
    val heroShadow = Color(0xFF4F46E5)
    /** Brand mark / launcher icon gradient (sky → indigo → violet). */
    val brand: Brush = angledBrush(listOf(Color(0xFF38BDF8), Color(0xFF4F46E5), Color(0xFFA78BFA)), 135f)

    val green = GradientPair(Color(0xFF34D399), Color(0xFF047857))
    val sky = GradientPair(Color(0xFF38BDF8), Color(0xFF2563EB))
    val amber = GradientPair(Color(0xFFF59E0B), Color(0xFFC2410C))
    val violet = GradientPair(Color(0xFFA78BFA), Color(0xFF6D28D9))
    val indigo = GradientPair(Color(0xFF818CF8), Color(0xFF4338CA))
    val rose = GradientPair(Color(0xFFFB7185), Color(0xFFBE123C))
    val progress = GradientPair(Color(0xFF34D399), Color(0xFF38BDF8))

    /** Ordered palette for deterministic per-item colouring (e.g. letter icons). */
    val tiles = listOf(green, sky, amber, violet, indigo, rose)
}
