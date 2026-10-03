package com.jesuskrastev.bali.ui.screens.stats

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.domain.model.ReadinessLevel
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.repository.TestResultRepository
import com.jesuskrastev.bali.domain.usecase.CalculateProgressStatsUseCase
import com.jesuskrastev.bali.domain.usecase.CalculateReadinessUseCase
import com.jesuskrastev.bali.ui.screens.auth.FakeAnswerRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.util.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import java.util.Date
import java.util.concurrent.TimeUnit

class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class StatsResults(initial: List<TestResult> = emptyList()) : TestResultRepository {
        val results = MutableStateFlow(initial)
        override fun getRecent(): Flow<List<TestResult>> = results
        override fun get(): Flow<List<TestResult>> = results
        override suspend fun insert(result: TestResult): String = "id"
        override fun count(): Flow<Int> = flowOf(results.value.size)
        override fun getAverageScore(): Flow<Double?> = flowOf(null)
        override suspend fun clear() {}
    }

    private class RecordingTracker : AnalyticsTracker(mock(), mock()) {
        val views = mutableListOf<Triple<String, Int, Int?>>()
        val examDates = mutableListOf<Pair<Int, Boolean>>()
        val examDateSources = mutableListOf<String>()

        override fun examDateSet(daysUntil: Int, hadPlanDate: Boolean, source: String) {
            examDates += daysUntil to hadPlanDate
            examDateSources += source
        }

        override fun readinessViewed(level: String, mocksTaken: Int, passPercent: Int?) {
            views += Triple(level, mocksTaken, passPercent)
        }
    }

    private val tracker = RecordingTracker()

    private fun exam(score: Int) =
        TestResult("", ExamRules.OFFICIAL_EXAM_CATEGORY, score, 30, Date(), ExamRules.isPassed(score))

    private fun viewModel(results: StatsResults, users: FakeUserRepository = FakeUserRepository()) = StatsViewModel(
        testResultRepository = results,
        answerRepository = FakeAnswerRepository(),
        userRepository = users,
        calculateProgressStats = CalculateProgressStatsUseCase(CalculateReadinessUseCase()),
        analytics = tracker
    )

    @Test
    fun `the figures are ready once the history has loaded`() = runTest {
        val viewModel = viewModel(StatsResults(listOf(exam(29), exam(29), exam(30))))

        val state = viewModel.uiState.first { !it.isLoading }

        assertThat(state.stats?.readiness?.mocksTaken).isEqualTo(3)
        assertThat(state.stats?.readiness?.level).isEqualTo(ReadinessLevel.READY)
    }

    @Test
    fun `a new user gets a state with no verdict instead of an error`() = runTest {
        val viewModel = viewModel(StatsResults())

        val state = viewModel.uiState.first { !it.isLoading }

        assertThat(state.stats?.readiness?.level).isEqualTo(ReadinessLevel.NOT_ENOUGH_DATA)
        assertThat(state.stats?.totalQuestions).isEqualTo(0)
    }

    @Test
    fun `a new mock exam updates the figures while the screen is open`() = runTest {
        val results = StatsResults(listOf(exam(20), exam(21)))
        val viewModel = viewModel(results)
        assertThat(viewModel.uiState.first { !it.isLoading }.stats?.readiness?.mocksTaken).isEqualTo(2)

        results.results.value = results.results.value + exam(28)

        val updated = viewModel.uiState.first { it.stats?.readiness?.mocksTaken == 3 }
        assertThat(updated.stats?.readiness?.level).isNotEqualTo(ReadinessLevel.NOT_ENOUGH_DATA)
    }

    @Test
    fun `the view is reported once with what the screen showed, however often it refreshes`() = runTest {
        val results = StatsResults(listOf(exam(29), exam(29), exam(30)))
        val viewModel = viewModel(results)
        viewModel.uiState.first { !it.isLoading }

        results.results.value = results.results.value + exam(28)
        viewModel.uiState.first { it.stats?.readiness?.mocksTaken == 4 }

        assertThat(tracker.views).hasSize(1)
        val (level, mocks, percent) = tracker.views.single()
        assertThat(level).isEqualTo("READY")
        assertThat(mocks).isEqualTo(3)
        assertThat(percent).isNotNull()
    }

    @Test
    fun `no percentage is reported while there is no verdict`() = runTest {
        val viewModel = viewModel(StatsResults(listOf(exam(25))))
        viewModel.uiState.first { !it.isLoading }

        assertThat(tracker.views.single().third).isNull()
    }

    @Test
    fun `a user with no dates sees the countdown asking for one`() = runTest {
        val viewModel = viewModel(StatsResults())

        val state = viewModel.uiState.first { !it.isLoading }

        assertThat(state.plan.targetMillis).isNull()
    }

    @Test
    fun `the plan date saved at onboarding counts down as the promise`() = runTest {
        val users = FakeUserRepository()
        val viewModel = viewModel(StatsResults(), users)
        val promise = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(21)

        users.setPlanDatesForTest(examDateMillis = null, planTargetMillis = promise)

        val plan = viewModel.uiState.first { it.plan.targetMillis != null }.plan
        assertThat(plan.targetMillis).isEqualTo(promise)
        assertThat(plan.isExamDate).isFalse()
    }

    @Test
    fun `setting the exam date makes the countdown count down to it and reports it`() = runTest {
        val users = FakeUserRepository()
        val viewModel = viewModel(StatsResults(), users)
        viewModel.uiState.first { !it.isLoading }
        val inTenDays = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(10)

        viewModel.setExamDate(pickerMillisFromLocalDay(inTenDays))

        val plan = viewModel.uiState.first { it.plan.isExamDate }.plan
        assertThat(plan.daysLeft).isEqualTo(10)
        assertThat(tracker.examDates).containsExactly(10 to false)
        assertThat(tracker.examDateSources).containsExactly("stats")
    }

    @Test
    fun `changing a date already set is reported as a correction`() = runTest {
        val users = FakeUserRepository()
        val viewModel = viewModel(StatsResults(), users)
        users.setPlanDatesForTest(
            examDateMillis = null,
            planTargetMillis = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(21)
        )
        viewModel.uiState.first { it.plan.targetMillis != null }

        viewModel.setExamDate(pickerMillisFromLocalDay(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(5)))

        assertThat(tracker.examDates.single().second).isTrue()
    }
}
