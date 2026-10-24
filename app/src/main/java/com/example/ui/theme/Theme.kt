package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WarmBrownDarkColorScheme = darkColorScheme(
    primary = BurnishedGold,
    onPrimary = DarkEspressoSurface,
    primaryContainer = PrimaryCocoaBrown,
    onPrimaryContainer = CreamTextOnDark,
    secondary = SecondaryWarmCinnamon,
    onSecondary = CreamTextOnDark,
    secondaryContainer = DarkElevatedCardSurface,
    onSecondaryContainer = WarmSandAccent,
    tertiary = BurnishedGoldLight,
    onTertiary = DarkEspressoSurface,
    tertiaryContainer = BurnishedGoldContainerDark,
    onTertiaryContainer = BurnishedGoldLight,
    background = DarkEspressoSurface,
    onBackground = CreamTextOnDark,
    surface = DarkEspressoSurface,
    onSurface = CreamTextOnDark,
    surfaceVariant = DarkElevatedCardSurface,
    onSurfaceVariant = MutedSandOnDark,
    surfaceContainer = DarkElevatedCardSurface,
    surfaceContainerHigh = PrimaryCocoaBrown,
    outline = WarmSandAccent.copy(alpha = 0.35f),
    outlineVariant = WarmSandAccent.copy(alpha = 0.18f),
    error = RubyErrorDark,
    onError = Color.White
)

private val WarmBrownLightColorScheme = lightColorScheme(
    primary = PrimaryCocoaBrown,
    onPrimary = Color.White,
    primaryContainer = BurnishedGoldContainerLight,
    onPrimaryContainer = DeepEspressoText,
    secondary = SecondaryWarmCinnamon,
    onSecondary = Color.White,
    secondaryContainer = LightCardSurface,
    onSecondaryContainer = DeepEspressoText,
    tertiary = BurnishedGold,
    onTertiary = DeepEspressoText,
    tertiaryContainer = BurnishedGoldContainerLight,
    onTertiaryContainer = DeepEspressoText,
    background = WarmCreamLightBackground,
    onBackground = DeepEspressoText,
    surface = WarmCreamLightBackground,
    onSurface = DeepEspressoText,
    surfaceVariant = LightCardSurface,
    onSurfaceVariant = MutedCocoaOnLight,
    surfaceContainer = LightCardSurface,
    surfaceContainerHigh = WarmSandAccent.copy(alpha = 0.45f),
    outline = WarmSandAccent,
    outlineVariant = WarmSandAccent.copy(alpha = 0.6f),
    error = RubyError,
    onError = Color.White
)

@Composable
fun ShareAPKTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        // Android 12+ (minSdk 31) supports Material You dynamic color
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> WarmBrownDarkColorScheme
        else -> WarmBrownLightColorScheme
    }

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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ShareAPKTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
