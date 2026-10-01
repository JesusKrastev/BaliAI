package com.jesuskrastev.bali.ui.screens.streak

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.SettleStreakUseCase
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.ui.screens.home.StreakStatus
import com.jesuskrastev.bali.util.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class StreakViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = System.currentTimeMillis()

    /**
     * Local midnight of a day relative to today.
     *
     * @param offset days from today, negative for the past
     */
    private fun day(offset: Int): Long =
        DailyStreak.startOfDayMillis(DailyStreak.epochDay(now) + offset)

    @Test
    fun `a streak lost since the last visit shows as zero before it is saved`() {
        val user = User(currentStreak = 5, highestStreak = 5, lastPracticeTimestamp = day(-3), practiceDays = listOf(day(-3)))

        val state = streakUiStateOf(user, now)

        assertThat(state.currentStreak).isEqualTo(0)
        assertThat(state.highestStreak).isEqualTo(5)
    }

    @Test
    fun `today counts once the user has studied`() {
        val user = User(currentStreak = 2, lastPracticeTimestamp = day(0), practiceDays = listOf(day(-1), day(0)))

        val state = streakUiStateOf(user, now)

        assertThat(state.practicedToday).isTrue()
        assertThat(state.week.single { it.isToday }.status).isEqualTo(StreakStatus.COMPLETED)
    }

    @Test
    fun `the week has seven days, Monday first, and marks today while it is open`() {
        val state = streakUiStateOf(User(), now)

        assertThat(state.week.map { it.dayOfWeek }).containsExactly("L", "M", "X", "J", "V", "S", "D").inOrder()
        assertThat(state.week.single { it.isToday }.status).isEqualTo(StreakStatus.TODAY)
    }

    @Test
    fun `the screen follows the profile`() = runTest {
        val users = FakeUserRepository().apply {
            insert(User(currentStreak = 1, highestStreak = 4, lastPracticeTimestamp = now, practiceDays = listOf(day(0))))
        }

        val state = StreakViewModel(users).uiState.first { !it.isLoading }

        assertThat(state.currentStreak).isEqualTo(1)
        assertThat(state.highestStreak).isEqualTo(4)
    }

    @Test
    fun `only the first session of the day extends the streak`() = runTest {
        val users = FakeUserRepository().apply {
            insert(User(currentStreak = 3, highestStreak = 3, lastPracticeTimestamp = day(-1), practiceDays = listOf(day(-1))))
        }
        val increment = IncrementStreakUseCase(users)

        assertThat(increment()).isEqualTo(4)
        assertThat(increment()).isEqualTo(-1)
        assertThat(users.get().first()?.currentStreak).isEqualTo(4)
        assertThat(users.get().first()?.highestStreak).isEqualTo(4)
    }

    @Test
    fun `opening the app spends a freeze on a missed day`() = runTest {
        val users = FakeUserRepository().apply {
            insert(User(currentStreak = 6, streakFreezes = 1, lastPracticeTimestamp = day(-2), practiceDays = listOf(day(-2))))
        }

        SettleStreakUseCase(users)()

        val user = users.get().first()!!
        assertThat(user.currentStreak).isEqualTo(6)
        assertThat(user.streakFreezes).isEqualTo(0)
        assertThat(user.frozenDays).containsExactly(day(-1))
    }

    @Test
    fun `opening the app after the freezes run out ends the streak`() = runTest {
        val users = FakeUserRepository().apply {
            insert(User(currentStreak = 6, lastPracticeTimestamp = day(-2), practiceDays = listOf(day(-2))))
        }

        SettleStreakUseCase(users)()

        assertThat(users.get().first()?.currentStreak).isEqualTo(0)
    }
}
