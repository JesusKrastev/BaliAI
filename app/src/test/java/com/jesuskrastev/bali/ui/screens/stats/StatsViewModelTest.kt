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

    private class RecordingTracker : AnalyticsTracker(mock(), mock(), mock()) {
        val views = mutableListOf<Triple<String, Int, Int?>>()

        override fun readinessViewed(level: String, mocksTaken: Int, passPercent: Int?) {
            views += Triple(level, mocksTaken, passPercent)
        }
    }

    private val tracker = RecordingTracker()

    private fun exam(score: Int) =
        TestResult("", ExamRules.OFFICIAL_EXAM_CATEGORY, score, 30, Date(), ExamRules.isPassed(score))

    private fun viewModel(results: StatsResults) = StatsViewModel(
        testResultRepository = results,
        answerRepository = FakeAnswerRepository(),
        userRepository = FakeUserRepository(),
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
}
