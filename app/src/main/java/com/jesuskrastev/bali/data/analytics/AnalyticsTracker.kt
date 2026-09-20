package com.jesuskrastev.bali.data.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.posthog.PostHogInterface
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class AnalyticsTracker @Inject constructor(
    private val firebase: FirebaseAnalytics,
    private val mixpanel: MixpanelAPI,
    private val posthog: PostHogInterface
) {

    /** Sends [event] with the properties built by [params] to Firebase, Mixpanel and PostHog. */
    private fun log(event: String, params: Bundle.() -> Unit = {}) {
        val bundle = Bundle().apply(params)
        val properties = bundleToMap(bundle)
        firebase.logEvent(event, bundle)
        mixpanel.track(event, JSONObject(properties))
        posthog.capture(event = event, properties = properties)
    }

    /** Converts a params [Bundle] into a plain map so Mixpanel and PostHog can share it. */
    private fun bundleToMap(bundle: Bundle): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        for (key in bundle.keySet()) {
            val value = bundle.get(key)
            if (value != null) map[key] = value
        }
        return map
    }

    // ── USERS ───────────────────────────────────────────────────────────────

    /** Identifies the user in Firebase, Mixpanel and PostHog, and opts Mixpanel into tracking. */
    open fun identifyUser(userId: String, email: String? = null) {
        firebase.setUserId(userId)
        mixpanel.identify(userId)
        mixpanel.optInTracking()
        email?.let { mixpanel.people.set("\$email", it) }
        posthog.identify(distinctId = userId, userProperties = email?.let { mapOf("email" to it) })
    }

    /** Resets analytics identity on sign-out. */
    open fun resetUser() {
        firebase.setUserId(null)
        mixpanel.reset()
        posthog.reset()
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

    /** Tracks that the user finished the full onboarding flow and flushes immediately. */
    open fun onboardingCompleted() {
        log("onboarding_completed")
        mixpanel.flush()
        posthog.flush()
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
        posthog.flush()
    }

    /**
     * Tracks that the user closed the paywall without buying. Flushes immediately.
     *
     * @param source entry point that triggered the paywall
     */
    open fun paywallClosed(source: String = "onboarding") {
        log("paywall_closed") { putString("source", source) }
        mixpanel.flush()
        posthog.flush()
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
        posthog.flush()
    }

    /**
     * Tracks that the user came back to the paywall after backgrounding it.
     *
     * @param source entry point that triggered the paywall
     */
    open fun paywallResumed(source: String = "onboarding") = log("paywall_resumed") {
        putString("source", source)
    }

    // ── AI CHAT ─────────────────────────────────────────────────────────────

    /**
     * Tracks that the user opened the AI tutor chat.
     *
     * @param hasHistory true when the user is resuming an existing conversation, which
     *   separates first-time curiosity from people who actually came back to it
     */
    open fun chatOpened(hasHistory: Boolean) = log("chat_opened") {
        putBoolean("has_history", hasHistory)
    }

    /**
     * Tracks a question sent to the AI tutor.
     *
     * The question text is deliberately not sent — only its shape — so the funnel never
     * carries free-text the user typed.
     *
     * @param questionLength number of characters in the question
     * @param fromSuggestion true when the user tapped a suggested prompt instead of typing
     * @param turnIndex zero-based position of this question within the conversation
     */
    open fun chatMessageSent(questionLength: Int, fromSuggestion: Boolean, turnIndex: Int) =
        log("chat_message_sent") {
            putInt("question_length", questionLength)
            putBoolean("from_suggestion", fromSuggestion)
            putInt("turn_index", turnIndex)
        }

    /**
     * Tracks that the tutor failed to answer.
     *
     * @param reason short machine-readable cause, e.g. the exception's simple name
     */
    open fun chatMessageFailed(reason: String) = log("chat_message_failed") {
        putString("reason", reason)
    }

    /** Tracks that the user wiped their conversation with the AI tutor. */
    open fun chatCleared() = log("chat_cleared")

    // ── AI GENERATION (test / exam / mistakes) ─────────────────────────────

    /**
     * Tracks a practice-test generation call to Gemini.
     *
     * @param reason why the call happened: `"initial"` (first generation for this screen
     *   instance), `"process_restart"` (the ViewModel was recreated — typically because
     *   Android killed the process in the background — and found no restorable questions
     *   in [androidx.lifecycle.SavedStateHandle]), or `"retry"` (the user tapped retry)
     * @param inputTokens prompt tokens billed for the call, from the response's usage metadata
     * @param outputTokens response tokens billed for the call, from the response's usage metadata
     */
    open fun testGenerated(reason: String, inputTokens: Int, outputTokens: Int) =
        log("test_generated") {
            putString("reason", reason)
            putInt("input_tokens", inputTokens)
            putInt("output_tokens", outputTokens)
        }

    /**
     * Tracks a full exam generation call to Gemini.
     *
     * @param reason why the call happened: `"initial"`, `"process_restart"` or `"retry"` —
     *   see [testGenerated]
     * @param inputTokens prompt tokens billed for the call, from the response's usage metadata
     * @param outputTokens response tokens billed for the call, from the response's usage metadata
     */
    open fun examGenerated(reason: String, inputTokens: Int, outputTokens: Int) =
        log("exam_generated") {
            putString("reason", reason)
            putInt("input_tokens", inputTokens)
            putInt("output_tokens", outputTokens)
        }

    /**
     * Tracks a mistakes-review generation call to Gemini.
     *
     * @param reason why the call happened: `"initial"`, `"process_restart"` or `"retry"` —
     *   see [testGenerated]
     * @param inputTokens prompt tokens billed for the call, from the response's usage metadata
     * @param outputTokens response tokens billed for the call, from the response's usage metadata
     */
    open fun mistakesGenerated(reason: String, inputTokens: Int, outputTokens: Int) =
        log("mistakes_generated") {
            putString("reason", reason)
            putInt("input_tokens", inputTokens)
            putInt("output_tokens", outputTokens)
        }

    // ── MINI-GAMES ──────────────────────────────────────────────────────────

    /**
     * Tracks that the user started an arcade mini-game session.
     *
     * @param gameId the [com.jesuskrastev.bali.ui.screens.games.GameType] id being played
     */
    open fun gameStarted(gameId: String) = log("game_started") {
        putString("game_id", gameId)
    }

    /**
     * Tracks that a mini-game session finished all its rounds.
     *
     * @param gameId the game that was played
     * @param score number of rounds won
     * @param totalRounds total rounds in the session
     * @param durationSeconds time spent playing the session
     */
    open fun gameCompleted(gameId: String, score: Int, totalRounds: Int, durationSeconds: Int) =
        log("game_completed") {
            putString("game_id", gameId)
            putInt("score", score)
            putInt("total_rounds", totalRounds)
            putInt("duration_seconds", durationSeconds)
        }

    /**
     * Tracks that the user left a mini-game before finishing the session.
     *
     * @param gameId the game that was abandoned
     * @param roundIndex zero-based round the user was on when they left
     */
    open fun gameAbandoned(gameId: String, roundIndex: Int) = log("game_abandoned") {
        putString("game_id", gameId)
        putInt("round_index", roundIndex)
    }
}
