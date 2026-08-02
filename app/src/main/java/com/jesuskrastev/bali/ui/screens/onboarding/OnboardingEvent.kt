package com.jesuskrastev.bali.ui.screens.onboarding

sealed class OnboardingEvent {
    // Selection Events
    data class SetName(val name: String) : OnboardingEvent()
    data class SelectLicense(val license: String) : OnboardingEvent()
    data class SelectExperience(val experience: String) : OnboardingEvent()
    data class SelectTheoryBlocker(val blocker: String) : OnboardingEvent()
    data class SelectMotivation(val motivation: String) : OnboardingEvent()
    data class SelectFutureImpact(val impact: String) : OnboardingEvent()
    data class SelectExamDate(val dateMillis: Long?) : OnboardingEvent()
    data class SelectDailyGoal(val goal: String) : OnboardingEvent()
    data class SelectLearningPreference(val preference: String) : OnboardingEvent()
    data class ToggleDifficultTopic(val topic: String) : OnboardingEvent()
    // Navigation Events
    data object GoToNextStep : OnboardingEvent()
    data object GoToPreviousStep : OnboardingEvent()
    data object CompleteOnboarding : OnboardingEvent()
    // Rating Events
    data object RateAppClicked : OnboardingEvent()
}
