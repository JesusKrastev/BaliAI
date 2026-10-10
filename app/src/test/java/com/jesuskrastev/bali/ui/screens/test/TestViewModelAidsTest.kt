package com.jesuskrastev.bali.ui.screens.test

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.model.AnswerMode
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.usecase.CompleteFirstStepUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import com.jesuskrastev.bali.domain.util.PendingFirstStepRewards
import com.jesuskrastev.bali.domain.util.QuestionId
import com.jesuskrastev.bali.ui.screens.auth.FakePathRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeTestResultRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.util.FakeSoundEffects
import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.util.RecordingAnswerRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import com.jesuskrastev.bali.domain.usecase.CompletePathNodeUseCase
import com.jesuskrastev.bali.util.FakeImagePrefetcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

/** The practice aids bought in the shop (hints and 50/50) inside [TestViewModel]. */
class TestViewModelAidsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val answers = RecordingAnswerRepository()
    private val userRepository = FakeUserRepository()
    private lateinit var viewModel: TestViewModel

    /** Built here, not as a property: `viewModelScope` needs the rule's `Dispatchers.Main` set first. */
    @Before
    fun setup() {
        runBlocking { userRepository.insert(User(hints = 2, fiftyFifties = 2)) }
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

    /** Opens the first question of a lesson from the static question bank. */
    private fun openFirstQuestion(): QuestionUiState {
        viewModel.setAiNodeParams("Conductor", "Factores", "node_0_0_lesson", "LESSON")
        return viewModel.uiState.value.questions.first()
    }

    private fun owned(): User = runBlocking { userRepository.get().first()!! }

    @Test
    fun `a 50-50 leaves the correct option and one incorrect option and spends one`() {
        val question = openFirstQuestion()

        viewModel.onEvent(TestEvent.UseFiftyFifty)

        val state = viewModel.uiState.value
        val visible = question.options.indices.filter { it !in state.eliminatedOptionIndices }
        assertThat(visible).hasSize(2)
        assertThat(visible).contains(question.correctAnswerIndex)
        assertThat(owned().fiftyFifties).isEqualTo(1)
        assertThat(state.fiftyFifties).isEqualTo(1)
    }

    @Test
    fun `a 50-50 keeps an incorrect option the user had already picked`() {
        val question = openFirstQuestion()
        val picked = question.options.indices.first { it != question.correctAnswerIndex }
        viewModel.onEvent(TestEvent.SelectOption(picked))

        viewModel.onEvent(TestEvent.UseFiftyFifty)

        assertThat(viewModel.uiState.value.eliminatedOptionIndices).doesNotContain(picked)
    }

    @Test
    fun `a second 50-50 on the same question is not spent`() {
        openFirstQuestion()

        viewModel.onEvent(TestEvent.UseFiftyFifty)
        viewModel.onEvent(TestEvent.UseFiftyFifty)

        assertThat(owned().fiftyFifties).isEqualTo(1)
    }

    @Test
    fun `a hint shows the explanation, spends one and clears on the next question`() {
        openFirstQuestion()

        viewModel.onEvent(TestEvent.UseHint)
        viewModel.onEvent(TestEvent.UseHint)

        assertThat(viewModel.uiState.value.isHintVisible).isTrue()
        assertThat(owned().hints).isEqualTo(1)

        viewModel.onEvent(TestEvent.NextQuestion)

        assertThat(viewModel.uiState.value.isHintVisible).isFalse()
        assertThat(viewModel.uiState.value.eliminatedOptionIndices).isEmpty()
    }

    @Test
    fun `aids cannot be used once the answer has been checked`() {
        val question = openFirstQuestion()
        viewModel.onEvent(TestEvent.SelectOption(question.correctAnswerIndex))
        viewModel.onEvent(TestEvent.CheckAnswer)

        viewModel.onEvent(TestEvent.UseHint)
        viewModel.onEvent(TestEvent.UseFiftyFifty)

        assertThat(owned().hints).isEqualTo(2)
        assertThat(owned().fiftyFifties).isEqualTo(2)
    }

    @Test
    fun `an answer given with aids is still saved with its question id, topic and mode`() {
        val question = openFirstQuestion()
        viewModel.onEvent(TestEvent.UseHint)
        viewModel.onEvent(TestEvent.UseFiftyFifty)
        viewModel.onEvent(TestEvent.SelectOption(question.correctAnswerIndex))

        runBlocking {
            val finished = CompletableDeferred<TestSummary>()
            viewModel.onEvent(TestEvent.FinishTest { finished.complete(it) })
            withTimeout(10_000) { finished.await() }
        }

        val saved = answers.inserted.single()
        assertThat(saved.questionId).isEqualTo(QuestionId.of(question.text))
        assertThat(saved.mode).isEqualTo(AnswerMode.LESSON)
        assertThat(saved.topic).isNotNull()
    }
}
