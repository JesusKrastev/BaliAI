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
import com.jesuskrastev.bali.domain.model.FirstStepReward
import com.jesuskrastev.bali.domain.model.FirstStepTask
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.model.TestResult.Companion.OFFICIAL_EXAM_CATEGORY
import com.jesuskrastev.bali.domain.util.DateTimeHelper
import com.jesuskrastev.bali.domain.util.PendingFirstStepRewards
import com.jesuskrastev.bali.domain.usecase.GenerateInitialPathUseCase
import com.jesuskrastev.bali.domain.usecase.GenerateNextPathNodesUseCase
import com.jesuskrastev.bali.data.remote.RemoteConfigProvider
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.mockito.kotlin.whenever
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val fakeUserRepository = FakeUserRepository()
    private val fakeAnswerRepository = FakeAnswerRepository()
    private val fakeAuthRepository = FakeAuthRepository()
    private val fakePathRepository = FakePathRepository()
    private val fakeAnalyticsTracker = FakeAnalyticsTracker(mock(), mock(), mock())
    
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dateTimeHelper: DateTimeHelper = mock()
    
    private val fakeGenerateInitialPathUseCase = GenerateInitialPathUseCase(fakePathRepository, fakeAuthRepository)
    private val fakeGenerateNextPathNodesUseCase = GenerateNextPathNodesUseCase(mock(), fakeUserRepository, fakePathRepository)
    private val remoteConfigProvider: RemoteConfigProvider = mock()
    private val pendingRewards = PendingFirstStepRewards()

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        whenever(remoteConfigProvider.getWeeklyGoal()).thenReturn(5)
        viewModel = createViewModel()
    }

    /**
     * Builds a [HomeViewModel] over the shared fakes.
     *
     * @param testResults results the account already has, to tell whether it took the first simulacro
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
            dateTimeHelper = dateTimeHelper,
            remoteConfigProvider = remoteConfigProvider,
            pendingFirstStepRewards = pendingRewards,
            context = context
        )

    private fun officialExam() = TestResult(
        category = OFFICIAL_EXAM_CATEGORY,
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
    fun `setting the exam date makes the plan card count down to it`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        val inTenDays = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(10)

        viewModel.setExamDate(pickerMillisFromLocalDay(inTenDays))

        val plan = viewModel.uiState.value.plan
        assertThat(plan.isExamDate).isTrue()
        assertThat(plan.daysLeft).isEqualTo(10)
        collectJob.cancel()
    }

    @Test
    fun `the plan date saved at onboarding shows as the promise`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        val promise = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(21)

        fakeUserRepository.setPlanDatesForTest(examDateMillis = null, planTargetMillis = promise)

        val plan = viewModel.uiState.value.plan
        assertThat(plan.targetMillis).isEqualTo(promise)
        assertThat(plan.isExamDate).isFalse()
        collectJob.cancel()
    }

    @Test
    fun `the first-steps card stays hidden for an account that was never enrolled`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }

        assertThat(viewModel.uiState.value.firstSteps).isNull()
        collectJob.cancel()
    }

    @Test
    fun `an enrolled account sees its progress in the first-steps card`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }

        fakeUserRepository.enrollInFirstStepsForTest(setOf(FirstStepTask.FIRST_TEST))

        val progress = viewModel.uiState.value.firstSteps
        assertThat(progress).isNotNull()
        assertThat(progress!!.completed).containsExactly(FirstStepTask.FIRST_TEST)
        assertThat(progress.doneCount).isEqualTo(1)
        collectJob.cancel()
    }

    @Test
    fun `dismissing the card hides it for good and reports how far the student got`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        fakeUserRepository.enrollInFirstStepsForTest(setOf(FirstStepTask.ASK_BALI))

        viewModel.dismissFirstSteps()

        assertThat(viewModel.uiState.value.firstSteps).isNull()
        assertThat(fakeAnalyticsTracker.firstStepsDismissedEvents).containsExactly(1)
        collectJob.cancel()
    }

    @Test
    fun `with every task done the card stays until the first simulacro is taken`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }

        fakeUserRepository.enrollInFirstStepsForTest(FirstStepTask.entries.toSet())

        assertThat(viewModel.uiState.value.firstSteps?.isComplete).isTrue()
        collectJob.cancel()
    }

    @Test
    fun `with every task done and the simulacro taken the card is gone`() = runTest {
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
    fun `the card being shown is tracked once however often it is reported`() = runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.uiState.collect {} }
        fakeUserRepository.enrollInFirstStepsForTest()

        viewModel.onFirstStepsShown()
        viewModel.onFirstStepsShown()

        assertThat(fakeAnalyticsTracker.firstStepsShownEvents).containsExactly(0)
        collectJob.cancel()
    }
}
