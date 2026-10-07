package com.jesuskrastev.bali.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.ShopInventoryItem
import com.jesuskrastev.bali.domain.model.TestMode
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

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

    /** Verifies that a practice aid cannot be consumed after its only copy has been spent. */
    @Test
    fun `practice aid is consumed once`() = runTest {
        val users = FakeUserRepository().apply { insert(User(hints = 1)) }

        assertThat(users.consumeInventoryItem(ShopInventoryItem.HINT)).isTrue()
        assertThat(users.consumeInventoryItem(ShopInventoryItem.HINT)).isFalse()
    }
}
