package com.jesuskrastev.bali.ui.screens.test

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.model.AnswerMode
import com.jesuskrastev.bali.domain.model.DrivingTopic
import com.jesuskrastev.bali.domain.usecase.CompleteFirstStepUseCase
import com.jesuskrastev.bali.domain.util.PendingFirstStepRewards
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import com.jesuskrastev.bali.domain.util.QuestionId
import com.jesuskrastev.bali.ui.screens.auth.FakePathRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeTestResultRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.util.FakeSoundEffects
import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.util.RecordingAnswerRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import com.jesuskrastev.bali.domain.usecase.CompletePathNodeUseCase
import com.jesuskrastev.bali.util.FakeImagePrefetcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

/** What [TestViewModel] saves for each answer when a lesson is finished. */
class TestViewModelAnswersTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val answers = RecordingAnswerRepository()
    private val userRepository = FakeUserRepository()
    private lateinit var viewModel: TestViewModel

    /** Built here, not as a property: `viewModelScope` needs the rule's `Dispatchers.Main` set first. */
    @Before
    fun setup() {
        viewModel = TestViewModel(
            userRepository = userRepository,
            testResultRepository = FakeTestResultRepository(),
            answerRepository = answers,
            gemini = mock(),
            incrementStreakUseCase = IncrementStreakUseCase(userRepository),
            incrementXpUseCase = IncrementXpUseCase(userRepository),
            incrementCoinsUseCase = IncrementCoinsUseCase(userRepository),
            completeFirstStepUseCase = CompleteFirstStepUseCase(userRepository, PendingFirstStepRewards()),
            pathRepository = FakePathRepository(),
            imagePrefetcher = FakeImagePrefetcher(),
            completePathNodeUseCase = CompletePathNodeUseCase(FakePathRepository()),
            analytics = mock<AnalyticsTracker>(),
            soundEffects = FakeSoundEffects(),
            savedStateHandle = SavedStateHandle()
        )
    }

    /** Finishes the test and waits for the IO work that saves the answers. */
    private fun finishTest() = runBlocking {
        val finished = CompletableDeferred<TestSummary>()
        viewModel.onEvent(TestEvent.FinishTest { finished.complete(it) })
        withTimeout(10_000) { finished.await() }
    }

    @Test
    fun `a lesson answer is saved with its question id, topic and the lesson mode`() {
        // node_0_0_lesson exists in LessonQuestionBank
        viewModel.setAiNodeParams("Conductor", "Factores", "node_0_0_lesson", "LESSON")
        val question = viewModel.uiState.value.questions.first()
        viewModel.onEvent(TestEvent.SelectOption(question.correctAnswerIndex))

        finishTest()

        val saved = answers.inserted.single()
        assertThat(saved.questionId).isEqualTo(QuestionId.of(question.text))
        assertThat(saved.topic).isEqualTo(DrivingTopic.fromCategory("Conductor"))
        assertThat(saved.topic).isNotNull()
        assertThat(saved.mode).isEqualTo(AnswerMode.LESSON)
        assertThat(saved.isCorrect).isTrue()
    }

    @Test
    fun `only the questions that were answered are saved`() {
        viewModel.setAiNodeParams("Conductor", "Factores", "node_0_0_lesson", "LESSON")
        val questions = viewModel.uiState.value.questions
        assertThat(questions.size).isGreaterThan(1)
        viewModel.onEvent(TestEvent.SelectOption(0))

        finishTest()

        assertThat(answers.inserted).hasSize(1)
        assertThat(answers.inserted.single().questionId).isEqualTo(QuestionId.of(questions.first().text))
    }
}
