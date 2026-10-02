package com.jesuskrastev.bali.domain.util

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.FirstStepReward
import com.jesuskrastev.bali.domain.model.FirstStepTask
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PendingFirstStepRewardsTest {

    private val pending = PendingFirstStepRewards()
    private val first = FirstStepReward(FirstStepTask.FIRST_TEST, coins = 30, completedAll = false)
    private val second = FirstStepReward(FirstStepTask.ASK_BALI, coins = 20, completedAll = false)

    @Test
    fun `nothing is waiting at first`() = runTest {
        assertThat(pending.next.first()).isNull()
    }

    @Test
    fun `rewards are celebrated in the order they were earned`() = runTest {
        pending.publish(first)
        pending.publish(second)

        assertThat(pending.next.first()).isEqualTo(first)
        pending.consume()
        assertThat(pending.next.first()).isEqualTo(second)
        pending.consume()
        assertThat(pending.next.first()).isNull()
    }

    @Test
    fun `consuming with nothing queued is harmless`() = runTest {
        pending.consume()

        assertThat(pending.next.first()).isNull()
    }
}
