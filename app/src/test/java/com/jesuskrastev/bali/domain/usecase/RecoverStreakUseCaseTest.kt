package com.jesuskrastev.bali.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecoverStreakUseCaseTest {

    private val now = System.currentTimeMillis()

    /**
     * Local midnight of a day relative to today.
     *
     * @param offset days from today, negative for the past
     */
    private fun day(offset: Int): Long =
        DailyStreak.startOfDayMillis(DailyStreak.epochDay(now) + offset)

    /**
     * A repository whose user studied for [days] days up to [lastStudyOffset] days from today.
     *
     * @param coins starting balance
     * @param days streak length stored on the profile
     * @param lastStudyOffset day of the last study, relative to today
     */
    private suspend fun repository(coins: Int, days: Int = 8, lastStudyOffset: Int = -2) =
        FakeUserRepository().apply {
            insert(
                User(
                    coins = coins,
                    currentStreak = days,
                    highestStreak = days,
                    lastPracticeTimestamp = day(lastStudyOffset),
                    practiceDays = listOf(day(lastStudyOffset))
                )
            )
        }

    @Test
    fun `recovering charges the price and brings back the whole streak`() = runTest {
        val users = repository(coins = 400)

        val restored = RecoverStreakUseCase(users, DecrementCoinsUseCase(users))()

        val user = users.get().first()!!
        assertThat(restored).isEqualTo(8)
        assertThat(user.currentStreak).isEqualTo(8)
        assertThat(user.coins).isEqualTo(400 - DailyStreak.RECOVERY_COST_COINS)
        assertThat(user.lostStreak).isEqualTo(0)
        assertThat(user.frozenDays).containsExactly(day(-1))
    }

    @Test
    fun `without enough coins nothing is charged and the streak stays lost and recoverable`() = runTest {
        val users = repository(coins = DailyStreak.RECOVERY_COST_COINS - 1)

        val restored = RecoverStreakUseCase(users, DecrementCoinsUseCase(users))()

        val user = users.get().first()!!
        assertThat(restored).isNull()
        assertThat(user.coins).isEqualTo(DailyStreak.RECOVERY_COST_COINS - 1)
        assertThat(user.frozenDays).isEmpty()
    }

    @Test
    fun `a streak lost two days ago cannot be bought back and costs nothing`() = runTest {
        val users = repository(coins = 500, lastStudyOffset = -3)

        val restored = RecoverStreakUseCase(users, DecrementCoinsUseCase(users))()

        assertThat(restored).isNull()
        assertThat(users.get().first()?.coins).isEqualTo(500)
    }

    @Test
    fun `a streak that is still alive has nothing to recover`() = runTest {
        val users = repository(coins = 500, lastStudyOffset = -1)

        val restored = RecoverStreakUseCase(users, DecrementCoinsUseCase(users))()

        assertThat(restored).isNull()
        assertThat(users.get().first()?.coins).isEqualTo(500)
    }

    @Test
    fun `buying twice charges once`() = runTest {
        val users = repository(coins = 700)
        val recover = RecoverStreakUseCase(users, DecrementCoinsUseCase(users))

        assertThat(recover()).isEqualTo(8)
        assertThat(recover()).isNull()
        assertThat(users.get().first()?.coins).isEqualTo(700 - DailyStreak.RECOVERY_COST_COINS)
    }

    @Test
    fun `without a profile there is nothing to recover`() = runTest {
        val users = FakeUserRepository()
        users.clear()

        assertThat(RecoverStreakUseCase(users, DecrementCoinsUseCase(users))()).isNull()
    }
}
