package com.jesuskrastev.bali.ui.screens.onboarding

sealed class OnboardingEvent {
    // Selection Events
    data class SetName(val name: String) : OnboardingEvent()
    data class SelectExperience(val experience: String) : OnboardingEvent()
    data class SelectTheoryBlocker(val blocker: String) : OnboardingEvent()
    data class SelectConcern(val concern: String) : OnboardingEvent()
    data class SelectReadiness(val readiness: String) : OnboardingEvent()
    data class SelectMotivation(val motivation: String) : OnboardingEvent()

    /**
     * The answer to the exam date screen.
     *
     * @property pickerMillis the day picked, as the Material date picker gives it (midnight UTC),
     *   or null for "Aún no tengo fecha"
     */
    data class SetExamDate(val pickerMillis: Long?) : OnboardingEvent()
    data class SelectProvince(val province: String) : OnboardingEvent()
    data class SelectWeeklyStudy(val weeklyStudy: String) : OnboardingEvent()
    data class SelectStudyTime(val studyTime: String) : OnboardingEvent()
    /** The answer to the reminders offer: true for "Sí, avísame", false for "Ahora no". */
    data class AnswerNotifications(val accepted: Boolean) : OnboardingEvent()

    /** @property key the [LearningStyle.key] tapped */
    data class SelectLearningPreference(val key: String) : OnboardingEvent()

    // Intro Events
    /** @property position the intro card that came on screen, counted from 0 */
    data class IntroCardShown(val position: Int) : OnboardingEvent()

    // Mini-test Events
    /** @property optionIndex the option tapped on the current question of the mini-test */
    data class AnswerQuiz(val optionIndex: Int) : OnboardingEvent()
    /** "Siguiente" under an answered question: the next one, or the result after the last. */
    data object NextQuizQuestion : OnboardingEvent()
    /** "Saltar" on the first question: goes on without the test or its result. */
    data object SkipQuiz : OnboardingEvent()

    // Navigation Events
    data object GoToNextStep : OnboardingEvent()
    data object GoToPreviousStep : OnboardingEvent()
    data object CompleteOnboarding : OnboardingEvent()
}
