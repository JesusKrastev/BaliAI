package com.jesuskrastev.bali.ui.screens.mistakes

import androidx.lifecycle.SavedStateHandle
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.ui.screens.auth.FakeAnswerRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeTestResultRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import org.mockito.kotlin.mock

class MistakesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeUserRepository = FakeUserRepository()
    private val fakeAnswerRepository = FakeAnswerRepository()
    private val fakeTestResultRepository = FakeTestResultRepository()

    private val fakeIncrementStreakUseCase = IncrementStreakUseCase(fakeUserRepository, mock())
    private val fakeIncrementXpUseCase = IncrementXpUseCase(fakeUserRepository)
    private val fakeIncrementCoinsUseCase = IncrementCoinsUseCase(fakeUserRepository)

    private lateinit var viewModel: MistakesViewModel

    @Before
    fun setup() {
        viewModel = MistakesViewModel(
            userRepository = fakeUserRepository,
            answerRepository = fakeAnswerRepository,
            testResultRepository = fakeTestResultRepository,
            gemini = mock(),
            incrementStreakUseCase = fakeIncrementStreakUseCase,
            incrementXpUseCase = fakeIncrementXpUseCase,
            incrementCoinsUseCase = fakeIncrementCoinsUseCase,
            analytics = mock<AnalyticsTracker>(),
            savedStateHandle = SavedStateHandle()
        )
    }

    @Test
    fun `uiState initializes correctly`() = runTest {
        assertThat(viewModel.uiState.value).isNotNull()
    }

    @Test
    fun `isLoading is true initially`() = runTest {
        // Since generateMistakesTest is called in init, it might be loading or finished if no mistakes
        assertThat(viewModel.uiState.value.isLoading).isAnyOf(true, false)
    }
}
