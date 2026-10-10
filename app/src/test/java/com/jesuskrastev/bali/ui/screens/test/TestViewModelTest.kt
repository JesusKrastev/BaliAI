package com.jesuskrastev.bali.ui.screens.test

import androidx.lifecycle.SavedStateHandle
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.util.FakeSoundEffects
import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.ui.screens.auth.FakeAnswerRepository
import com.jesuskrastev.bali.ui.screens.auth.FakePathRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeTestResultRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.domain.usecase.CompleteFirstStepUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import com.jesuskrastev.bali.domain.util.PendingFirstStepRewards
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerateContentResponse
import com.jesuskrastev.bali.domain.model.FirstStepTask
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import com.jesuskrastev.bali.domain.usecase.CompletePathNodeUseCase
import com.jesuskrastev.bali.util.FakeImagePrefetcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TestViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val fakeUserRepository = FakeUserRepository()
    private val fakeTestResultRepository = FakeTestResultRepository()
    private val fakeAnswerRepository = FakeAnswerRepository()
    private val fakePathRepository = FakePathRepository()
    private val mockGemini: GenerativeModel = mock()
    private val fakeSoundEffects = FakeSoundEffects()

    private val fakeIncrementStreakUseCase = IncrementStreakUseCase(fakeUserRepository)
    private val fakeIncrementXpUseCase = IncrementXpUseCase(fakeUserRepository)
    private val fakeIncrementCoinsUseCase = IncrementCoinsUseCase(fakeUserRepository)
    private val pendingRewards = PendingFirstStepRewards()
    private val fakeCompleteFirstStepUseCase = CompleteFirstStepUseCase(fakeUserRepository, pendingRewards)

    private lateinit var viewModel: TestViewModel

    @Before
    fun setup() {
        viewModel = TestViewModel(
            userRepository = fakeUserRepository,
            testResultRepository = fakeTestResultRepository,
            answerRepository = fakeAnswerRepository,
            gemini = mockGemini,
            incrementStreakUseCase = fakeIncrementStreakUseCase,
            incrementXpUseCase = fakeIncrementXpUseCase,
            incrementCoinsUseCase = fakeIncrementCoinsUseCase,
            completeFirstStepUseCase = fakeCompleteFirstStepUseCase,
            pathRepository = fakePathRepository,
            imagePrefetcher = FakeImagePrefetcher(),
            completePathNodeUseCase = CompletePathNodeUseCase(fakePathRepository),
            analytics = mock<AnalyticsTracker>(),
            soundEffects = fakeSoundEffects,
            savedStateHandle = SavedStateHandle()
        )
    }

    /** Loads a lesson from the static question bank and returns its first question. */
    private fun loadLessonFirstQuestion(): QuestionUiState {
        // node_0_0_lesson exists in LessonQuestionBank
        viewModel.setAiNodeParams("Conductor", "Factores", "node_0_0_lesson", "LESSON")
        return viewModel.uiState.value.questions.first()
    }

    @Test
    fun `uiState initializes with empty state`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        assertThat(viewModel.uiState.value).isNotNull()
        collectJob.cancel()
    }

    @Test
    fun `setTopic updates the topic correctly`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        
        val mockResponse: GenerateContentResponse = mock()
        whenever(mockResponse.text).thenReturn(
            "{\"selectedCategory\": \"Spanish\", \"questions\": [" +
                "{\"text\": \"¿Pregunta?\", \"options\": [\"a\", \"b\", \"c\"], " +
                "\"correctAnswerIndex\": 0, \"explanation\": \"Porque sí.\"}]}"
        )
        whenever(mockGemini.generateContent(any<String>())).thenReturn(mockResponse)

        viewModel.setTopic("Spanish")
        assertThat(viewModel.uiState.value.category).isEqualTo("Spanish")
        collectJob.cancel()
    }

    @Test
    fun `setAiNodeParams updates UI state correctly`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        // node_0_0_lesson exists in LessonQuestionBank
        viewModel.setAiNodeParams("Conductor", "Factores", "node_0_0_lesson", "LESSON")
        assertThat(viewModel.uiState.value.category).isEqualTo("Conductor")
        collectJob.cancel()
    }

    @Test
    fun `finishing a test completes the first-test step for an enrolled account`() = runTest {
        fakeUserRepository.enrollInFirstStepsForTest()
        viewModel.setAiNodeParams("Conductor", "Factores", "node_0_0_lesson", "LESSON")
        val summary = CompletableDeferred<TestSummary>()

        viewModel.onEvent(TestEvent.FinishTest { summary.complete(it) })

        // calculateResult saves on Dispatchers.IO and then resumes on Main. Wait for it for
        // real (the timeout runs on a real dispatcher, not on runTest's virtual clock): if the
        // test ended first, that tail would resume after MainDispatcherRule reset Main and leak
        // an exception into whichever test runs next.
        withContext(Dispatchers.Default) { withTimeout(10_000) { summary.await() } }

        val user = fakeUserRepository.get().first()!!
        assertThat(user.firstSteps.completed).containsExactly(FirstStepTask.FIRST_TEST)
        assertThat(pendingRewards.next.first()?.task).isEqualTo(FirstStepTask.FIRST_TEST)
    }

    @Test
    fun `checking a right answer plays the correct sound only`() {
        val question = loadLessonFirstQuestion()

        viewModel.onEvent(TestEvent.SelectOption(question.correctAnswerIndex))
        viewModel.onEvent(TestEvent.CheckAnswer)

        assertThat(fakeSoundEffects.correctPlays).isEqualTo(1)
        assertThat(fakeSoundEffects.wrongPlays).isEqualTo(0)
        assertThat(viewModel.uiState.value.isAnswerChecked).isTrue()
    }

    @Test
    fun `checking a wrong answer plays the wrong sound only`() {
        val question = loadLessonFirstQuestion()
        val wrongOption = (question.correctAnswerIndex + 1) % question.options.size

        viewModel.onEvent(TestEvent.SelectOption(wrongOption))
        viewModel.onEvent(TestEvent.CheckAnswer)

        assertThat(fakeSoundEffects.wrongPlays).isEqualTo(1)
        assertThat(fakeSoundEffects.correctPlays).isEqualTo(0)
    }

    @Test
    fun `checking with no option selected plays nothing`() {
        loadLessonFirstQuestion()

        viewModel.onEvent(TestEvent.CheckAnswer)

        assertThat(fakeSoundEffects.correctPlays).isEqualTo(0)
        assertThat(fakeSoundEffects.wrongPlays).isEqualTo(0)
        assertThat(viewModel.uiState.value.isAnswerChecked).isFalse()
    }

    @Test
    fun `a double tap on check plays once and counts the streak once`() {
        val question = loadLessonFirstQuestion()

        viewModel.onEvent(TestEvent.SelectOption(question.correctAnswerIndex))
        viewModel.onEvent(TestEvent.CheckAnswer)
        viewModel.onEvent(TestEvent.CheckAnswer)

        assertThat(fakeSoundEffects.correctPlays).isEqualTo(1)
        assertThat(viewModel.uiState.value.sessionStreak).isEqualTo(1)
    }
}
