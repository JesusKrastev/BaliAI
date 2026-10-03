package com.jesuskrastev.bali.ui.screens.onboarding

import com.jesuskrastev.bali.domain.model.StudyRhythm
import com.jesuskrastev.bali.domain.model.StudySchedule
import com.jesuskrastev.bali.domain.model.StudySlot
import com.jesuskrastev.bali.domain.repository.NotificationsRepository
import com.jesuskrastev.bali.util.FakeSoundEffects
import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.ui.screens.stats.localDayFromPickerMillis
import com.jesuskrastev.bali.ui.screens.stats.pickerMillisFromLocalDay
import java.util.concurrent.TimeUnit
import com.jesuskrastev.bali.ui.screens.auth.FakeAnalyticsTracker
import com.jesuskrastev.bali.ui.screens.auth.FakeNotificationsRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
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
    private val fakeSoundEffects = FakeSoundEffects()

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
            notificationsRepository = notificationsRepository,
            soundEffects = fakeSoundEffects
        )
    }

    @Test
    fun `the flow opens on what Bali is, before any question`() = runTest {
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Intro)

        viewModel.onEvent(OnboardingEvent.GoToNextStep)

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Motivation)
    }

    @Test
    fun `each intro card shown is reported by its position from 1`() = runTest {
        viewModel.onEvent(OnboardingEvent.IntroCardShown(0))
        viewModel.onEvent(OnboardingEvent.IntroCardShown(3))

        assertThat(fakeAnalyticsTracker.introCards).containsExactly(1, 4).inOrder()
    }

    @Test
    fun `the pain answers the blocker right after it is named`() = runTest {
        startQuestions()
        viewModel.onEvent(OnboardingEvent.SelectMotivation(OnboardingConfig.motivations.first()))
        viewModel.onEvent(OnboardingEvent.SelectTheoryBlocker(OnboardingConfig.theoryBlockers.first()))

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Pain)
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
    fun `the worry leads into the mini-test it picks`() = runTest {
        advanceThroughDiagnosis()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Concern)

        viewModel.onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.CONCERN_SILLY_MISTAKES))

        val state = viewModel.uiState.value
        assertThat(state.currentStep).isEqualTo(OnboardingStep.Quiz)
        assertThat(state.data.quizQuestions())
            .isEqualTo(OnboardingQuiz.questionsFor(OnboardingConfig.CONCERN_SILLY_MISTAKES))
    }

    @Test
    fun `after the test result come the road to the exam and the gain, then the name`() = runTest {
        advanceThroughQuiz()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.MethodComparison)

        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Gain)

        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Name)
    }

    @Test
    fun `the why answers are kept for the plan reveal`() = runTest {
        val motivation = OnboardingConfig.motivations.first()

        advanceThroughDiagnosis(motivation = motivation)

        assertThat(viewModel.uiState.value.data.motivation).isEqualTo(motivation)
    }

    @Test
    fun `the day picked becomes the exam date, as the local day`() = runTest {
        advanceToExamDate()
        val picked = examDayPickerMillis()

        viewModel.onEvent(OnboardingEvent.SetExamDate(picked))

        val state = viewModel.uiState.value
        assertThat(state.data.examDate).isEqualTo(localDayFromPickerMillis(picked))
        assertThat(state.currentStep).isEqualTo(OnboardingStep.Province)
        assertThat(fakeAnalyticsTracker.examDates).containsExactly(EXAM_IN_DAYS to "onboarding")
    }

    @Test
    fun `having no date yet leaves the plan without one and moves on`() = runTest {
        advanceToExamDate()

        viewModel.onEvent(OnboardingEvent.SetExamDate(null))

        val state = viewModel.uiState.value
        assertThat(state.data.examDate).isNull()
        assertThat(state.currentStep).isEqualTo(OnboardingStep.Province)
        assertThat(fakeAnalyticsTracker.examDates).isEmpty()
    }

    @Test
    fun `an answer is marked once, with its sound and its event`() = runTest {
        advanceThroughDiagnosis()
        viewModel.onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.CONCERN_SILLY_MISTAKES))
        val question = viewModel.uiState.value.data.quizQuestions().first()
        val wrong = (question.correctIndex + 1) % question.options.size

        viewModel.onEvent(OnboardingEvent.AnswerQuiz(wrong))
        viewModel.onEvent(OnboardingEvent.AnswerQuiz(question.correctIndex))

        val answers = viewModel.uiState.value.data.quizAnswers
        assertThat(answers).containsExactly(QuizAnswer(question.id, wrong, isCorrect = false))
        assertThat(fakeSoundEffects.wrongPlays).isEqualTo(1)
        assertThat(fakeSoundEffects.correctPlays).isEqualTo(0)
        assertThat(fakeAnalyticsTracker.quizAnswers).containsExactly(question.id to false)
    }

    @Test
    fun `the next question waits for an answer`() = runTest {
        advanceThroughDiagnosis()
        viewModel.onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.concerns.first()))

        viewModel.onEvent(OnboardingEvent.NextQuizQuestion)

        assertThat(viewModel.uiState.value.quizIndex).isEqualTo(0)
    }

    @Test
    fun `after the last question comes the result`() = runTest {
        advanceThroughDiagnosis()
        viewModel.onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.concerns.first()))

        answerQuiz(correct = true)

        val state = viewModel.uiState.value
        assertThat(state.currentStep).isEqualTo(OnboardingStep.QuizResult)
        assertThat(state.data.quizScore()).isEqualTo(OnboardingQuiz.QUESTION_COUNT)
        assertThat(fakeSoundEffects.correctPlays).isEqualTo(OnboardingQuiz.QUESTION_COUNT)
    }

    @Test
    fun `skipping the test skips its result too, both ways`() = runTest {
        advanceThroughDiagnosis()
        viewModel.onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.concerns.first()))

        viewModel.onEvent(OnboardingEvent.SkipQuiz)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.MethodComparison)
        assertThat(fakeAnalyticsTracker.quizSkips).isEqualTo(1)

        viewModel.onEvent(OnboardingEvent.GoToPreviousStep)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Quiz)
    }

    @Test
    fun `the test cannot be skipped once started`() = runTest {
        advanceThroughDiagnosis()
        viewModel.onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.concerns.first()))
        viewModel.onEvent(OnboardingEvent.AnswerQuiz(0))

        viewModel.onEvent(OnboardingEvent.SkipQuiz)

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Quiz)
        assertThat(fakeAnalyticsTracker.quizSkips).isEqualTo(0)
    }

    @Test
    fun `another worry starts the test over with its own questions`() = runTest {
        advanceThroughDiagnosis()
        viewModel.onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.CONCERN_NOT_READY))
        viewModel.onEvent(OnboardingEvent.AnswerQuiz(0))
        viewModel.onEvent(OnboardingEvent.GoToPreviousStep)

        viewModel.onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.CONCERN_EXAM_MISMATCH))

        val state = viewModel.uiState.value
        assertThat(state.data.quizAnswers).isEmpty()
        assertThat(state.quizIndex).isEqualTo(0)
    }

    @Test
    fun `the same worry keeps the answers already given`() = runTest {
        advanceThroughDiagnosis()
        viewModel.onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.CONCERN_NOT_READY))
        viewModel.onEvent(OnboardingEvent.AnswerQuiz(0))
        viewModel.onEvent(OnboardingEvent.GoToPreviousStep)

        viewModel.onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.CONCERN_NOT_READY))

        assertThat(viewModel.uiState.value.data.quizAnswers).hasSize(1)
    }

    @Test
    fun `the finished flow reports the test score and the exam timing`() = runTest {
        advanceToSocialProof()
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        advanceUntilIdle()
        // Plan reveal, then the pact.
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        advanceUntilIdle()

        val profile = fakeAnalyticsTracker.completedProfile!!
        assertThat(profile["quiz_score"]).isEqualTo("3/3")
        assertThat(profile["exam_timing"]).isEqualTo("soon")
        assertThat(profile["exam_days_left"]).isEqualTo(EXAM_IN_DAYS.toString())
        assertThat(profile["learning_preference"]).isEqualTo(OnboardingConfig.STYLE_MOCK_EXAMS.key)
        assertThat(profile).doesNotContainKey("future_impact")
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
        assertThat(fakeAnalyticsTracker.onboardingSteps).containsExactly("o01_intro")
    }

    @Test
    fun `every screen reached emits exactly one funnel event, numbered in order`() = runTest {
        advanceThroughDiagnosis()

        assertThat(fakeAnalyticsTracker.onboardingSteps)
            .containsExactly(
                "o01_intro",
                "o02_motivation",
                "o03_reasons",
                "o04_pain",
                "o05_experience",
                "o06_comparison",
                "o07_readiness",
                "o08_concern"
            )
            .inOrder()
    }

    @Test
    fun `a screen the user only lands on is still counted`() = runTest {
        advanceThroughDiagnosis()

        // The user goes no further: the screen must appear even though it was never answered.
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Concern)
        assertThat(fakeAnalyticsTracker.onboardingSteps).contains("o08_concern")
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
            .containsExactly("o16_weekly_study", "o17_study_time", "o18_notifications")
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

    @Test
    fun `there is no way back from the first screen`() = runTest {
        assertThat(viewModel.uiState.value.canGoBack).isFalse()

        viewModel.onEvent(OnboardingEvent.GoToPreviousStep)

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Intro)
    }

    @Test
    fun `going back returns to the previous question and keeps its answer`() = runTest {
        startQuestions()
        val motivation = OnboardingConfig.motivations.first()
        viewModel.onEvent(OnboardingEvent.SelectMotivation(motivation))
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.TheoryBlocker)
        assertThat(viewModel.uiState.value.canGoBack).isTrue()

        viewModel.onEvent(OnboardingEvent.GoToPreviousStep)

        val state = viewModel.uiState.value
        assertThat(state.currentStep).isEqualTo(OnboardingStep.Motivation)
        assertThat(state.data.motivation).isEqualTo(motivation)
        // The intro is still behind it.
        assertThat(state.canGoBack).isTrue()
        assertThat(state.mascotMessage)
            .isEqualTo(OnboardingReducer().updateMascotMessage(OnboardingStep.Motivation, state.data))
    }

    @Test
    fun `answering again after going back replaces the earlier answer`() = runTest {
        startQuestions()
        viewModel.onEvent(OnboardingEvent.SelectMotivation(OnboardingConfig.motivations.first()))
        viewModel.onEvent(OnboardingEvent.GoToPreviousStep)

        viewModel.onEvent(OnboardingEvent.SelectMotivation(OnboardingConfig.motivations.last()))

        assertThat(viewModel.uiState.value.data.motivation).isEqualTo(OnboardingConfig.motivations.last())
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.TheoryBlocker)
    }

    @Test
    fun `the screens slide backwards only while going back`() = runTest {
        startQuestions()
        viewModel.onEvent(OnboardingEvent.SelectMotivation(OnboardingConfig.motivations.first()))
        assertThat(viewModel.uiState.value.isMovingBack).isFalse()

        viewModel.onEvent(OnboardingEvent.GoToPreviousStep)
        assertThat(viewModel.uiState.value.isMovingBack).isTrue()

        viewModel.onEvent(OnboardingEvent.SelectMotivation(OnboardingConfig.motivations.first()))
        assertThat(viewModel.uiState.value.isMovingBack).isFalse()
    }

    @Test
    fun `coming back to a screen does not count it twice in the funnel`() = runTest {
        startQuestions()
        viewModel.onEvent(OnboardingEvent.SelectMotivation(OnboardingConfig.motivations.first()))
        viewModel.onEvent(OnboardingEvent.GoToPreviousStep)
        viewModel.onEvent(OnboardingEvent.SelectMotivation(OnboardingConfig.motivations.last()))

        assertThat(fakeAnalyticsTracker.onboardingSteps)
            .containsExactly("o01_intro", "o02_motivation", "o03_reasons")
            .inOrder()
    }

    @Test
    fun `going back from the plan reveal skips the plan being built`() = runTest {
        advanceToSocialProof()
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.PlanReveal)

        viewModel.onEvent(OnboardingEvent.GoToPreviousStep)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.SocialProof)

        // Moving on builds the plan again instead of jumping straight to the reveal.
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Processing)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.PlanReveal)
    }

    @Test
    fun `going back is ignored while the plan is being built`() = runTest {
        advanceToSocialProof()
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Processing)
        assertThat(viewModel.uiState.value.canGoBack).isFalse()

        viewModel.onEvent(OnboardingEvent.GoToPreviousStep)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Processing)

        // The plan still finishes and carries on to the reveal.
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.PlanReveal)
    }

    @Test
    fun `the reminder offer can be answered again after coming back to it`() = runTest {
        advanceToNotifications()
        viewModel.onEvent(OnboardingEvent.AnswerNotifications(accepted = false))
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.LearningPreference)

        viewModel.onEvent(OnboardingEvent.GoToPreviousStep)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Notifications)
        viewModel.onEvent(OnboardingEvent.AnswerNotifications(accepted = true))

        assertThat(fakeNotificationsRepository.permissionRequests).isEqualTo(1)
        assertThat(viewModel.uiState.value.data.notifications).isEqualTo(NotificationsAnswer.GRANTED)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.LearningPreference)
    }

    @Test
    fun `going back is ignored while the permission dialog is open`() = runTest {
        val dialog = CompletableDeferred<Boolean>()
        val dialogOpen = object : NotificationsRepository by FakeNotificationsRepository() {
            override suspend fun requestPermission(): Boolean = dialog.await()
        }
        viewModel = OnboardingViewModel(fakeUserRepository, fakeAnalyticsTracker, dialogOpen, fakeSoundEffects)
        advanceToNotifications()

        viewModel.onEvent(OnboardingEvent.AnswerNotifications(accepted = true))
        assertThat(viewModel.uiState.value.isRequestingNotifications).isTrue()

        viewModel.onEvent(OnboardingEvent.GoToPreviousStep)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.Notifications)

        // The answer lands on the screen that asked, not on the one the user tried to reach.
        dialog.complete(true)
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.LearningPreference)
    }

    /**
     * Answers everything up to the study rhythm question, leaving the flow on
     * [OnboardingStep.WeeklyStudy].
     */
    private fun advanceToWeeklyStudy() {
        advanceThroughArc()
        viewModel.onEvent(OnboardingEvent.SetName("Jesus"))
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        viewModel.onEvent(OnboardingEvent.SetExamDate(examDayPickerMillis()))
        viewModel.onEvent(OnboardingEvent.SelectProvince("Almería"))
        // The province waits for the bottom button.
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
    }

    /** Answers everything up to the exam date, leaving the flow on [OnboardingStep.ExamDate]. */
    private fun advanceToExamDate() {
        advanceThroughArc()
        viewModel.onEvent(OnboardingEvent.SetName("Jesus"))
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
    }

    /** The exam day used throughout, as the date picker hands it over. */
    private fun examDayPickerMillis(): Long =
        pickerMillisFromLocalDay(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(EXAM_IN_DAYS.toLong()))

    /**
     * Answers every question of the mini-test and moves past the last one.
     *
     * @param correct whether to tap the right option every time
     */
    private fun answerQuiz(correct: Boolean) {
        viewModel.uiState.value.data.quizQuestions().forEach { question ->
            val option = if (correct) question.correctIndex else (question.correctIndex + 1) % question.options.size
            viewModel.onEvent(OnboardingEvent.AnswerQuiz(option))
            viewModel.onEvent(OnboardingEvent.NextQuizQuestion)
        }
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

    /**
     * Answers everything up to the last question, leaving the flow on
     * [OnboardingStep.SocialProof], one tap away from the plan being built.
     */
    private fun advanceToSocialProof() {
        advanceToNotifications()
        viewModel.onEvent(OnboardingEvent.AnswerNotifications(accepted = false))
        viewModel.onEvent(OnboardingEvent.SelectLearningPreference(OnboardingConfig.STYLE_MOCK_EXAMS.key))
    }

    /**
     * Answers the diagnosis block, leaving the flow on [OnboardingStep.Concern], the question
     * that picks the mini-test.
     *
     * @param motivation the answer to the first question
     */
    private fun advanceThroughDiagnosis(motivation: String = OnboardingConfig.motivations.first()) {
        startQuestions()
        viewModel.onEvent(OnboardingEvent.SelectMotivation(motivation))
        viewModel.onEvent(OnboardingEvent.SelectTheoryBlocker(OnboardingConfig.theoryBlockers.first()))
        // The pain is informational and needs the bottom button.
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        viewModel.onEvent(OnboardingEvent.SelectExperience(OnboardingConfig.experiences.first()))
        // Comparison is informational and needs the bottom button.
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
        viewModel.onEvent(OnboardingEvent.SelectReadiness(OnboardingConfig.readinessLevels.first()))
    }

    /**
     * Picks a worry and answers its mini-test right, leaving the flow on
     * [OnboardingStep.MethodComparison].
     */
    private fun advanceThroughQuiz() {
        advanceThroughDiagnosis()
        viewModel.onEvent(OnboardingEvent.SelectConcern(OnboardingConfig.concerns.first()))
        answerQuiz(correct = true)
        // The result is informational and needs the bottom button.
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
    }

    /**
     * Answers everything up to and including the gain block, leaving the flow on
     * [OnboardingStep.Name] — the first screen that asks the user to type.
     */
    private fun advanceThroughArc() {
        advanceThroughQuiz()
        // The road to the exam and the gain are tap-to-continue.
        repeat(2) { viewModel.onEvent(OnboardingEvent.GoToNextStep) }
    }

    /** Leaves the intro, landing on the first question. */
    private fun startQuestions() {
        viewModel.onEvent(OnboardingEvent.GoToNextStep)
    }

    private companion object {
        /** How far ahead the exam is in these tests: inside the "soon" bucket. */
        const val EXAM_IN_DAYS = 10
    }
}
