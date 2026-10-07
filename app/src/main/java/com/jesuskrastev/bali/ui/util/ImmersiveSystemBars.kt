package com.jesuskrastev.bali.ui.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * System bars plus display cutout: the area an edge-to-edge screen keeps its controls out of.
 * Screens that paint under the bars apply this themselves instead of relying on a parent's padding.
 */
val WindowInsets.Companion.drawSafe: WindowInsets
    @Composable get() = WindowInsets.systemBars.union(WindowInsets.displayCutout)

/**
 * Keeps the status and navigation bar icons light while a dark, full-bleed screen is shown, and
 * restores the previous appearance when it leaves the composition.
 */
@Composable
fun LightSystemBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? android.app.Activity)?.window
        if (window == null) return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val wasLightStatus = controller.isAppearanceLightStatusBars
        val wasLightNav = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = wasLightStatus
            controller.isAppearanceLightNavigationBars = wasLightNav
        }
    }
}
