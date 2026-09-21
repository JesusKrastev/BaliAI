package com.jesuskrastev.bali.ui.util

import androidx.compose.ui.Modifier
import com.posthog.android.replay.PostHogMaskModifier

/**
 * Hides this element, and everything inside it, from PostHog session replay captures.
 *
 * Put it on anything that can show who the user is or what they typed — names, emails,
 * avatars, chat and free-text content. The masked pixels are painted over on the device, so
 * they never reach the recording.
 *
 * @param enabled when false the modifier does nothing, e.g. to mask a line only when it really
 *   carries personal data
 * @return this modifier with the replay-mask semantics applied
 */
fun Modifier.replayMask(enabled: Boolean = true): Modifier =
    with(PostHogMaskModifier) { this@replayMask.postHogMask(isEnabled = enabled) }
