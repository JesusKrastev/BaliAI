package com.jesuskrastev.bali.ui.screens.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.ui.screens.auth.FakeAnalyticsTracker
import com.jesuskrastev.bali.ui.screens.auth.FakeAnswerRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeAuthRepository
import com.jesuskrastev.bali.ui.screens.auth.FakePathRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeTestResultRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.domain.util.DateTimeHelper
import com.jesuskrastev.bali.domain.usecase.DecrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.GenerateInitialPathUseCase
import com.jesuskrastev.bali.domain.usecase.GenerateNextPathNodesUseCase
import com.jesuskrastev.bali.domain.usecase.RestoreEnergyUseCase
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val fakeUserRepository = FakeUserRepository()
    private val fakeTestResultRepository = FakeTestResultRepository()
    private val fakeAnswerRepository = FakeAnswerRepository()
    private val fakeAuthRepository = FakeAuthRepository()
    private val fakePathRepository = FakePathRepository()
    private val fakeAnalyticsTracker = FakeAnalyticsTracker(mock(), mock())
    
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dateTimeHelper: DateTimeHelper = mock()
    
    private val fakeDecrementCoinsUseCase = DecrementCoinsUseCase(fakeUserRepository)
    private val fakeRestoreEnergyUseCase = RestoreEnergyUseCase(fakeUserRepository)
    private val fakeGenerateInitialPathUseCase = GenerateInitialPathUseCase(fakePathRepository, fakeUserRepository)
    private val fakeGenerateNextPathNodesUseCase = GenerateNextPathNodesUseCase(mock(), fakeUserRepository, fakePathRepository)

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        viewModel = HomeViewModel(
            userRepository = fakeUserRepository,
            testResultRepository = fakeTestResultRepository,
            answerRepository = fakeAnswerRepository,
            decrementCoinsUseCase = fakeDecrementCoinsUseCase,
            authRepository = fakeAuthRepository,
            pathRepository = fakePathRepository,
            generateNextPathNodesUseCase = fakeGenerateNextPathNodesUseCase,
            generateInitialPathUseCase = fakeGenerateInitialPathUseCase,
            analyticsTracker = fakeAnalyticsTracker,
            dateTimeHelper = dateTimeHelper,
            restoreEnergyUseCase = fakeRestoreEnergyUseCase,
            context = context
        )
    }

    @Test
    fun `isLoggedIn state is correctly reflected`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        assertThat(viewModel.uiState.value.isLoggedIn).isTrue()
        collectJob.cancel()
    }

    @Test
    fun `showEnergyDialog can be toggled`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        assertThat(viewModel.uiState.value.showEnergyDialog).isFalse()
        viewModel.showEnergyDialog()
        assertThat(viewModel.uiState.value.showEnergyDialog).isTrue()
        collectJob.cancel()
    }

    @Test
    fun `showNoCoinsDialog can be triggered by startExam`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        // User starts with 500 coins in FakeUserRepository, so 100 coin exam should work.
        // Let's set coins to 0 to trigger the dialog.
        fakeUserRepository.updateCoins(0)
        assertThat(viewModel.uiState.value.showNoCoinsDialog).isFalse()
        viewModel.startExam {}
        assertThat(viewModel.uiState.value.showNoCoinsDialog).isTrue()
        collectJob.cancel()
    }
}
