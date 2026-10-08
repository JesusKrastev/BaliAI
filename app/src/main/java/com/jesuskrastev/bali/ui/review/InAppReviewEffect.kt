package com.jesuskrastev.bali.ui.review

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.review.ReviewManagerFactory
import com.jesuskrastev.bali.RobolectricDetector
import java.io.IOException
import kotlinx.coroutines.delay

private const val REVIEW_PROMPT_DELAY_MILLIS = 1_200L

/**
 * Counts a finished test, exam or game and, once the user has finished several and this result is a
 * good one, requests Google Play's in-app review flow (see [shouldRequestInAppReview]).
 *
 * Each result screen counts once, even across recompositions or rotation. Google Play applies its
 * own quota and may intentionally decide not to display the prompt.
 *
 * @param accuracy percentage of correctly answered questions (or resolved situations)
 * @param enabled false holds the count and the request back, e.g. while a celebration is still on
 *   screen; the delay starts once it turns true
 */
@Composable
fun InAppReviewEffect(accuracy: Int, enabled: Boolean = true) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var counted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(enabled, activity) {
        // Unit tests never ask for reviews; skipping also keeps DataStore writes from leaking
        // coroutines (and their uncaught errors) into the next test.
        if (!enabled || counted || activity == null || RobolectricDetector.isRobolectric()) return@LaunchedEffect
        counted = true

        val store = ReviewPromptStore(context.applicationContext)
        // A review prompt is never worth breaking the results screen: a storage error just skips it.
        val state = try {
            store.recordCompletion()
        } catch (e: IOException) {
            return@LaunchedEffect
        }
        if (!shouldRequestInAppReview(state, accuracy)) return@LaunchedEffect

        delay(REVIEW_PROMPT_DELAY_MILLIS)
        if (activity.isFinishing || activity.isDestroyed) return@LaunchedEffect

        try {
            store.markRequested(state)
        } catch (e: IOException) {
            return@LaunchedEffect
        }
        val reviewManager = ReviewManagerFactory.create(context)
        reviewManager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful && !activity.isFinishing && !activity.isDestroyed) {
                reviewManager.launchReviewFlow(activity, request.result)
            }
        }
    }
}

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
