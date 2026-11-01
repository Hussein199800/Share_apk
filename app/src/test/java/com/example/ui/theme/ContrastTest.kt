package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {
    private fun assertAA(fg: Color, bg: Color) {
        val ratio = contrastRatio(fg, bg)
        assertTrue("fg=$fg bg=$bg ratio=$ratio", ratio >= 4.5)
    }

    private fun ShareApkColors.textColors() =
        listOf(onBackground, onSurfaceVariant, primaryText, success, warning, info, error)

    @Test
    fun darkTextPairsPassAA() = with(DarkShareApkColors) {
        textColors().forEach { assertAA(it, surface); assertAA(it, background) }
    }

    @Test
    fun lightTextPairsPassAA() = with(LightShareApkColors) {
        textColors().forEach { assertAA(it, surface); assertAA(it, background) }
    }

    @Test
    fun whiteOnPrimaryButtonPassesAA() = assertAA(Color.White, ShareApkGradients.primary.end)

    @Test
    fun contrastOfBlackOnWhiteIs21() {
        val r = contrastRatio(Color.Black, Color.White)
        assertTrue("ratio=$r", r in 20.99..21.01)
    }
}
