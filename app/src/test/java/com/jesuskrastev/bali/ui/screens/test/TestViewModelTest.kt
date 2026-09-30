package com.jesuskrastev.bali.ui.screens.test

import androidx.lifecycle.SavedStateHandle
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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

    private val fakeIncrementStreakUseCase = IncrementStreakUseCase(fakeUserRepository, mock())
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
            analytics = mock<AnalyticsTracker>(),
            savedStateHandle = SavedStateHandle()
        )
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
        whenever(mockResponse.text).thenReturn("{\"selectedCategory\": \"Spanish\", \"questions\": []}")
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

        // The reward is granted before calculateResult hops to the IO dispatcher to save the
        // result, so it has already happened by the time onEvent returns.
        viewModel.onEvent(TestEvent.FinishTest { })

        val user = fakeUserRepository.get().first()!!
        assertThat(user.firstSteps.completed).containsExactly(FirstStepTask.FIRST_TEST)
        assertThat(pendingRewards.next.first()?.task).isEqualTo(FirstStepTask.FIRST_TEST)
    }
}
