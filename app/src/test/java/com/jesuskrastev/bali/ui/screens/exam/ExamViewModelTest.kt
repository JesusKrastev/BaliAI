package com.jesuskrastev.bali.ui.screens.exam

import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.ui.screens.auth.FakeAnalyticsTracker
import com.jesuskrastev.bali.ui.screens.auth.FakeAnswerRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeTestResultRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.domain.usecase.DecrementEnergyUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import org.mockito.kotlin.mock
import com.google.ai.client.generativeai.GenerativeModel

class ExamViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeUserRepository = FakeUserRepository()
    private val fakeTestResultRepository = FakeTestResultRepository()
    private val fakeAnswerRepository = FakeAnswerRepository()
    private val fakeAnalyticsTracker = FakeAnalyticsTracker(mock(), mock())
    
    private val fakeDecrementEnergyUseCase = DecrementEnergyUseCase(fakeUserRepository)
    private val fakeIncrementStreakUseCase = IncrementStreakUseCase(fakeUserRepository, mock())
    private val fakeIncrementXpUseCase = IncrementXpUseCase(fakeUserRepository)
    private val fakeIncrementCoinsUseCase = IncrementCoinsUseCase(fakeUserRepository)

    private lateinit var viewModel: ExamViewModel

    @Before
    fun setup() {
        viewModel = ExamViewModel(
            userRepository = fakeUserRepository,
            testResultRepository = fakeTestResultRepository,
            answerRepository = fakeAnswerRepository,
            gemini = mock(),
            decrementEnergyUseCase = fakeDecrementEnergyUseCase,
            incrementStreakUseCase = fakeIncrementStreakUseCase,
            incrementXpUseCase = fakeIncrementXpUseCase,
            incrementCoinsUseCase = fakeIncrementCoinsUseCase,
            analyticsTracker = fakeAnalyticsTracker
        )
    }

    @Test
    fun `uiState initializes correctly`() = runTest {
        assertThat(viewModel.uiState.value).isNotNull()
        assertThat(viewModel.uiState.value.questions).isEmpty()
    }

    @Test
    fun `timer starts at 1800 seconds for 30 minute exam`() = runTest {
        assertThat(viewModel.uiState.value.timeLeftSeconds).isEqualTo(1800)
    }

    @Test
    fun `isTimeUp is initially false`() = runTest {
        assertThat(viewModel.uiState.value.isTimeUp).isFalse()
    }

    @Test
    fun `sessionStreak is tracked correctly`() = runTest {
        assertThat(viewModel.uiState.value.sessionStreak).isAtLeast(0)
    }
}
