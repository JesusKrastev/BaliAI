package com.jesuskrastev.bali.data.analytics

import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import com.google.firebase.analytics.FirebaseAnalytics
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.posthog.PostHogInterface
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AnalyticsTrackerTest {

    private val firebase = mock<FirebaseAnalytics>()
    private val mixpanel = mock<MixpanelAPI>()
    private val posthog = mock<PostHogInterface>()
    private val tracker = AnalyticsTracker(firebase, mixpanel, posthog)
    private val environment = AnalyticsTracker.currentEnvironment()

    @Test
    fun `every event carries the environment it was sent from`() {
        val bundle = argumentCaptor<Bundle>()

        tracker.onboardingStarted()

        verify(firebase).logEvent(eq("onboarding_started"), bundle.capture())
        assertThat(bundle.firstValue.getString(AnalyticsTracker.KEY_ENVIRONMENT)).isEqualTo(environment)
    }

    @Test
    fun `a screen view goes to all three tools tagged with the environment`() {
        tracker.screenViewed("Home")

        verify(firebase).logEvent(eq(FirebaseAnalytics.Event.SCREEN_VIEW), any())
        verify(mixpanel).track(eq("screen_viewed"), any<JSONObject>())
        verify(posthog).screen("Home", mapOf(AnalyticsTracker.KEY_ENVIRONMENT to environment))
    }

    @Test
    fun `finishing the onboarding keeps the answers on every later event`() {
        tracker.onboardingFlowCompleted(mapOf("exam_timing" to "soon", "experience" to "first"))

        verify(posthog).register("exam_timing", "soon")
        verify(posthog).register("experience", "first")
        verify(mixpanel).registerSuperProperties(any())
    }

    @Test
    fun `finishing the onboarding without answers registers nothing`() {
        tracker.onboardingFlowCompleted()

        verify(posthog, never()).register(any(), any())
        verify(mixpanel, never()).registerSuperProperties(any())
    }

    @Test
    fun `signing out registers the environment again after the reset`() {
        tracker.resetUser()

        inOrder(posthog) {
            verify(posthog).reset()
            verify(posthog).register(AnalyticsTracker.KEY_ENVIRONMENT, environment)
        }
    }

    @Test
    fun `identifying a user opts Mixpanel in`() {
        tracker.identifyUser("uid_1")

        verify(firebase).setUserId("uid_1")
        verify(mixpanel).identify("uid_1")
        verify(mixpanel).optInTracking()
    }

    @Test
    fun `identifying a user never turns Mixpanel back on while PostHog is opted out`() {
        // PostHog's opt-out marks a silenced build (Firebase Test Lab, Robolectric).
        whenever(posthog.isOptOut()).thenReturn(true)

        tracker.identifyUser("uid_1")

        verify(mixpanel).identify("uid_1")
        verify(mixpanel, never()).optInTracking()
    }

    @Test
    fun `a finished test reports its accuracy and the kind of node it came from`() {
        val bundle = argumentCaptor<Bundle>()

        tracker.testCompleted(
            score = 8,
            total = 10,
            accuracy = 80,
            durationSeconds = 95,
            xpGained = 12,
            nodeType = "LESSON",
            isRepeat = false
        )

        verify(firebase).logEvent(eq("test_completed"), bundle.capture())
        verify(mixpanel).track(eq("test_completed"), any<JSONObject>())
        with(bundle.firstValue) {
            assertThat(getInt("score")).isEqualTo(8)
            assertThat(getInt("total_questions")).isEqualTo(10)
            assertThat(getInt("accuracy")).isEqualTo(80)
            assertThat(getInt("duration_seconds")).isEqualTo(95)
            assertThat(getInt("xp_gained")).isEqualTo(12)
            assertThat(getString("node_type")).isEqualTo("LESSON")
            assertThat(getBoolean("is_repeat")).isFalse()
        }
    }

    @Test
    fun `a finished test outside the learning path carries no node type`() {
        val bundle = argumentCaptor<Bundle>()

        tracker.testCompleted(7, 10, 70, 60, 5, nodeType = null, isRepeat = false)

        verify(firebase).logEvent(eq("test_completed"), bundle.capture())
        assertThat(bundle.firstValue.containsKey("node_type")).isFalse()
    }

    @Test
    fun `a finished node tells an attempt apart from a real advance`() {
        val bundle = argumentCaptor<Bundle>()

        tracker.nodeCompleted(
            nodeId = "lesson_3",
            nodeType = "REVIEW",
            accuracy = 60,
            passed = false,
            unlockedNext = false,
            isRepeat = true
        )

        verify(firebase).logEvent(eq("node_completed"), bundle.capture())
        with(bundle.firstValue) {
            assertThat(getString("node_id")).isEqualTo("lesson_3")
            assertThat(getString("node_type")).isEqualTo("REVIEW")
            assertThat(getInt("accuracy")).isEqualTo(60)
            assertThat(getBoolean("passed")).isFalse()
            assertThat(getBoolean("unlocked_next")).isFalse()
            assertThat(getBoolean("is_repeat")).isTrue()
        }
    }

    @Test
    fun `a finished exam reports whether it was passed and which attempt it was`() {
        val bundle = argumentCaptor<Bundle>()

        tracker.examCompleted(
            score = 27,
            total = 30,
            accuracy = 90,
            durationSeconds = 1400,
            passed = true,
            xpGained = 40,
            attemptNumber = 2
        )

        verify(firebase).logEvent(eq("exam_completed"), bundle.capture())
        with(bundle.firstValue) {
            assertThat(getInt("score")).isEqualTo(27)
            assertThat(getBoolean("passed")).isTrue()
            assertThat(getInt("attempt_number")).isEqualTo(2)
        }
    }

    @Test
    fun `passing the exam is reported as its own event`() {
        val bundle = argumentCaptor<Bundle>()

        tracker.examPassed(score = 28, total = 30, durationSeconds = 1300, attemptNumber = 3)

        verify(firebase).logEvent(eq("exam_passed"), bundle.capture())
        verify(mixpanel).track(eq("exam_passed"), any<JSONObject>())
        with(bundle.firstValue) {
            assertThat(getInt("score")).isEqualTo(28)
            assertThat(getInt("attempt_number")).isEqualTo(3)
        }
    }

    @Test
    fun `failing the exam reports the mistakes and whether time ran out`() {
        val bundle = argumentCaptor<Bundle>()

        tracker.examFailed(
            score = 22,
            total = 30,
            mistakes = 8,
            durationSeconds = 1800,
            timeRanOut = true,
            attemptNumber = 2
        )

        verify(firebase).logEvent(eq("exam_failed"), bundle.capture())
        verify(mixpanel).track(eq("exam_failed"), any<JSONObject>())
        with(bundle.firstValue) {
            assertThat(getInt("score")).isEqualTo(22)
            assertThat(getInt("mistakes")).isEqualTo(8)
            assertThat(getInt("duration_seconds")).isEqualTo(1800)
            assertThat(getBoolean("time_ran_out")).isTrue()
            assertThat(getInt("attempt_number")).isEqualTo(2)
        }
    }

    @Test
    fun `an exam failed before the clock ran out is not flagged as a timeout`() {
        val bundle = argumentCaptor<Bundle>()

        tracker.examFailed(
            score = 25,
            total = 30,
            mistakes = 5,
            durationSeconds = 1200,
            timeRanOut = false,
            attemptNumber = 1
        )

        verify(firebase).logEvent(eq("exam_failed"), bundle.capture())
        assertThat(bundle.firstValue.getBoolean("time_ran_out")).isFalse()
    }
}
