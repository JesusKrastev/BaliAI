package com.jesuskrastev.bali.data.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsTracker @Inject constructor(
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

    fun identifyUser(userId: String, email: String) {
        firebase.setUserId(userId)
        mixpanel.identify(userId)
        mixpanel.people.set("\$email", email)
    }

    fun resetUser() {
        firebase.setUserId(null)
        mixpanel.reset()
    }

    // ── ONBOARDING ──────────────────────────────────────────────────────────

    fun onboardingStarted() = log("onboarding_started")

    fun onboardingStepCompleted(stepName: String) = log("onboarding_step_completed") {
        putString("step_name", stepName)
    }

    fun onboardingCompleted() = log("onboarding_completed")

    fun onboardingAbandoned(lastStep: String) = log("onboarding_abandoned") {
        putString("last_step", lastStep)
    }

    // ── AUTH ────────────────────────────────────────────────────────────────

    fun signUp() = log("sign_up")

    fun login() = log("login")

    fun logout() = log("logout")

    // ── TESTS ───────────────────────────────────────────────────────────────

    fun testStarted(type: String) = log("test_started") {
        putString("type", type)
    }

    fun testCompleted(type: String) = log("test_completed") {
        putString("type", type)
    }

    fun testAbandoned(type: String, questionNumber: Int) = log("test_abandoned") {
        putString("type", type)
        putInt("question_number", questionNumber)
    }

    fun questionAnswered(correct: Boolean) = log("question_answered") {
        putBoolean("correct", correct)
    }

    // ── REPASO ──────────────────────────────────────────────────────────────

    fun reviewStarted() = log("review_started")

    fun reviewCompleted() = log("review_completed")

    // ── TIENDA ──────────────────────────────────────────────────────────────

    fun storeOpened() = log("store_opened")

    fun storeItemViewed(item: String) = log("store_item_viewed") {
        putString("item", item)
    }

    fun storeItemPurchased(item: String) = log("store_item_purchased") {
        putString("item", item)
    }

    // ── GEMINI ──────────────────────────────────────────────────────────────

    fun geminiUsage(
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

    fun energyConsumed(remaining: Int) = log("energy_consumed") {
        putInt("remaining", remaining)
    }

    fun energyDepleted() = log("energy_depleted")

    fun coinsEarned(amount: Int) = log("coins_earned") {
        putInt("amount", amount)
    }

    fun coinsSpent(amount: Int, item: String) = log("coins_spent") {
        putInt("amount", amount)
        putString("item", item)
    }

    fun streakRecorded(streak: Int) = log("streak_recorded") {
        putInt("streak", streak)
    }

    fun streakFreezerUsed(count: Int) = log("streak_freezer_used") {
        putInt("count", count)
    }

    fun dgtSimulacroUnlocked() = log("dgt_simulacro_unlocked")
}
