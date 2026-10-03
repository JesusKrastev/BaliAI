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
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.domain.model.FirstStepReward
import com.jesuskrastev.bali.domain.model.FirstStepTask
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.util.PendingFirstStepRewards
import com.jesuskrastev.bali.domain.usecase.GenerateInitialPathUseCase
import com.jesuskrastev.bali.domain.usecase.GenerateNextPathNodesUseCase
import com.jesuskrastev.bali.domain.usecase.SettleStreakUseCase
import com.jesuskrastev.bali.data.remote.RemoteConfigProvider
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
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val fakeUserRepository = FakeUserRepository()
    private val fakeAnswerRepository = FakeAnswerRepository()
    private val fakeAuthRepository = FakeAuthRepository()
    private val fakePathRepository = FakePathRepository()
    private val fakeAnalyticsTracker = FakeAnalyticsTracker(mock(), mock())

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val fakeGenerateInitialPathUseCase = GenerateInitialPathUseCase(fakePathRepository, fakeAuthRepository)
    private val fakeGenerateNextPathNodesUseCase = GenerateNextPathNodesUseCase(mock(), fakeUserRepository, fakePathRepository)
    private val remoteConfigProvider: RemoteConfigProvider = mock()
    private val pendingRewards = PendingFirstStepRewards()

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        viewModel = createViewModel()
    }

    /**
     * Builds a [HomeViewModel] over the shared fakes.
     *
     * @param testResults results the account already has, to tell whether it took the first simulacro
     * @return the view model under test
     */
    private fun createViewModel(testResults: List<TestResult> = emptyList()) =
        HomeViewModel(
            userRepository = fakeUserRepository,
            testResultRepository = FakeTestResultRepository(testResults),
            answerRepository = fakeAnswerRepository,
            authRepository = fakeAuthRepository,
            pathRepository = fakePathRepository,
            generateNextPathNodesUseCase = fakeGenerateNextPathNodesUseCase,
            generateInitialPathUseCase = fakeGenerateInitialPathUseCase,
            analyticsTracker = fakeAnalyticsTracker,
            remoteConfigProvider = remoteConfigProvider,
            settleStreak = SettleStreakUseCase(fakeUserRepository),
            pendingFirstStepRewards = pendingRewards,
            context = context
        )

    /** @return a passed official exam, i.e. the first simulacro already taken */
    private fun officialExam() = TestResult(
        category = ExamRules.OFFICIAL_EXAM_CATEGORY,
        score = 27,
        total = 30,
        date = Date(),
        isPassed = true
    )

    @Test
    fun `the coin balance on Home follows the user profile`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }

        fakeUserRepository.setCoinsForTest(250)

        assertThat(viewModel.uiState.value.coinsCount).isEqualTo(250)
        collectJob.cancel()
    }

    @Test
    fun `the first-steps bar stays hidden for an account that was never enrolled`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }

        assertThat(viewModel.uiState.value.firstSteps).isNull()
        collectJob.cancel()
    }

    @Test
    fun `an enrolled account sees its progress in the first-steps bar`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }

        fakeUserRepository.enrollInFirstStepsForTest(setOf(FirstStepTask.FIRST_TEST))

        val progress = viewModel.uiState.value.firstSteps
        assertThat(progress).isNotNull()
        assertThat(progress!!.completed).containsExactly(FirstStepTask.FIRST_TEST)
        assertThat(progress.doneCount).isEqualTo(1)
        collectJob.cancel()
    }

    @Test
    fun `a task completed on another device shows up in the bar without a celebration`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        fakeUserRepository.enrollInFirstStepsForTest()

        // The account document changes underneath Home, as a snapshot from another phone would.
        fakeUserRepository.enrollInFirstStepsForTest(setOf(FirstStepTask.PLAY_GAME))

        assertThat(viewModel.uiState.value.firstSteps?.completed).containsExactly(FirstStepTask.PLAY_GAME)
        assertThat(viewModel.uiState.value.firstStepReward).isNull()
        collectJob.cancel()
    }

    @Test
    fun `dismissing the bar hides it for good and reports how far the student got`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        fakeUserRepository.enrollInFirstStepsForTest(setOf(FirstStepTask.ASK_BALI))

        viewModel.dismissFirstSteps()

        assertThat(viewModel.uiState.value.firstSteps).isNull()
        assertThat(fakeAnalyticsTracker.firstStepsDismissedEvents).containsExactly(1)
        collectJob.cancel()
    }

    @Test
    fun `with every task done the bar stays until the first simulacro is taken`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }

        fakeUserRepository.enrollInFirstStepsForTest(FirstStepTask.entries.toSet())

        assertThat(viewModel.uiState.value.firstSteps?.isComplete).isTrue()
        collectJob.cancel()
    }

    @Test
    fun `with every task done and the simulacro taken the bar is gone`() = runTest {
        val viewModelWithExam = createViewModel(testResults = listOf(officialExam()))
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModelWithExam.uiState.collect {} }

        fakeUserRepository.enrollInFirstStepsForTest(FirstStepTask.entries.toSet())

        assertThat(viewModelWithExam.uiState.value.firstSteps).isNull()
        collectJob.cancel()
    }

    @Test
    fun `a simulacro taken early does not take away the tasks still pending`() = runTest {
        val viewModelWithExam = createViewModel(testResults = listOf(officialExam()))
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModelWithExam.uiState.collect {} }

        fakeUserRepository.enrollInFirstStepsForTest(setOf(FirstStepTask.FIRST_TEST))

        assertThat(viewModelWithExam.uiState.value.firstSteps).isNotNull()
        collectJob.cancel()
    }

    @Test
    fun `a reward queued by another screen is celebrated once and then cleared`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        val reward = FirstStepReward(FirstStepTask.FIRST_TEST, coins = 30, completedAll = false)

        pendingRewards.publish(reward)
        assertThat(viewModel.uiState.value.firstStepReward).isEqualTo(reward)

        viewModel.dismissFirstStepReward()
        assertThat(viewModel.uiState.value.firstStepReward).isNull()
        collectJob.cancel()
    }

    @Test
    fun `the bar being shown is tracked once however often it is reported`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        fakeUserRepository.enrollInFirstStepsForTest()

        viewModel.onFirstStepsShown()
        viewModel.onFirstStepsShown()

        assertThat(fakeAnalyticsTracker.firstStepsShownEvents).containsExactly(0)
        collectJob.cancel()
    }
}
