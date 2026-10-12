package com.jesuskrastev.bali.ui.review

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.jesuskrastev.bali.RobolectricDetector
import java.io.IOException
import kotlinx.coroutines.delay

private const val REVIEW_PROMPT_DELAY_MILLIS = 1_200L

/**
 * Raises the Home feedback banner. Provided once above the navigation graph; screens rendered
 * without it (previews, screenshot tests) get a no-op.
 */
val LocalFeedbackPromptRaiser = compositionLocalOf<() -> Unit> { {} }

/**
 * Counts a finished test, exam or game and, once the user has finished several and this result is a
 * good one (see [shouldRequestInAppReview]), raises the green WhatsApp banner on Home that invites
 * them to share their opinion. It replaces Google Play's in-app review dialog.
 *
 * Each result screen counts once, even across recompositions or rotation.
 *
 * @param accuracy percentage of correctly answered questions (or resolved situations)
 * @param enabled false holds the count and the request back, e.g. while a celebration is still on
 *   screen; the delay starts once it turns true
 */
@Composable
fun InAppReviewEffect(accuracy: Int, enabled: Boolean = true) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val raiseBanner = LocalFeedbackPromptRaiser.current
    var counted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(enabled, activity) {
        // Unit tests never raise the banner; skipping also keeps DataStore writes from leaking
        // coroutines (and their uncaught errors) into the next test.
        if (!enabled || counted || activity == null || RobolectricDetector.isRobolectric()) return@LaunchedEffect
        counted = true

        val store = ReviewPromptStore(context.applicationContext)
        // An invitation is never worth breaking the results screen: a storage error just skips it.
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
        raiseBanner()
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
