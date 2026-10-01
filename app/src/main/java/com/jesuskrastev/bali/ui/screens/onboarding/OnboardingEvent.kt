package com.jesuskrastev.bali.ui.screens.onboarding

sealed class OnboardingEvent {
    // Selection Events
    data class SetName(val name: String) : OnboardingEvent()
    data class SelectExperience(val experience: String) : OnboardingEvent()
    data class SelectTheoryBlocker(val blocker: String) : OnboardingEvent()
    data class SelectConcern(val concern: String) : OnboardingEvent()
    data class SelectReadiness(val readiness: String) : OnboardingEvent()
    data class SelectMotivation(val motivation: String) : OnboardingEvent()
    data class SelectFutureImpact(val impact: String) : OnboardingEvent()
    data class SelectExamTiming(val timing: String) : OnboardingEvent()
    data class SelectProvince(val province: String) : OnboardingEvent()
    data class SelectWeeklyStudy(val weeklyStudy: String) : OnboardingEvent()
    data class SelectStudyTime(val studyTime: String) : OnboardingEvent()
    /** The answer to the reminders offer: true for "Sí, avísame", false for "Ahora no". */
    data class AnswerNotifications(val accepted: Boolean) : OnboardingEvent()
    data class SelectLearningPreference(val preference: String) : OnboardingEvent()
    // Navigation Events
    data object GoToNextStep : OnboardingEvent()
    data object GoToPreviousStep : OnboardingEvent()
    data object CompleteOnboarding : OnboardingEvent()
}
