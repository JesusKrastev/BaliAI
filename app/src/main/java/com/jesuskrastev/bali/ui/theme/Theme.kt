package com.jesuskrastev.bali.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BaliPrimary,
    onPrimary = White,
    primaryContainer = BaliPrimaryDark,
    onPrimaryContainer = White,
    secondary = BaliAccentYellow,
    onSecondary = BaliSecondary,
    tertiary = BaliAccentGreen,
    background = BaliDarkBackground,
    onBackground = BaliOnSecondary,
    surface = BaliDarkSurface,
    onSurface = BaliOnSecondary,
    surfaceVariant = BaliDarkGray,
    onSurfaceVariant = BaliGrayLight,
    outline = BaliGrayMedium,
    error = BaliAccentRed,
    onError = White
)

private val LightColorScheme = lightColorScheme(
    primary = BaliPrimary,
    onPrimary = White,
    primaryContainer = BaliPrimaryLight,
    onPrimaryContainer = White,
    secondary = BaliSecondary,
    onSecondary = White,
    tertiary = BaliAccentYellow,
    background = BaliBackground,
    onBackground = BaliSecondary,
    surface = BaliSurface,
    onSurface = BaliSecondary,
    surfaceVariant = BaliGrayLight,
    onSurfaceVariant = BaliGrayMedium,
    outline = BaliGrayLight,
    error = BaliAccentRed,
    onError = White
)

/**
 * Applies the Bali Material3 color scheme and typography. System bar appearance is not set
 * here: `MainActivity` calls `enableEdgeToEdge()`, which already follows the system light/dark
 * mode, and `Window.statusBarColor` is ignored once the app draws edge to edge.
 *
 * @param darkTheme whether to use the dark color scheme; follows the system by default
 * @param dynamicColor whether to use Material You colors on Android 12+ instead of the Bali palette
 * @param content UI that receives the theme
 */
@Composable
fun BaliTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
