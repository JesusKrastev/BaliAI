package com.jesuskrastev.bali.data.analytics

import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import com.google.firebase.analytics.FirebaseAnalytics
import com.jesuskrastev.bali.domain.model.FirstStepReward
import com.jesuskrastev.bali.domain.model.FirstStepTask
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
    fun `a first-step reward is tracked with its task and coins`() {
        val bundle = argumentCaptor<Bundle>()

        tracker.firstStepRewarded(FirstStepReward(FirstStepTask.ASK_BALI, coins = 20, completedAll = false))

        verify(firebase).logEvent(eq("first_steps_task_completed"), bundle.capture())
        assertThat(bundle.firstValue.getString("task")).isEqualTo("ask_bali")
        assertThat(bundle.firstValue.getInt("coins")).isEqualTo(20)
        verify(firebase, never()).logEvent(eq("first_steps_completed"), any())
    }

    @Test
    fun `the last first step also sends the completion event`() {
        tracker.firstStepRewarded(FirstStepReward(FirstStepTask.PLAY_GAME, coins = 50, completedAll = true))

        verify(firebase).logEvent(eq("first_steps_task_completed"), any())
        verify(firebase).logEvent(eq("first_steps_completed"), any())
    }

    @Test
    fun `a cancellation reason is sent with the chosen option before the user leaves for Play`() {
        val bundle = argumentCaptor<Bundle>()

        tracker.subscriptionCancelReason("too_expensive")

        verify(firebase).logEvent(eq("subscription_cancel_reason"), bundle.capture())
        assertThat(bundle.firstValue.getString("reason")).isEqualTo("too_expensive")
        verify(mixpanel).flush()
        verify(posthog).flush()
    }

    @Test
    fun `the readiness view carries the verdict and the shown percentage`() {
        val bundle = argumentCaptor<Bundle>()

        tracker.readinessViewed(level = "ALMOST", mocksTaken = 4, passPercent = 76)

        verify(firebase).logEvent(eq("readiness_viewed"), bundle.capture())
        assertThat(bundle.firstValue.getString("level")).isEqualTo("ALMOST")
        assertThat(bundle.firstValue.getInt("mocks_taken")).isEqualTo(4)
        assertThat(bundle.firstValue.getInt("pass_percent")).isEqualTo(76)
    }

    @Test
    fun `the readiness view omits the percentage when none was shown`() {
        val bundle = argumentCaptor<Bundle>()

        tracker.readinessViewed(level = "NOT_ENOUGH_DATA", mocksTaken = 1, passPercent = null)

        verify(firebase).logEvent(eq("readiness_viewed"), bundle.capture())
        assertThat(bundle.firstValue.containsKey("pass_percent")).isFalse()
    }
}
