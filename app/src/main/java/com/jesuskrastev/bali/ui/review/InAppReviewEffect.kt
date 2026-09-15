package com.jesuskrastev.bali.ui.review

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.delay

internal const val EXCELLENT_RESULT_ACCURACY = 90
private const val REVIEW_PROMPT_DELAY_MILLIS = 1_200L

/**
 * Requests Google Play's in-app review flow after the user reaches an excellent result.
 *
 * Google Play applies its own quota and may intentionally decide not to display the prompt.
 *
 * @param accuracy percentage of correctly answered questions
 */
@Composable
fun InAppReviewEffect(accuracy: Int) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(accuracy, activity) {
        if (!shouldRequestInAppReview(accuracy) || activity == null) return@LaunchedEffect

        delay(REVIEW_PROMPT_DELAY_MILLIS)
        if (activity.isFinishing || activity.isDestroyed) return@LaunchedEffect

        val reviewManager = ReviewManagerFactory.create(context)
        reviewManager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful && !activity.isFinishing && !activity.isDestroyed) {
                reviewManager.launchReviewFlow(activity, request.result)
            }
        }
    }
}

/**
 * Determines whether a score represents the success peak used for asking for a review.
 *
 * @param accuracy percentage of correctly answered questions
 * @return true when the result is excellent enough to trigger the Play review flow
 */
internal fun shouldRequestInAppReview(accuracy: Int): Boolean =
    accuracy >= EXCELLENT_RESULT_ACCURACY

/**
 * Unwraps a Compose context until its hosting activity is found.
 *
 * @return the hosting activity, or null when the context is not activity-backed
 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
