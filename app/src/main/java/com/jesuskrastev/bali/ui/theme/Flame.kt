package com.jesuskrastev.bali.ui.theme

import androidx.compose.ui.graphics.Color

/** Amber at the tip of the app's flame (`streak_icon.xml`). */
val BaliFlameAmber = Color(0xFFFFB52C)

/**
 * The flame's own gradient, tip to base: amber, the brand orange and its darker shade. Used by
 * the celebrations that belong to the flame (answer runs, streak milestones), so they look like
 * the same fire as the icon.
 */
val BaliFlameColors = listOf(BaliFlameAmber, BaliPrimary, BaliPrimaryDark)
