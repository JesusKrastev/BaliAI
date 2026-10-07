package com.jesuskrastev.bali.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.StreakBet
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** The streak bet pays only after the streak grows by seven study days, and only once. */
class StreakBetSettlementTest {

    private val now = System.currentTimeMillis()
    private val today = DailyStreak.epochDay(now)

    /**
     * A user who studied every day up to yesterday, with a bet that started at [betStart] days.
     *
     * @param streak the current streak, last studied yesterday
     * @param betStart the streak when the bet was placed, 0 for no bet
     */
    private fun userWithBet(streak: Int, betStart: Int): User {
        val yesterday = DailyStreak.startOfDayMillis(today - 1)
        return User(
            currentStreak = streak,
            highestStreak = streak,
            lastPracticeTimestamp = yesterday,
            practiceDays = listOf(yesterday),
            streakBetTarget = if (betStart > 0) StreakBet.targetFor(betStart) else 0
        )
    }

    /** A user whose last study day was five days ago, so any streak is lost when they study today. */
    private fun userWhoLostTheStreak(streak: Int): User {
        val fiveDaysAgo = DailyStreak.startOfDayMillis(today - 5)
        return User(
            currentStreak = streak,
            highestStreak = streak,
            lastPracticeTimestamp = fiveDaysAgo,
            practiceDays = listOf(fiveDaysAgo),
            streakBetTarget = StreakBet.targetFor(streak)
        )
    }

    private suspend fun study(users: FakeUserRepository): Int = IncrementStreakUseCase(users)()

    @Test
    fun `studying one day does not pay the bet`() = runTest {
        val users = FakeUserRepository().apply { insert(userWithBet(streak = 3, betStart = 3)) }

        study(users)

        val user = users.get().first()!!
        assertThat(user.coins).isEqualTo(0)
        assertThat(user.streakBetTarget).isEqualTo(3 + StreakBet.DAYS)
    }

    @Test
    fun `reaching the target pays the payout once and clears the bet`() = runTest {
        // Six study days are already done since the bet; today is the seventh.
        val users = FakeUserRepository().apply {
            insert(userWithBet(streak = 3 + StreakBet.DAYS - 1, betStart = 3))
        }

        study(users)
        study(users)

        val user = users.get().first()!!
        assertThat(user.currentStreak).isEqualTo(3 + StreakBet.DAYS)
        assertThat(user.coins).isEqualTo(StreakBet.PAYOUT_COINS)
        assertThat(user.streakBetTarget).isEqualTo(0)
    }

    @Test
    fun `losing the streak forgets the bet without paying anything`() = runTest {
        val users = FakeUserRepository().apply { insert(userWhoLostTheStreak(streak = 4)) }

        study(users)

        val user = users.get().first()!!
        assertThat(user.currentStreak).isEqualTo(1)
        assertThat(user.coins).isEqualTo(0)
        assertThat(user.streakBetTarget).isEqualTo(0)
    }

    @Test
    fun `a bet whose streak was lost does not pay even if a new streak is already long`() = runTest {
        // Bet at 5 with the streak lost: today starts a new streak of 1, far from the target of 12.
        val users = FakeUserRepository().apply { insert(userWhoLostTheStreak(streak = 5)) }

        study(users)

        assertThat(users.get().first()!!.coins).isEqualTo(0)
    }

    @Test
    fun `a user without a bet gets nothing extra for studying`() = runTest {
        val users = FakeUserRepository().apply { insert(userWithBet(streak = 3, betStart = 0)) }

        study(users)

        assertThat(users.get().first()!!.coins).isEqualTo(0)
    }

    @Test
    fun `the bet is lost only below its starting streak`() {
        val target = StreakBet.targetFor(5)

        assertThat(StreakBet.isLost(target, settledStreak = 5)).isFalse()
        assertThat(StreakBet.isLost(target, settledStreak = 9)).isFalse()
        assertThat(StreakBet.isLost(target, settledStreak = 0)).isTrue()
        assertThat(StreakBet.isLost(0, settledStreak = 0)).isFalse()
        assertThat(StreakBet.daysDone(target, streak = 8)).isEqualTo(3)
        assertThat(StreakBet.isWon(target, streak = 12)).isTrue()
        assertThat(StreakBet.isWon(target, streak = 11)).isFalse()
    }
}
