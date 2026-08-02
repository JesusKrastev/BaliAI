package com.jesuskrastev.bali.data.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class AnalyticsTracker @Inject constructor(
    private val firebase: FirebaseAnalytics,
    private val mixpanel: MixpanelAPI
) {

    private fun log(event: String, params: Bundle.() -> Unit = {}) {
        val bundle = Bundle().apply(params)
        firebase.logEvent(event, bundle)
        mixpanel.track(event, bundleToJson(bundle))
    }

    private fun bundleToJson(bundle: Bundle): JSONObject {
        val json = JSONObject()
        for (key in bundle.keySet()) {
            val value = bundle.get(key)
            if (value != null) {
                try {
                    json.put(key, value)
                } catch (_: Exception) {}
            }
        }
        return json
    }

    // ── USERS ───────────────────────────────────────────────────────────────

    /** Identifies the user in both Firebase and Mixpanel and opts them into tracking. */
    open fun identifyUser(userId: String, email: String? = null) {
        firebase.setUserId(userId)
        mixpanel.identify(userId)
        mixpanel.optInTracking()
        email?.let { mixpanel.people.set("\$email", it) }
    }

    /** Resets analytics identity on sign-out. */
    open fun resetUser() {
        firebase.setUserId(null)
        mixpanel.reset()
    }

    // ── AUTH ────────────────────────────────────────────────────────────────

    /** Tracks a new account creation with the given auth method. */
    open fun signUp(method: String) = log("sign_up") { putString("method", method) }

    /** Tracks a sign-in with the given auth method. */
    open fun login(method: String) = log("login") { putString("method", method) }

    /** Tracks a sign-out. */
    open fun logout() = log("logout")

    // ── ONBOARDING ──────────────────────────────────────────────────────────

    /** Tracks that the user started the onboarding flow. */
    open fun onboardingStarted() = log("onboarding_started")

    /**
     * Tracks that a step was *shown* to the user. Each step owns its event name via
     * `OnboardingStep.analyticsName`, so the funnel stays one event per screen and
     * adding a screen needs no change here.
     *
     * Arrival rather than completion is what makes the counts a funnel: somebody who
     * lands on a screen and quits has still reached it, and has to be counted there.
     *
     * @param eventName the step's analytics name; blank names are ignored so that
     *   terminal states outside the funnel never emit an event
     */
    open fun onboardingStepReached(eventName: String) {
        if (eventName.isNotBlank()) log(eventName)
    }

    /** Tracks that the user tapped the "rate the app" button on the SocialProof step. */
    open fun onboardingRateAppClicked() = log("onboarding_rate_app_clicked")

    /** Tracks that the user finished the full onboarding flow and flushes immediately. */
    open fun onboardingCompleted() {
        log("onboarding_completed")
        mixpanel.flush()
    }

    /**
     * Tracks that the user left the onboarding flow before completing it.
     *
     * @param lastStep simple class name of the [OnboardingStep] where the user stopped
     * @param stepIndex zero-based position of that step
     */
    open fun onboardingAbandoned(lastStep: String, stepIndex: Int) = log("onboarding_abandoned") {
        putString("last_step", lastStep)
        putInt("step_index", stepIndex)
    }

    // ── PAYWALL ─────────────────────────────────────────────────────────────

    /**
     * Tracks that the paywall screen was displayed to the user.
     *
     * @param source entry point that triggered the paywall (e.g. "onboarding", "home")
     */
    open fun paywallShown(source: String = "onboarding") = log("paywall_shown") {
        putString("source", source)
    }

    /**
     * Tracks that the user left the paywall having bought. Flushes immediately.
     *
     * @param source entry point that triggered the paywall
     */
    open fun paywallPurchased(source: String = "onboarding") {
        log("paywall_purchased") { putString("source", source) }
        mixpanel.flush()
    }

    /**
     * Tracks that the user closed the paywall without buying. Flushes immediately.
     *
     * @param source entry point that triggered the paywall
     */
    open fun paywallClosed(source: String = "onboarding") {
        log("paywall_closed") { putString("source", source) }
        mixpanel.flush()
    }

    /**
     * Tracks that the paywall left the foreground with no decision taken — the user
     * pressed home, switched apps or killed the app while looking at it.
     *
     * Pair with [paywallResumed]: people who walked away for good are the backgrounded
     * count minus the resumed count. Counting the two separately is what keeps the
     * Google Play purchase sheet, which also backgrounds the app, from inflating it.
     *
     * Flushes immediately, because the process may not survive.
     *
     * @param source entry point that triggered the paywall
     */
    open fun paywallBackgrounded(source: String = "onboarding") {
        log("paywall_backgrounded") { putString("source", source) }
        mixpanel.flush()
    }

    /**
     * Tracks that the user came back to the paywall after backgrounding it.
     *
     * @param source entry point that triggered the paywall
     */
    open fun paywallResumed(source: String = "onboarding") = log("paywall_resumed") {
        putString("source", source)
    }
}
