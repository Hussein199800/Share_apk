package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalShareApkColors = staticCompositionLocalOf { DarkShareApkColors }

/** Access point for ShareAPK colour roles inside composables. */
object ShareApkTheme {
    val colors: ShareApkColors
        @Composable get() = LocalShareApkColors.current
}

internal fun ShareApkColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = surfaceElevated,
        onPrimaryContainer = onBackground,
        secondary = primaryText,
        onSecondary = background,
        secondaryContainer = surfaceElevated,
        onSecondaryContainer = onBackground,
        tertiary = info,
        onTertiary = background,
        tertiaryContainer = surfaceElevated,
        onTertiaryContainer = onBackground,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onBackground,
        surfaceVariant = surfaceElevated,
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainerLowest = background,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceElevated,
        surfaceContainerHighest = surfaceElevated,
        outline = onSurfaceVariant.copy(alpha = 0.5f),
        outlineVariant = outline,
        error = error,
        onError = Color.White
    )
}

@Composable
fun ShareAPKTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkShareApkColors else LightShareApkColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalShareApkColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = Typography,
            content = content
        )
    }
}
