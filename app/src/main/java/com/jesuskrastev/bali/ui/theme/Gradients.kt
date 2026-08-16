package com.jesuskrastev.bali.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush

/**
 * Subtle top-down wash of the brand orange fading into the app background.
 *
 * Used by full-screen gamification surfaces (streak, celebrations) so they feel warmer than a
 * plain background while still following the active light/dark color scheme.
 *
 * @return A vertical [Brush] going from a tinted primary to the theme background.
 */
@Composable
@ReadOnlyComposable
fun BaliBackgroundGradient(): Brush = Brush.verticalGradient(
    colors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.background
    )
)
