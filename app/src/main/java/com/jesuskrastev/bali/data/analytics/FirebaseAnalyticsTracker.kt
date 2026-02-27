package com.jesuskrastev.bali.data.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticsTracker @Inject constructor(
    private val analytics: FirebaseAnalytics
) {

    private fun log(event: String, params: Bundle.() -> Unit = {}) {
        analytics.logEvent(event, Bundle().apply(params))
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
}