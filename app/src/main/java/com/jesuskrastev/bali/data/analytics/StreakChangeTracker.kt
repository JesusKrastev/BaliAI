package com.jesuskrastev.bali.data.analytics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns changes of the weekly streak into `streak_extended` / `streak_broken` events.
 *
 * The streak is not computed in the app: a weekly Cloud Function evaluates it, usually while the
 * app is closed. So the app never sees the moment it changes, only a new value the next time it
 * reads the user. To tell a change from a first look, the last value seen is kept per account on
 * this device, and every observation is compared against it.
 *
 * The first value ever seen for an account is only stored as the baseline and reports nothing:
 * without a previous value, "grew" and "was lost" cannot be told apart from "the app was just
 * installed".
 */
@Singleton
class StreakChangeTracker @Inject constructor(
    @ApplicationContext context: Context,
    private val analytics: AnalyticsTracker
) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /**
     * Compares the streak the user's document reports now with the last one seen for that
     * account, reports the difference, and remembers the new value.
     *
     * @param userId Firebase uid of the signed-in account; the last value seen is kept per uid so
     *   two accounts on one device never get compared with each other
     * @param streakWeeks the current streak, in weeks, as stored on the user
     */
    fun onStreakObserved(userId: String, streakWeeks: Int) {
        val key = KEY_PREFIX + userId
        val previousWeeks = if (preferences.contains(key)) preferences.getInt(key, 0) else null
        if (previousWeeks == streakWeeks) return

        preferences.edit().putInt(key, streakWeeks).apply()

        when {
            previousWeeks == null -> Unit
            streakWeeks > previousWeeks -> analytics.streakExtended(streakWeeks, previousWeeks)
            else -> analytics.streakBroken(previousWeeks, streakWeeks)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "streak_change_tracker"
        const val KEY_PREFIX = "last_seen_streak_"
    }
}
