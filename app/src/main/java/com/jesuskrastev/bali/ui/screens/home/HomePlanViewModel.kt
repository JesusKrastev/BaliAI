package com.jesuskrastev.bali.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.ui.screens.stats.PlanSummary
import com.jesuskrastev.bali.ui.screens.stats.PlanUrgency
import com.jesuskrastev.bali.ui.screens.stats.calendarDaysBetween
import com.jesuskrastev.bali.ui.screens.stats.localDayFromPickerMillis
import com.jesuskrastev.bali.ui.screens.stats.urgency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What Home's plan chip shows.
 *
 * @property isLoaded false until the profile has loaded; the chip stays hidden until then
 * @property plan the date counted down to, the same one the statistics countdown uses
 * @property datePassed whether a date was saved but has already gone by
 * @property studiedToday whether today already has a study session
 * @property copy the chip's and the sheet's texts, null until [isLoaded]
 */
data class HomePlanUiState(
    val isLoaded: Boolean = false,
    val plan: PlanSummary = PlanSummary(),
    val datePassed: Boolean = false,
    val studiedToday: Boolean = false,
    val copy: HomePlanCopy? = null
)

/**
 * A button on Home's plan sheet, named as analytics records it.
 *
 * @property eventName the `action` property sent with `home_plan_action_clicked`
 */
enum class HomePlanAction(val eventName: String) {
    /** Opens the next unlocked node of the path. */
    START_SESSION("start_session"),

    /** Opens the statistics tab, where the full countdown is. */
    SEE_PLAN("see_plan"),

    /** Opens the date picker when there is no date ahead. */
    SET_DATE("set_date"),

    /** Opens the date picker to correct the date being counted down to. */
    CHANGE_DATE("change_date")
}

/**
 * Feeds the plan chip at the top of Home, kept apart from [HomeViewModel] so the chip owns its
 * state: the onboarding plan's date, the days left and today's goal, all read from the profile
 * that already holds them (no new data). It also saves an exam date picked from the chip's sheet
 * and reports what the user taps.
 */
@HiltViewModel
class HomePlanViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    val uiState: StateFlow<HomePlanUiState> = userRepository.get()
        .map { user -> homePlanStateOf(user, System.currentTimeMillis()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomePlanUiState())

    /** Reports a tap on the chip, with the stretch it showed and whether today was done. */
    fun onChipClick() {
        val state = uiState.value
        analytics.homePlanChipClicked(
            stage = analyticsStageOf(state),
            daysLeft = state.daysLeftOrNull(),
            todayDone = state.studiedToday
        )
    }

    /**
     * Reports a button pressed on the plan sheet.
     *
     * @param action the button
     */
    fun onActionClick(action: HomePlanAction) {
        val state = uiState.value
        analytics.homePlanActionClicked(
            action = action.eventName,
            stage = analyticsStageOf(state),
            daysLeft = state.daysLeftOrNull()
        )
    }

    /**
     * Saves the exam date picked from the plan sheet; from then on Home and Statistics count down to it.
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
                hadPlanDate = hadPlanDate,
                source = "home"
            )
        }
    }
}

/**
 * Names what the chip showed, for analytics.
 *
 * @param state the chip's state
 * @return a lower-case [PlanUrgency] name such as "final_week", or "date_passed" when the saved
 *   date is behind
 */
internal fun analyticsStageOf(state: HomePlanUiState): String =
    if (state.datePassed) "date_passed" else state.plan.urgency().name.lowercase()

/**
 * Gives the days left for analytics.
 *
 * @return the days until the date, or null when there is no date ahead
 */
private fun HomePlanUiState.daysLeftOrNull(): Int? = plan.targetMillis?.let { plan.daysLeft }
