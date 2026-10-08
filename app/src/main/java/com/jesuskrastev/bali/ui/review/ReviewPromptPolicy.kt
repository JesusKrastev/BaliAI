package com.jesuskrastev.bali.ui.review

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/** Accuracy (or equivalent) from which a result counts as a happy moment worth asking in. */
internal const val GOOD_RESULT_ACCURACY = 70

/** Finished tests or games the user must have before the first request. */
internal const val MIN_COMPLETIONS_BEFORE_REVIEW = 3

/** Further finished tests or games required between two requests. */
internal const val COMPLETIONS_BETWEEN_REVIEW_REQUESTS = 5

/** Lifetime cap on requests; Google Play applies its own, stricter quota on top. */
internal const val MAX_REVIEW_REQUESTS = 3

/**
 * Device-local counters behind the review prompt.
 *
 * @property completions tests, exams and games finished so far
 * @property requests times the Play review flow was requested
 * @property completionsAtLastRequest value of [completions] when the last request was made
 */
internal data class ReviewPromptState(
    val completions: Int = 0,
    val requests: Int = 0,
    val completionsAtLastRequest: Int = 0,
)

/**
 * Decides whether this result is the moment to ask for a Play review: the user has finished enough
 * tests or games, is happy with this one, has not been asked too often and enough activity has
 * passed since the last request.
 *
 * @param state counters, already including the result being shown
 * @param accuracy percentage of correct answers (or resolved situations) of this result
 * @return true when the Play review flow should be requested
 */
internal fun shouldRequestInAppReview(state: ReviewPromptState, accuracy: Int): Boolean =
    accuracy >= GOOD_RESULT_ACCURACY &&
        state.completions >= MIN_COMPLETIONS_BEFORE_REVIEW &&
        state.requests < MAX_REVIEW_REQUESTS &&
        (state.requests == 0 ||
            state.completions - state.completionsAtLastRequest >= COMPLETIONS_BETWEEN_REVIEW_REQUESTS)

private val Context.reviewPromptDataStore by preferencesDataStore(name = "review_prompt")
private val completionsKey = intPreferencesKey("completions")
private val requestsKey = intPreferencesKey("requests")
private val atLastRequestKey = intPreferencesKey("completions_at_last_request")

/** Persists the [ReviewPromptState] counters in a small DataStore file of their own. */
internal class ReviewPromptStore(private val context: Context) {

    /**
     * Counts one more finished test or game.
     *
     * @return the counters after the increment
     */
    suspend fun recordCompletion(): ReviewPromptState {
        var updated = ReviewPromptState()
        context.reviewPromptDataStore.edit { preferences ->
            val completions = (preferences[completionsKey] ?: 0) + 1
            preferences[completionsKey] = completions
            updated = ReviewPromptState(completions, preferences[requestsKey] ?: 0, preferences[atLastRequestKey] ?: 0)
        }
        return updated
    }

    /**
     * Remembers that a review was requested at the current completion count.
     *
     * @param state counters returned by [recordCompletion] for the result that triggered the request
     */
    suspend fun markRequested(state: ReviewPromptState) {
        context.reviewPromptDataStore.edit { preferences ->
            preferences[requestsKey] = state.requests + 1
            preferences[atLastRequestKey] = state.completions
        }
    }

    /**
     * Reads the stored counters without changing them.
     *
     * @return current counters, zeroed when nothing was stored
     */
    suspend fun state(): ReviewPromptState {
        val preferences = context.reviewPromptDataStore.data.first()
        return ReviewPromptState(preferences[completionsKey] ?: 0, preferences[requestsKey] ?: 0, preferences[atLastRequestKey] ?: 0)
    }
}
