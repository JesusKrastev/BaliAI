package com.jesuskrastev.bali.data.analytics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.ui.screens.auth.FakeAnalyticsTracker
import com.jesuskrastev.bali.ui.screens.auth.StreakChangeEvent
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers [StreakChangeTracker]: the streak is computed by a weekly Cloud Function, so the app
 * can only compare the value it sees now with the last one it saw on this device.
 */
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class StreakChangeTrackerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val analytics = FakeAnalyticsTracker(mock(), mock(), mock())
    private val tracker = StreakChangeTracker(context, analytics)

    @Test
    fun `the first value seen for an account is a baseline and reports nothing`() {
        tracker.onStreakObserved("uid_1", 3)

        assertThat(analytics.streakExtendedEvents).isEmpty()
        assertThat(analytics.streakBrokenEvents).isEmpty()
    }

    @Test
    fun `a longer streak than last time is reported as extended`() {
        tracker.onStreakObserved("uid_1", 2)

        tracker.onStreakObserved("uid_1", 3)

        assertThat(analytics.streakExtendedEvents).containsExactly(StreakChangeEvent(previousWeeks = 2, weeks = 3))
        assertThat(analytics.streakBrokenEvents).isEmpty()
    }

    @Test
    fun `a streak starting from zero is reported as extended`() {
        tracker.onStreakObserved("uid_1", 0)

        tracker.onStreakObserved("uid_1", 1)

        assertThat(analytics.streakExtendedEvents).containsExactly(StreakChangeEvent(previousWeeks = 0, weeks = 1))
    }

    @Test
    fun `a shorter streak than last time is reported as broken`() {
        tracker.onStreakObserved("uid_1", 5)

        tracker.onStreakObserved("uid_1", 0)

        assertThat(analytics.streakBrokenEvents).containsExactly(StreakChangeEvent(previousWeeks = 5, weeks = 0))
        assertThat(analytics.streakExtendedEvents).isEmpty()
    }

    @Test
    fun `seeing the same value again reports nothing`() {
        tracker.onStreakObserved("uid_1", 4)

        tracker.onStreakObserved("uid_1", 4)
        tracker.onStreakObserved("uid_1", 4)

        assertThat(analytics.streakExtendedEvents).isEmpty()
        assertThat(analytics.streakBrokenEvents).isEmpty()
    }

    @Test
    fun `the last value seen survives the app being restarted`() {
        tracker.onStreakObserved("uid_1", 2)

        // A new instance stands for a new process: it has no memory but the stored preference.
        StreakChangeTracker(context, analytics).onStreakObserved("uid_1", 3)

        assertThat(analytics.streakExtendedEvents).containsExactly(StreakChangeEvent(previousWeeks = 2, weeks = 3))
    }

    @Test
    fun `two accounts on the same device are never compared with each other`() {
        tracker.onStreakObserved("uid_1", 6)

        tracker.onStreakObserved("uid_2", 1)

        assertThat(analytics.streakBrokenEvents).isEmpty()
        assertThat(analytics.streakExtendedEvents).isEmpty()
    }
}
