package com.jesuskrastev.bali.ui.screens.test

import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.ui.screens.auth.FakeAnswerRepository
import com.jesuskrastev.bali.ui.screens.auth.FakePathRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeTestResultRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import kotlinx.coroutines.flow.collect
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
            pathRepository = fakePathRepository
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
}
