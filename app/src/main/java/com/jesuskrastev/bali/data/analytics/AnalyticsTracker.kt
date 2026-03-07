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
                } catch (e: Exception) {
                    // Ignore JSON exception for this property
                }
            }
        }
        return json
    }

    // ── USERS ───────────────────────────────────────────────────────────────

    open fun identifyUser(userId: String, email: String? = null) {
        firebase.setUserId(userId) // Keep Firebase setUserId as it's not removed in the instruction
        mixpanel.identify(userId)
        email?.let { mixpanel.people.set("\$email", it) }
    }

    open fun resetUser() {
        firebase.setUserId(null)
        mixpanel.reset()
    }

    // ── AUTH ────────────────────────────────────────────────────────────────

    open fun signUp(method: String) {
        trackEvent("sign_up", mapOf("method" to method))
    }

    open fun login(method: String) {
        trackEvent("login", mapOf("method" to method))
    }

    open fun logout() = log("logout")

    open fun trackEvent(eventName: String, properties: Map<String, Any>? = null) {
        // The instruction only provided the signature and opening brace.
        // Assuming a default implementation that logs to Firebase and Mixpanel.
        val bundle = Bundle()
        properties?.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                is Double -> bundle.putDouble(key, value)
                is Long -> bundle.putLong(key, value)
                // Add other types as needed
                else -> bundle.putString(key, value.toString())
            }
        }
        firebase.logEvent(eventName, bundle)
        mixpanel.track(eventName, bundleToJson(bundle))
    }

    // ── ONBOARDING ──────────────────────────────────────────────────────────

    open fun onboardingStarted() = log("onboarding_started")

    open fun onboardingStepCompleted(stepName: String) = log("onboarding_step_completed") {
        putString("step_name", stepName)
    }

    open fun onboardingCompleted() = log("onboarding_completed")

    open fun onboardingAbandoned(lastStep: String) = log("onboarding_abandoned") {
        putString("last_step", lastStep)
    }

    // ── TESTS ───────────────────────────────────────────────────────────────

    open fun testStarted(type: String) = log("test_started") {
        putString("type", type)
    }

    open fun testCompleted(type: String) = log("test_completed") {
        putString("type", type)
    }

    open fun testAbandoned(type: String, questionNumber: Int) = log("test_abandoned") {
        putString("type", type)
        putInt("question_number", questionNumber)
    }

    open fun questionAnswered(correct: Boolean) = log("question_answered") {
        putBoolean("correct", correct)
    }

    // ── REPASO ──────────────────────────────────────────────────────────────

    open fun reviewStarted() = log("review_started")

    open fun reviewCompleted() = log("review_completed")

    // ── TIENDA ──────────────────────────────────────────────────────────────

    open fun storeOpened() = log("store_opened")

    open fun storeItemViewed(item: String) = log("store_item_viewed") {
        putString("item", item)
    }

    open fun storeItemPurchased(item: String) = log("store_item_purchased") {
        putString("item", item)
    }

    // ── GEMINI ──────────────────────────────────────────────────────────────

    open fun geminiUsage(
        inputTokens: Int,
        outputTokens: Int,
        feature: String = "unknown"
    ) {
        log("gemini_usage") {
            putInt("input_tokens", inputTokens)
            putInt("output_tokens", outputTokens)
            putString("feature", feature)
        }
    }

    // ── ECONOMÍA ─────────────────────────────────────────────────────

    open fun energyConsumed(remaining: Int) = log("energy_consumed") {
        putInt("remaining", remaining)
    }

    open fun energyDepleted() = log("energy_depleted")

    open fun coinsEarned(amount: Int) = log("coins_earned") {
        putInt("amount", amount)
    }

    open fun coinsSpent(amount: Int, item: String) = log("coins_spent") {
        putInt("amount", amount)
        putString("item", item)
    }

    open fun streakRecorded(streak: Int) = log("streak_recorded") {
        putInt("streak", streak)
    }

    open fun streakFreezerUsed(count: Int) = log("streak_freezer_used") {
        putInt("count", count)
    }

    open fun dgtSimulacroUnlocked() = log("dgt_simulacro_unlocked")
}
