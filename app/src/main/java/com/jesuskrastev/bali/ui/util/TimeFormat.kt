package com.jesuskrastev.bali.ui.util

import java.util.Locale

/**
 * Formats a duration as a stopwatch reading.
 *
 * Digits are always the ASCII ones, whatever the phone's language, so the clock looks the same
 * everywhere.
 *
 * @param seconds the duration in whole seconds
 * @return the duration as `mm:ss`, e.g. `07:05`
 */
fun formatClock(seconds: Int): String =
    String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
