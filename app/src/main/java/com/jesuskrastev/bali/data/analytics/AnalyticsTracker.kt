package com.jesuskrastev.bali.data.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.jesuskrastev.bali.BuildConfig
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

    /**
     * Sends [event] with the properties built by [params] to Firebase, Mixpanel and PostHog.
     *
     * Every event carries an `environment` property (`"debug"` or `"production"`, from
     * [BuildConfig.DEBUG]) so manual testing on a debug build can be filtered out of the real
     * funnels later — no SDK here is opted out in debug the way it is under Robolectric, so
     * without this tag a developer's own run-through is indistinguishable from a real user.
     */
    private fun log(event: String, params: Bundle.() -> Unit = {}) {
        val bundle = Bundle().apply(params).apply {
            putString(KEY_ENVIRONMENT, currentEnvironment())
        }
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

    /**
     * Identifies the user in Firebase, Mixpanel and PostHog, and opts Mixpanel into tracking.
     *
     * Mixpanel is only opted in while PostHog is not: PostHog's opt-out is the one switch that
     * marks a silenced build (Robolectric, Firebase Test Lab), and turning Mixpanel back on here
     * would let a signed-in robot leak into it.
     *
     * The [userId] is the Firebase uid, the same id RevenueCat is logged in with (see
     * [com.jesuskrastev.bali.domain.repository.SubscriptionRepository.identify]), so a purchase
     * lands on the same person as the in-app events.
     *
     * @param userId Firebase uid of the signed-in user
     * @param email the user's email, stored as a person property when present
     */
    open fun identifyUser(userId: String, email: String? = null) {
        firebase.setUserId(userId)
        mixpanel.identify(userId)
        if (!posthog.isOptOut()) mixpanel.optInTracking()
        email?.let { mixpanel.people.set("\$email", it) }
        posthog.identify(distinctId = userId, userProperties = email?.let { mapOf("email" to it) })
    }

    /**
     * Resets analytics identity on sign-out. PostHog's reset also drops every registered
     * property, so the `environment` tag is registered again straight away.
     */
    open fun resetUser() {
        firebase.setUserId(null)
        mixpanel.reset()
        posthog.reset()
        posthog.register(KEY_ENVIRONMENT, currentEnvironment())
    }

    // ── AUTH ────────────────────────────────────────────────────────────────

    /** Tracks a new account creation with the given auth method. */
    open fun signUp(method: String) = log("sign_up") { putString("method", method) }

    /** Tracks a sign-in with the given auth method. */
    open fun login(method: String) = log("login") { putString("method", method) }

    /** Tracks a sign-out. */
    open fun logout() = log("logout")

    // ── NAVIGATION ──────────────────────────────────────────────────────────

    /**
     * Tracks that a screen became visible.
     *
     * PostHog gets it through its `screen()` call rather than as a plain event, so it produces
     * a `$screen` event and PostHog stamps `$screen_name` on every later event, which is what
     * lets funnels, paths and session replays be broken down by screen. The app is a single
     * Activity, so the SDK's own screen autocapture would only ever see one screen.
     *
     * @param screenName stable, argument-free name of the destination, e.g. `Home`
     */
    open fun screenViewed(screenName: String) {
        val environment = currentEnvironment()
        firebase.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(KEY_ENVIRONMENT, environment)
            }
        )
        mixpanel.track(
            "screen_viewed",
            JSONObject(mapOf("screen_name" to screenName, KEY_ENVIRONMENT to environment))
        )
        posthog.screen(screenTitle = screenName, properties = mapOf(KEY_ENVIRONMENT to environment))
    }

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

    /**
     * Tracks that the user reached the last onboarding screen (the "pact") and is about to
     * see the paywall — i.e. finished the onboarding *content*, independent of whether they
     * go on to buy. Use this, not [onboardingCompleted], to measure onboarding completion.
     *
     * @param profile the answers the user gave, keyed by property name. They travel with this
     *   event and are also kept as super properties, so later events (paywall, purchases, chat)
     *   can be segmented by them. Must never hold the user's name or any free-typed text
     */
    open fun onboardingFlowCompleted(profile: Map<String, String> = emptyMap()) {
        log("onboarding_flow_completed") { profile.forEach { (key, value) -> putString(key, value) } }
        registerProfile(profile)
    }

    /**
     * Attaches [profile] to every later event in Mixpanel and PostHog. Firebase has no
     * per-event properties; its user properties are capped at 25 and not needed for this.
     *
     * @param profile the answers to keep, keyed by property name
     */
    private fun registerProfile(profile: Map<String, String>) {
        if (profile.isEmpty()) return
        mixpanel.registerSuperProperties(JSONObject(profile))
        profile.forEach { (key, value) -> posthog.register(key, value) }
    }

    /**
     * Tracks that the onboarding flow ended with the user entitled to premium — a purchase in
     * release builds, or the debug paywall bypass in debug builds (tagged `environment=debug`
     * by [log], so it can be filtered out). Despite the name this is a purchase/entitlement
     * signal, not a content-completion one: see [onboardingFlowCompleted] for that. Flushes
     * immediately.
     */
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
    //
    // Not every paywall_* event in PostHog is sent from here. `paywall_impression`,
    // `paywall_close`, `paywall_cancel` and `paywall_component_interacted` come from RevenueCat's
    // own PostHog integration (server-to-server, so they carry no `$lib` and no `environment`),
    // and `rc_*` are its subscription lifecycle events. They overlap with `paywall_shown` and
    // `paywall_closed` below, and can only be switched off in the RevenueCat dashboard
    // (Integrations -> PostHog), never from this code.

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

    /**
     * Non-identifying description of the plan a paywall purchase event refers to. It comes
     * entirely from the store catalogue, so it carries nothing about the user.
     *
     * @property packageId RevenueCat package identifier, e.g. `$rc_monthly`
     * @property productId Google Play product id of the package
     * @property price price in major currency units (4.99, not 4,990,000 micros)
     * @property currency ISO 4217 currency code of [price]
     * @property period ISO 8601 billing period (`P1M`, `P1Y`), or null for a one-time product
     * @property offeringId RevenueCat offering the paywall was rendered from
     */
    data class PaywallPlan(
        val packageId: String,
        val productId: String,
        val price: Double,
        val currency: String,
        val period: String?,
        val offeringId: String
    )

    /**
     * Tracks that the user tapped a plan and the purchase flow began, so the Google Play sheet
     * is about to open. With [paywallPurchaseCancelled] and [paywallPurchaseFailed] this tells
     * apart people who never tapped "subscribe" from those who tapped it and backed out at the
     * store.
     *
     * @param plan the plan being purchased
     * @param secondsOnPaywall seconds between the paywall appearing and this tap
     * @param source entry point that triggered the paywall
     */
    open fun paywallPurchaseStarted(
        plan: PaywallPlan,
        secondsOnPaywall: Int,
        source: String = "onboarding"
    ) = log("paywall_purchase_started") { putPurchaseDetails(plan, secondsOnPaywall, source) }

    /**
     * Tracks that the user backed out of the Google Play sheet without buying.
     *
     * @param plan the plan that was being purchased, or null if it is unknown
     * @param secondsOnPaywall seconds since the paywall appeared
     * @param source entry point that triggered the paywall
     */
    open fun paywallPurchaseCancelled(
        plan: PaywallPlan?,
        secondsOnPaywall: Int,
        source: String = "onboarding"
    ) = log("paywall_purchase_cancelled") { putPurchaseDetails(plan, secondsOnPaywall, source) }

    /**
     * Tracks a purchase that failed for a reason other than the user cancelling: a store
     * problem, a pending payment, no connectivity. Only the error code is sent, never the
     * store's free-text message.
     *
     * @param errorCode name of the RevenueCat error code, e.g. `StoreProblemError`
     * @param plan the plan that was being purchased, or null if it is unknown
     * @param secondsOnPaywall seconds since the paywall appeared
     * @param source entry point that triggered the paywall
     */
    open fun paywallPurchaseFailed(
        errorCode: String,
        plan: PaywallPlan?,
        secondsOnPaywall: Int,
        source: String = "onboarding"
    ) = log("paywall_purchase_failed") {
        putString("error_code", errorCode)
        putPurchaseDetails(plan, secondsOnPaywall, source)
    }

    /**
     * Tracks that the store confirmed a purchase, straight from the paywall's own callback and
     * with the plan details. This is not [paywallPurchased], which fires when the user leaves
     * the paywall with premium active: that comes a moment later, and it also covers people
     * who already had premium. Flushes immediately.
     *
     * @param plan the plan that was purchased, or null if it is unknown
     * @param secondsOnPaywall seconds since the paywall appeared
     * @param source entry point that triggered the paywall
     */
    open fun paywallPurchaseCompleted(
        plan: PaywallPlan?,
        secondsOnPaywall: Int,
        source: String = "onboarding"
    ) {
        log("paywall_purchase_completed") { putPurchaseDetails(plan, secondsOnPaywall, source) }
        mixpanel.flush()
        posthog.flush()
    }

    /**
     * Tracks that the user tapped restore purchases on the paywall.
     *
     * @param source entry point that triggered the paywall
     */
    open fun paywallRestoreStarted(source: String = "onboarding") = log("paywall_restore_started") {
        putString("source", source)
    }

    /**
     * Tracks a restore that finished without error.
     *
     * @param hasPremium true when the restore left premium active, false when the store had
     *   nothing to give back
     * @param source entry point that triggered the paywall
     */
    open fun paywallRestoreCompleted(hasPremium: Boolean, source: String = "onboarding") =
        log("paywall_restore_completed") {
            putBoolean("has_premium", hasPremium)
            putString("source", source)
        }

    /**
     * Tracks a restore that failed. Only the error code is sent, never the free-text message.
     *
     * @param errorCode name of the RevenueCat error code, e.g. `NetworkError`
     * @param source entry point that triggered the paywall
     */
    open fun paywallRestoreFailed(errorCode: String, source: String = "onboarding") =
        log("paywall_restore_failed") {
            putString("error_code", errorCode)
            putString("source", source)
        }

    /**
     * Tracks that the one-time win-back discount offer was shown, after the user closed the
     * main paywall without buying.
     */
    open fun paywallWinbackShown() = log("paywall_winback_shown")

    /**
     * Tracks that the user left the paywall having bought the win-back offer. Flushes
     * immediately, like [paywallPurchased].
     */
    open fun paywallWinbackPurchased() {
        log("paywall_winback_purchased")
        mixpanel.flush()
        posthog.flush()
    }

    /**
     * Tracks that the user also closed the win-back offer without buying. Flushes immediately,
     * like [paywallClosed].
     */
    open fun paywallWinbackClosed() {
        log("paywall_winback_closed")
        mixpanel.flush()
        posthog.flush()
    }

    /**
     * Adds the properties every paywall purchase event shares.
     *
     * @param plan the plan the event refers to; its properties are skipped when null
     * @param secondsOnPaywall seconds since the paywall appeared
     * @param source entry point that triggered the paywall
     */
    private fun Bundle.putPurchaseDetails(plan: PaywallPlan?, secondsOnPaywall: Int, source: String) {
        plan?.let {
            putString("package_id", it.packageId)
            putString("product_id", it.productId)
            putDouble("price", it.price)
            putString("currency", it.currency)
            it.period?.let { period -> putString("period", period) }
            putString("offering_id", it.offeringId)
        }
        putInt("seconds_on_paywall", secondsOnPaywall)
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

    // ── AI GENERATION (test / exam) ────────────────────────────────────

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

    // ── STUDY LOOP ──────────────────────────────────────────────────────────
    //
    // The events that tell whether the habit loop works once someone is paying: did they finish
    // what they started (test, node, exam) and did it go well. Every one is sent once per
    // finished attempt, never per question.

    /**
     * Tracks that the user finished a practice test, whether it came from a learning-path node
     * or not.
     *
     * @param score number of correct answers
     * @param total number of questions in the test
     * @param accuracy correct answers as a whole percentage, 0 to 100
     * @param durationSeconds time between the first question appearing and the last answer
     * @param xpGained XP awarded for the attempt, already reduced when it is a repeat
     * @param nodeType [com.jesuskrastev.bali.domain.model.NodeType] name of the node the test
     *   belongs to (`LESSON`, `REVIEW`, `EXAM`), or null for a test outside the path
     * @param isRepeat true when the node had already been completed before this attempt
     */
    open fun testCompleted(
        score: Int,
        total: Int,
        accuracy: Int,
        durationSeconds: Int,
        xpGained: Int,
        nodeType: String?,
        isRepeat: Boolean
    ) = log("test_completed") {
        putInt("score", score)
        putInt("total_questions", total)
        putInt("accuracy", accuracy)
        putInt("duration_seconds", durationSeconds)
        putInt("xp_gained", xpGained)
        nodeType?.let { putString("node_type", it) }
        putBoolean("is_repeat", isRepeat)
    }

    /**
     * Tracks that the user finished the test of a learning-path node.
     *
     * The app marks a node as completed on any attempt and only unlocks the next one when the
     * score clears the bar, so [passed] and [unlockedNext] are what tell a real advance from a
     * mere attempt.
     *
     * @param nodeId identifier of the node on the learning path
     * @param nodeType `LESSON`, `REVIEW` or `EXAM`, or null when it is unknown
     * @param accuracy correct answers as a whole percentage, 0 to 100
     * @param passed true when [accuracy] cleared the bar that unlocks the next node
     * @param unlockedNext true when this attempt actually unlocked the next node
     * @param isRepeat true when the node had already been completed before this attempt
     */
    open fun nodeCompleted(
        nodeId: String,
        nodeType: String?,
        accuracy: Int,
        passed: Boolean,
        unlockedNext: Boolean,
        isRepeat: Boolean
    ) = log("node_completed") {
        putString("node_id", nodeId)
        nodeType?.let { putString("node_type", it) }
        putInt("accuracy", accuracy)
        putBoolean("passed", passed)
        putBoolean("unlocked_next", unlockedNext)
        putBoolean("is_repeat", isRepeat)
    }

    /**
     * Tracks that the user finished a full 30-question exam, pass or fail.
     *
     * @param score number of correct answers
     * @param total number of questions in the exam
     * @param accuracy correct answers as a whole percentage, 0 to 100
     * @param durationSeconds time spent on the exam
     * @param passed true when the score met the DGT bar (three mistakes or fewer)
     * @param xpGained XP awarded for the attempt, already reduced when it is a repeat
     * @param attemptNumber 1 for the first exam this user takes, 2 for the second, and so on
     */
    open fun examCompleted(
        score: Int,
        total: Int,
        accuracy: Int,
        durationSeconds: Int,
        passed: Boolean,
        xpGained: Int,
        attemptNumber: Int
    ) = log("exam_completed") {
        putInt("score", score)
        putInt("total_questions", total)
        putInt("accuracy", accuracy)
        putInt("duration_seconds", durationSeconds)
        putBoolean("passed", passed)
        putInt("xp_gained", xpGained)
        putInt("attempt_number", attemptNumber)
    }

    /**
     * Tracks that the user passed the in-app exam simulation. Sent on top of [examCompleted], so
     * the first pass and the number of tries it took can be read without filtering.
     *
     * This is the app's own 30-question exam, not the real DGT exam, whose result the app never
     * learns.
     *
     * @param score number of correct answers
     * @param total number of questions in the exam
     * @param durationSeconds time spent on the exam
     * @param attemptNumber how many exams this user had taken up to and including this one
     */
    open fun examPassed(score: Int, total: Int, durationSeconds: Int, attemptNumber: Int) =
        log("exam_passed") {
            putInt("score", score)
            putInt("total_questions", total)
            putInt("duration_seconds", durationSeconds)
            putInt("attempt_number", attemptNumber)
        }

    /**
     * Tracks that the user failed the in-app exam simulation. Sent on top of [examCompleted], so
     * failures can be counted, and told apart by cause, without filtering.
     *
     * Like [examPassed], this is the app's own 30-question exam, not the real DGT exam.
     *
     * @param score number of correct answers
     * @param total number of questions in the exam
     * @param mistakes number of wrong or unanswered questions; the exam is failed from the fourth
     * @param durationSeconds time spent on the exam
     * @param timeRanOut true when the clock ran out before the user finished, which tells a
     *   student who needs more speed from one who needs more knowledge
     * @param attemptNumber how many exams this user had taken up to and including this one
     */
    open fun examFailed(
        score: Int,
        total: Int,
        mistakes: Int,
        durationSeconds: Int,
        timeRanOut: Boolean,
        attemptNumber: Int
    ) = log("exam_failed") {
        putInt("score", score)
        putInt("total_questions", total)
        putInt("mistakes", mistakes)
        putInt("duration_seconds", durationSeconds)
        putBoolean("time_ran_out", timeRanOut)
        putInt("attempt_number", attemptNumber)
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

    companion object {
        /** Property that tells debug-build traffic apart from real users. */
        const val KEY_ENVIRONMENT = "environment"

        private const val ENVIRONMENT_DEBUG = "debug"
        private const val ENVIRONMENT_PRODUCTION = "production"

        /**
         * Names the kind of build this process is, for the [KEY_ENVIRONMENT] property.
         *
         * @return `"debug"` on debug builds, `"production"` otherwise
         */
        fun currentEnvironment(): String =
            if (BuildConfig.DEBUG) ENVIRONMENT_DEBUG else ENVIRONMENT_PRODUCTION
    }
}
