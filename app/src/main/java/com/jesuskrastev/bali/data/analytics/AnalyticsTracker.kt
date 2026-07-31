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

    /** Tracks completion of the Name step. */
    open fun onboardingStepName() = log("name")

    /** Tracks completion of the License step. */
    open fun onboardingStepLicense() = log("license")

    /** Tracks completion of the Experience step. */
    open fun onboardingStepExperience() = log("experience")

    /** Tracks completion of the DialogueExperience step. */
    open fun onboardingStepDialogueExperience() = log("dialogue_experience")

    /** Tracks completion of the Reasons step. */
    open fun onboardingStepReasons() = log("reasons")

    /** Tracks completion of the ExamDate step. */
    open fun onboardingStepExamDate() = log("exam_date")

    /** Tracks completion of the MethodComparison step. */
    open fun onboardingStepMethodComparison() = log("method_comparison")

    /** Tracks completion of the DailyGoal step. */
    open fun onboardingStepDailyGoal() = log("daily_goal")

    /** Tracks completion of the LearningPreference step. */
    open fun onboardingStepLearningPreference() = log("learning_preference")

    /** Tracks completion of the DifficultTopics step. */
    open fun onboardingStepDifficultTopics() = log("difficult_topics")

    /** Tracks completion of the DialogueDifficultTopics step. */
    open fun onboardingStepDialogueDifficultTopics() = log("dialogue_difficult_topics")

    /** Tracks completion of the Concern step. */
    open fun onboardingStepConcern() = log("concern")

    /** Tracks completion of the StudyTime step. */
    open fun onboardingStepStudyTime() = log("study_time")

    /** Tracks completion of the Notifications step. */
    open fun onboardingStepNotifications() = log("notifications")

    /** Tracks completion of the SocialProof step. */
    open fun onboardingStepSocialProof() = log("social_proof")

    /** Tracks that the user tapped the "rate the app" button on the SocialProof step. */
    open fun onboardingRateAppClicked() = log("onboarding_rate_app_clicked")

    /** Tracks completion of the Processing step. */
    open fun onboardingStepProcessing() = log("processing")

    /** Tracks completion of the Comparison step. */
    open fun onboardingStepComparison() = log("comparison")

    /** Tracks completion of the LossAversion step. */
    open fun onboardingStepLossAversion() = log("loss_aversion")

    /** Tracks completion of the Pact step. */
    open fun onboardingStepPact() = log("pact")

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
     * Tracks that the user dismissed the paywall and flushes immediately.
     *
     * @param purchased true if the user completed a purchase before dismissing
     * @param source entry point that triggered the paywall
     */
    open fun paywallDismissed(purchased: Boolean, source: String = "onboarding") {
        log("paywall_dismissed") {
            putBoolean("purchased", purchased)
            putString("source", source)
        }
        mixpanel.flush()
    }
}
