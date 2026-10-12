package com.jesuskrastev.bali.ui.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf

internal const val EXCELLENT_RESULT_ACCURACY = 90

/**
 * Raises the Home feedback banner. Provided once above the navigation graph; screens rendered
 * without it (previews, screenshot tests) get a no-op.
 */
val LocalFeedbackPromptRaiser = compositionLocalOf<() -> Unit> { {} }

/**
 * Asks Home to invite the student to share their opinion after an excellent result. It replaces
 * Google Play's in-app review dialog: the invitation is the green WhatsApp banner on Home.
 *
 * @param accuracy percentage of correctly answered questions
 * @param enabled false holds the request back, e.g. while a celebration is still on screen
 */
@Composable
fun InAppReviewEffect(accuracy: Int, enabled: Boolean = true) {
    val raiseBanner = LocalFeedbackPromptRaiser.current

    LaunchedEffect(accuracy, enabled) {
        if (enabled && shouldRequestInAppReview(accuracy)) raiseBanner()
    }
}

/**
 * Determines whether a score represents the success peak used for asking for an opinion.
 *
 * @param accuracy percentage of correctly answered questions
 * @return true when the result is excellent enough to raise the feedback banner
 */
internal fun shouldRequestInAppReview(accuracy: Int): Boolean =
    accuracy >= EXCELLENT_RESULT_ACCURACY
