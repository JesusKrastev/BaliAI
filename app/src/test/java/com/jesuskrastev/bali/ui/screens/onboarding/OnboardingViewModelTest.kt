package com.jesuskrastev.bali.ui.screens.onboarding

import com.jesuskrastev.bali.domain.model.StudyRhythm
import com.jesuskrastev.bali.domain.model.StudySchedule
import com.jesuskrastev.bali.domain.model.StudySlot
import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.ui.screens.auth.FakeAnalyticsTracker
import com.jesuskrastev.bali.ui.screens.auth.FakeNotificationsRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import org.mockito.kotlin.mock

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeUserRepository = FakeUserRepository()
    private val fakeAnalyticsTracker = FakeAnalyticsTracker(mock(), mock(), mock())
    private var fakeNotificationsRepository = FakeNotificationsRepository()

    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        viewModel = newViewModel()
    }

    /**
     * Builds the ViewModel under test on the shared fakes.
     *
     * @param notificationsRepository the notifications fake to use, which also replaces the
     *   shared one so assertions read the same instance
     */
    private fun newViewModel(
        notificationsRepository: FakeNotificationsRepository = fakeNotificationsRepository
    ): OnboardingViewModel {
        fakeNotificationsRepository = notificationsRepository
        return OnboardingViewModel(
            userRepository = fakeUserRepository,
            analyticsTracker = fakeAnalyticsTracker,
            notificationsRepository = notificationsRepository
        )
    }

    @Test
    fun `the flow opens on a tappable question, not on the keyboard`() = runTest {
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Motivation)
    }

    @Test
    fun `the name is only asked once the emotional arc is over`() = runTest {
        advanceThroughArc()

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Name)
    }

    @Test
    fun `name update changes canGoNext`() = runTest {
        advanceThroughArc()

        assertThat(viewModel.uiState.value.canGoNext).isFalse()
        viewModel.onEvent(OnboardingEvent.SetName("Jesus"))
        assertThat(viewModel.uiState.value.canGoNext).isTrue()
    }

    @Test
    fun `the diagnosis runs before the emotional arc`() = runTest {
        advanceThroughDiagnosis()

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.FutureImpact)

        viewModel.onEvent(OnboardingEvent.SelectFutureImpact(OnboardingConfig.futureImpacts.first()))
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Empathy)
    }

    @Test
    fun `the loss block leads into the method comparison`() = runTest {
        advanceThroughDiagnosis()
        viewModel.onEvent(OnboardingEvent.SelectFutureImpact(OnboardingConfig.futureImpacts.first()))

        val expectedArc = listOf(
            OnboardingStep.LossTime,
            OnboardingStep.LossOpportunity,
            OnboardingStep.LossAutonomy,
            OnboardingStep.MethodComparison,
            OnboardingStep.GainFreedom
        )

        expectedArc.forEach { expectedStep ->
            viewModel.onEvent(OnboardingEvent.GoToNextStep)
            assertThat(viewModel.uiState.value.currentStep).isEqualTo(expectedStep)
        }
    }

    @Test
    fun `the why answers are kept for the plan reveal`() = runTest {
        val motivation = OnboardingConfig.motivations.first()
        val impact = OnboardingConfig.futureImpacts.first()

        viewModel.onEvent(OnboardingEvent.SelectMotivation(motivation))
        viewModel.onEvent(OnboardingEvent.SelectTheoryBlocker(OnboardingConfig.theoryBlockers.first()))
        viewModel.onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.concerns.first()))
        viewModel.onEvent(OnboardingEvent.SelectExperience(OnboardingConfig.experiences.first()))
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        viewModel.onEvent(OnboardingEvent.SelectReadiness(OnboardingConfig.readinessLevels.first()))
        viewModel.onEvent(OnboardingEvent.SelectFutureImpact(impact))

        assertThat(viewModel.uiState.value.data.motivation).isEqualTo(motivation)
        assertThat(viewModel.uiState.value.data.futureImpact).isEqualTo(impact)
    }

    @Test
    fun `a booked exam becomes a date the plan can count down from`() = runTest {
        viewModel.onEvent(OnboardingEvent.SelectExamTiming(OnboardingConfig.examTimings.first()))

        val examDate = viewModel.uiState.value.data.examDate
        assertThat(examDate).isNotNull()
        assertThat(examDate!!).isGreaterThan(System.currentTimeMillis())
    }

    @Test
    fun `an unbooked exam leaves the plan without a date`() = runTest {
        viewModel.onEvent(OnboardingEvent.SelectExamTiming(OnboardingConfig.EXAM_TIMING_UNBOOKED))

        assertThat(viewModel.uiState.value.data.examTiming)
            .isEqualTo(OnboardingConfig.EXAM_TIMING_UNBOOKED)
        assertThat(viewModel.uiState.value.data.examDate).isNull()
    }

    @Test
    fun `picking a province does not advance on its own`() = runTest {
        // The user has to see which province landed in the field before moving on.
        val stepBefore = viewModel.uiState.value.currentStep

        viewModel.onEvent(OnboardingEvent.SelectProvince("Almería"))

        assertThat(viewModel.uiState.value.data.province).isEqualTo("Almería")
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(stepBefore)
    }

    @Test
    fun `the first screen is counted before the user does anything`() = runTest {
        assertThat(fakeAnalyticsTracker.onboardingSteps).containsExactly("o01_motivation")
    }

    @Test
    fun `every screen reached emits exactly one funnel event, numbered in order`() = runTest {
        advanceThroughDiagnosis()

        assertThat(fakeAnalyticsTracker.onboardingSteps)
            .containsExactly(
                "o01_motivation",
                "o02_reasons",
                "o03_concern",
                "o04_experience",
                "o05_comparison",
                "o06_readiness",
                "o07_future_impact"
            )
            .inOrder()
    }

    @Test
    fun `a screen the user only lands on is still counted`() = runTest {
        advanceThroughDiagnosis()

        // The user goes no further: the screen must appear even though it was never answered.
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.FutureImpact)
        assertThat(fakeAnalyticsTracker.onboardingSteps).contains("o07_future_impact")
    }

    @Test
    fun `event names are zero padded so they sort in flow order`() = runTest {
        // Without padding "o10_" would sort before "o02_" and the funnel would read wrong.
        assertThat(fakeAnalyticsTracker.onboardingSteps.first()).startsWith("o01_")
    }

    @Test
    fun `event names start with a letter, which Firebase requires`() = runTest {
        advanceThroughDiagnosis()

        fakeAnalyticsTracker.onboardingSteps.forEach { eventName ->
            assertThat(eventName.first().isLetter()).isTrue()
        }
    }

    @Test
    fun `the reminder is offered right after the study rhythm`() = runTest {
        advanceToWeeklyStudy()

        viewModel.onEvent(OnboardingEvent.SelectWeeklyStudy(OnboardingConfig.WEEKLY_STUDY_OFTEN))
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.StudyTime)

        viewModel.onEvent(OnboardingEvent.SelectStudyTime(OnboardingConfig.studyTimes.keys.last()))
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Notifications)

        viewModel.onEvent(OnboardingEvent.AnswerNotifications(accepted = true))
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.LearningPreference)
    }

    @Test
    fun `the new screens are numbered in the funnel after the study rhythm`() = runTest {
        advanceToNotifications()

        assertThat(fakeAnalyticsTracker.onboardingSteps.takeLast(3))
            .containsExactly("o20_weekly_study", "o21_study_time", "o22_notifications")
            .inOrder()
    }

    @Test
    fun `the study time is saved on the device with the rhythm it goes with`() = runTest {
        advanceToWeeklyStudy()
        viewModel.onEvent(OnboardingEvent.SelectWeeklyStudy(OnboardingConfig.WEEKLY_STUDY_DAILY))

        val night = OnboardingConfig.studyTimes.entries.first { it.value == StudySlot.NIGHT }.key
        viewModel.onEvent(OnboardingEvent.SelectStudyTime(night))

        assertThat(fakeNotificationsRepository.savedSchedule)
            .isEqualTo(StudySchedule(StudySlot.NIGHT, StudyRhythm.DAILY))
    }

    @Test
    fun `the reminder offer names the hour just picked`() = runTest {
        advanceToWeeklyStudy()
        viewModel.onEvent(OnboardingEvent.SelectWeeklyStudy(OnboardingConfig.WEEKLY_STUDY_OFTEN))

        val morning = OnboardingConfig.studyTimes.entries.first { it.value == StudySlot.MORNING }.key
        viewModel.onEvent(OnboardingEvent.SelectStudyTime(morning))

        assertThat(viewModel.uiState.value.mascotMessage).contains("9:00")
    }

    @Test
    fun `saying yes asks Android and reports the permission as granted`() = runTest {
        advanceToNotifications()

        viewModel.onEvent(OnboardingEvent.AnswerNotifications(accepted = true))

        assertThat(fakeNotificationsRepository.permissionRequests).isEqualTo(1)
        assertThat(fakeAnalyticsTracker.notificationsAnswers).containsExactly("granted" to "night")
        assertThat(viewModel.uiState.value.data.notifications).isEqualTo(NotificationsAnswer.GRANTED)
    }

    @Test
    fun `refusing the system dialog is reported apart from saying no in the app`() = runTest {
        viewModel = newViewModel(FakeNotificationsRepository(grantsPermission = false))
        advanceToNotifications()

        viewModel.onEvent(OnboardingEvent.AnswerNotifications(accepted = true))

        assertThat(fakeAnalyticsTracker.notificationsAnswers).containsExactly("denied" to "night")
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.LearningPreference)
    }

    @Test
    fun `saying no never shows the system dialog and keeps pushes off`() = runTest {
        advanceToNotifications()

        viewModel.onEvent(OnboardingEvent.AnswerNotifications(accepted = false))

        assertThat(fakeNotificationsRepository.permissionRequests).isEqualTo(0)
        assertThat(fakeNotificationsRepository.optedOut).isTrue()
        assertThat(fakeAnalyticsTracker.notificationsAnswers).containsExactly("declined" to "night")
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.LearningPreference)
    }

    @Test
    fun `a second tap on the reminder offer cannot skip the next question`() = runTest {
        advanceToNotifications()

        // The old screen is still on screen while it slides away.
        viewModel.onEvent(OnboardingEvent.AnswerNotifications(accepted = false))
        viewModel.onEvent(OnboardingEvent.AnswerNotifications(accepted = false))

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.LearningPreference)
        assertThat(fakeAnalyticsTracker.notificationsAnswers).hasSize(1)
    }

    /**
     * Answers everything up to the study rhythm question, leaving the flow on
     * [OnboardingStep.WeeklyStudy].
     */
    private fun advanceToWeeklyStudy() {
        advanceThroughArc()
        viewModel.onEvent(OnboardingEvent.SetName("Jesus"))
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        viewModel.onEvent(OnboardingEvent.SelectExamTiming(OnboardingConfig.EXAM_TIMING_SOON))
        viewModel.onEvent(OnboardingEvent.SelectProvince("Almería"))
        // The province and its confirmation both wait for the bottom button.
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
    }

    /**
     * Answers everything up to the reminders offer with a night study time, leaving the flow
     * on [OnboardingStep.Notifications].
     */
    private fun advanceToNotifications() {
        advanceToWeeklyStudy()
        viewModel.onEvent(OnboardingEvent.SelectWeeklyStudy(OnboardingConfig.WEEKLY_STUDY_OFTEN))
        val night = OnboardingConfig.studyTimes.entries.first { it.value == StudySlot.NIGHT }.key
        viewModel.onEvent(OnboardingEvent.SelectStudyTime(night))
    }

    /** Answers the whole diagnosis block, leaving the flow on [OnboardingStep.FutureImpact]. */
    private fun advanceThroughDiagnosis() {
        viewModel.onEvent(OnboardingEvent.SelectMotivation(OnboardingConfig.motivations.first()))
        viewModel.onEvent(OnboardingEvent.SelectTheoryBlocker(OnboardingConfig.theoryBlockers.first()))
        viewModel.onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.concerns.first()))
        viewModel.onEvent(OnboardingEvent.SelectExperience(OnboardingConfig.experiences.first()))
        // Comparison is informational and needs the bottom button.
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        viewModel.onEvent(OnboardingEvent.SelectReadiness(OnboardingConfig.readinessLevels.first()))
    }

    /**
     * Answers everything up to and including the gain block, leaving the flow on
     * [OnboardingStep.Name] — the first screen that asks the user to type.
     */
    private fun advanceThroughArc() {
        advanceThroughDiagnosis()
        viewModel.onEvent(OnboardingEvent.SelectFutureImpact(OnboardingConfig.futureImpacts.first()))
        // Empathy, the three losses, the method and the three gains are all tap-to-continue.
        repeat(8) { viewModel.onEvent(OnboardingEvent.GoToNextStep) }
    }
}
