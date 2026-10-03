package com.jesuskrastev.bali.ui.screens.home

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.ui.screens.stats.pickerMillisFromLocalDay
import com.jesuskrastev.bali.util.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

class HomePlanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** A tap on the chip as analytics received it. */
    private data class ChipClick(val stage: String, val daysLeft: Int?, val todayDone: Boolean)

    /** A sheet button as analytics received it. */
    private data class ActionClick(val action: String, val stage: String, val daysLeft: Int?)

    /** An exam date as analytics received it. */
    private data class ExamDate(val daysUntil: Int, val hadPlanDate: Boolean, val source: String)

    private class RecordingTracker : AnalyticsTracker(mock(), mock(), mock()) {
        val chipClicks = mutableListOf<ChipClick>()
        val actionClicks = mutableListOf<ActionClick>()
        val examDates = mutableListOf<ExamDate>()

        override fun homePlanChipClicked(stage: String, daysLeft: Int?, todayDone: Boolean) {
            chipClicks += ChipClick(stage, daysLeft, todayDone)
        }

        override fun homePlanActionClicked(action: String, stage: String, daysLeft: Int?) {
            actionClicks += ActionClick(action, stage, daysLeft)
        }

        override fun examDateSet(daysUntil: Int, hadPlanDate: Boolean, source: String) {
            examDates += ExamDate(daysUntil, hadPlanDate, source)
        }
    }

    private val tracker = RecordingTracker()
    private val users = FakeUserRepository()
    /** Built on first use, once [MainDispatcherRule] has set the main dispatcher its scope runs on. */
    private val viewModel by lazy { HomePlanViewModel(userRepository = users, analytics = tracker) }

    // Calendar days, not 24-hour blocks: across the end of summer time a block lands an hour short,
    // on the day before, when the test runs just after midnight.
    private fun daysFromNow(days: Int) = java.util.Calendar.getInstance().apply {
        add(java.util.Calendar.DAY_OF_YEAR, days)
    }.timeInMillis

    @Test
    fun `the chip counts down to the plan date saved at onboarding`() = runTest {
        users.setPlanDatesForTest(examDateMillis = null, planTargetMillis = daysFromNow(21))

        val state = viewModel.uiState.first { it.plan.targetMillis != null }

        assertThat(state.copy?.chipLabel).isEqualTo("21 días")
        assertThat(state.copy?.showsPendingDot).isTrue()
    }

    @Test
    fun `studying today clears the pending dot`() = runTest {
        users.setPlanDatesForTest(examDateMillis = null, planTargetMillis = daysFromNow(21))
        viewModel.uiState.first { it.plan.targetMillis != null }

        val today = DailyStreak.epochDay(System.currentTimeMillis())
        users.updateStreak(
            DailyStreak(
                current = 1,
                highest = 1,
                freezes = 0,
                lastPracticeMillis = System.currentTimeMillis(),
                practiceDays = listOf(System.currentTimeMillis()),
                frozenDays = emptyList()
            )
        )

        val state = viewModel.uiState.first { it.studiedToday }
        assertThat(DailyStreak.epochDay(state.plan.targetMillis!!)).isGreaterThan(today)
        assertThat(state.copy?.showsPendingDot).isFalse()
    }

    @Test
    fun `a tap on the chip is reported with what it showed`() = runTest {
        users.setPlanDatesForTest(examDateMillis = daysFromNow(5), planTargetMillis = daysFromNow(40))
        viewModel.uiState.first { it.plan.targetMillis != null }

        viewModel.onChipClick()

        assertThat(tracker.chipClicks).containsExactly(ChipClick("final_week", 5, false))
    }

    @Test
    fun `each sheet button is reported by name`() = runTest {
        users.setPlanDatesForTest(examDateMillis = null, planTargetMillis = daysFromNow(45))
        viewModel.uiState.first { it.plan.targetMillis != null }

        viewModel.onActionClick(HomePlanAction.START_SESSION)
        viewModel.onActionClick(HomePlanAction.SEE_PLAN)

        assertThat(tracker.actionClicks).containsExactly(
            ActionClick("start_session", "on_track", 45),
            ActionClick("see_plan", "on_track", 45)
        ).inOrder()
    }

    @Test
    fun `a date that has gone by is reported as such, without days`() = runTest {
        users.setPlanDatesForTest(examDateMillis = daysFromNow(-3), planTargetMillis = daysFromNow(-3))
        viewModel.uiState.first { it.datePassed }

        viewModel.onActionClick(HomePlanAction.SET_DATE)

        assertThat(tracker.actionClicks).containsExactly(ActionClick("set_date", "date_passed", null))
    }

    @Test
    fun `a date picked on Home is saved and counted down to, and reported from Home`() = runTest {
        viewModel.uiState.first { it.isLoaded }

        viewModel.setExamDate(pickerMillisFromLocalDay(daysFromNow(10)))

        val state = viewModel.uiState.first { it.plan.isExamDate }
        assertThat(state.plan.daysLeft).isEqualTo(10)
        assertThat(tracker.examDates).containsExactly(ExamDate(10, hadPlanDate = false, source = "home"))
    }
}
