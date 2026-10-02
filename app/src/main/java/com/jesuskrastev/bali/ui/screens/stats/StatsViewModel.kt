package com.jesuskrastev.bali.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.model.ProgressStats
import com.jesuskrastev.bali.domain.repository.AnswerRepository
import com.jesuskrastev.bali.domain.repository.TestResultRepository
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.usecase.CalculateProgressStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What the statistics screen shows.
 *
 * @property isLoading true until the first figures are ready
 * @property stats the figures, null while [isLoading]
 * @property plan the date the countdown counts down to, see [planSummaryOf]
 */
data class StatsUiState(
    val isLoading: Boolean = true,
    val stats: ProgressStats? = null,
    val plan: PlanSummary = PlanSummary()
)

/**
 * Feeds the statistics screen: recomputes the figures every time the user's results, answers or
 * profile change, so finishing a mock exam is reflected the next time the screen is open. It also
 * saves the exam date the user picks from the countdown.
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    testResultRepository: TestResultRepository,
    answerRepository: AnswerRepository,
    private val userRepository: UserRepository,
    private val calculateProgressStats: CalculateProgressStatsUseCase,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    private var viewReported = false

    val uiState: StateFlow<StatsUiState> = combine(
        testResultRepository.get(),
        answerRepository.getAll(),
        userRepository.get()
    ) { results, answers, user ->
        val now = System.currentTimeMillis()
        StatsUiState(
            isLoading = false,
            stats = calculateProgressStats(results, answers, user, now),
            plan = planSummaryOf(user?.examDateMillis, user?.planTargetMillis, now)
        )
    }
        .onEach { reportViewOnce(it.stats) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    /**
     * Saves the exam date picked on the countdown card; from then on the card counts down to it.
     *
     * @param pickerMillis the date picker's selection, midnight UTC of the chosen day
     */
    fun setExamDate(pickerMillis: Long) {
        val examDay = localDayFromPickerMillis(pickerMillis)
        val hadPlanDate = uiState.value.plan.targetMillis != null
        viewModelScope.launch {
            userRepository.updateExamDate(examDay)
            analytics.examDateSet(
                daysUntil = calendarDaysBetween(System.currentTimeMillis(), examDay),
                hadPlanDate = hadPlanDate
            )
        }
    }

    /**
     * Tells analytics what the screen showed, once per visit, so a figure refreshing while the
     * screen is open does not count as a second view.
     *
     * @param stats the figures just computed
     */
    private fun reportViewOnce(stats: ProgressStats?) {
        if (viewReported || stats == null) return
        viewReported = true
        analytics.readinessViewed(
            level = stats.readiness.level.name,
            mocksTaken = stats.readiness.mocksTaken,
            passPercent = stats.readiness.passProbability?.let { (it * 100).toInt() }
        )
    }
}
