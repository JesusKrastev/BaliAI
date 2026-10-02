package com.jesuskrastev.bali.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.ShopInventoryItem
import com.jesuskrastev.bali.domain.model.TestMode
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class RewardBoostUseCaseTest {

    /** Verifies that the next XP reward, and only that reward, consumes a double-XP boost. */
    @Test
    fun `double XP boost doubles one reward and is consumed`() = runTest {
        val users = FakeUserRepository().apply {
            insert(User(coins = 100, doubleXpBoosts = 1))
        }

        val earned = IncrementXpUseCase(users)(
            mode = TestMode.PRACTICE,
            correctAnswers = 10,
            totalQuestions = 10,
            durationSeconds = 100
        )

        assertThat(earned.xpGained).isEqualTo(60)
        assertThat(users.get().first()!!.doubleXpBoosts).isEqualTo(0)
    }

    /** Verifies that every part of the XP breakdown is doubled, so the result screen still adds up. */
    @Test
    fun `double XP doubles every part of the breakdown`() = runTest {
        val users = FakeUserRepository().apply { insert(User(doubleXpBoosts = 1, currentStreak = 7)) }

        val earned = IncrementXpUseCase(users)(
            mode = TestMode.PRACTICE,
            correctAnswers = 10,
            totalQuestions = 10,
            durationSeconds = 100
        )

        val parts = earned.baseXp + (earned.bonusPerfection ?: 0) +
            (earned.bonusFast ?: 0) + (earned.bonusStreak ?: 0)
        assertThat(parts).isEqualTo(earned.xpGained)
        assertThat(earned.bonusStreak).isEqualTo(6)
    }

    /** Verifies that the next coin reward doubles once and the boost is spent. */
    @Test
    fun `double coins boost doubles one reward and is consumed`() = runTest {
        val users = FakeUserRepository().apply { insert(User(coins = 0, doubleCoinBoosts = 1)) }

        val first = IncrementCoinsUseCase(users)(accuracy = 100)
        val second = IncrementCoinsUseCase(users)(accuracy = 100)

        assertThat(first).isIn(16..20)
        assertThat(second).isIn(8..10)
        assertThat(users.get().first()!!.coins).isEqualTo(first + second)
        assertThat(users.get().first()!!.doubleCoinBoosts).isEqualTo(0)
    }

    /** Verifies that failing to spend a boost never costs the user the normal reward. */
    @Test
    fun `a boost that cannot be spent leaves the normal reward untouched`() = runTest {
        val users = FakeUserRepository().apply { insert(User(coins = 0, doubleCoinBoosts = 1)) }
        val offline = object : UserRepository by users {
            override suspend fun consumeInventoryItem(item: ShopInventoryItem): Boolean =
                throw IOException("offline")
        }

        val gained = IncrementCoinsUseCase(offline)(accuracy = 100)

        assertThat(gained).isIn(8..10)
        assertThat(users.get().first()!!.coins).isEqualTo(gained)
    }

    /** Verifies that without a boost the repository is never asked to spend one (no transaction offline). */
    @Test
    fun `rewards without a boost never touch the inventory`() = runTest {
        val users = FakeUserRepository().apply { insert(User(coins = 0)) }
        val strict = object : UserRepository by users {
            override suspend fun consumeInventoryItem(item: ShopInventoryItem): Boolean =
                error("must not be called without an owned boost")
        }

        IncrementCoinsUseCase(strict)(accuracy = 100)
        IncrementXpUseCase(strict)(TestMode.PRACTICE, 10, 10, 100)
    }

    /** Verifies that a practice aid cannot be consumed after its only copy has been spent. */
    @Test
    fun `practice aid is consumed once`() = runTest {
        val users = FakeUserRepository().apply { insert(User(hints = 1)) }

        assertThat(users.consumeInventoryItem(ShopInventoryItem.HINT)).isTrue()
        assertThat(users.consumeInventoryItem(ShopInventoryItem.HINT)).isFalse()
    }
}
