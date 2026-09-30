package com.jesuskrastev.bali.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FirstStepsProgressTest {

    @Test
    fun `an account that was never enrolled is not in the first-steps window`() {
        val progress = FirstStepsProgress()

        assertThat(progress.isEnrolled).isFalse()
        assertThat(progress.isActive).isFalse()
    }

    @Test
    fun `an enrolled account is active until it dismisses the card`() {
        val progress = FirstStepsProgress.startingAt(1_000L)

        assertThat(progress.isActive).isTrue()
        assertThat(progress.copy(dismissed = true).isActive).isFalse()
    }

    @Test
    fun `the tasks pay exactly 100 coins counting the completion bonus`() {
        val total = FirstStepTask.entries.sumOf { it.coins } + FIRST_STEPS_BONUS_COINS

        assertThat(total).isEqualTo(100)
    }

    @Test
    fun `pending coins start at the full prize and shrink as tasks are done`() {
        val none = FirstStepsProgress.startingAt(1_000L)
        val first = none.copy(completed = setOf(FirstStepTask.FIRST_TEST))

        assertThat(none.pendingCoins).isEqualTo(100)
        assertThat(first.pendingCoins).isEqualTo(100 - FirstStepTask.FIRST_TEST.coins)
    }

    @Test
    fun `nothing is pending once every task and the bonus are paid`() {
        val done = FirstStepsProgress.startingAt(1_000L).copy(completed = FirstStepTask.entries.toSet())

        assertThat(done.isComplete).isTrue()
        assertThat(done.doneCount).isEqualTo(FirstStepTask.entries.size)
        assertThat(done.pendingCoins).isEqualTo(0)
    }

    @Test
    fun `a partly done card is not complete`() {
        val progress = FirstStepsProgress.startingAt(1_000L).copy(completed = setOf(FirstStepTask.PLAY_GAME))

        assertThat(progress.isComplete).isFalse()
        assertThat(progress.isDone(FirstStepTask.PLAY_GAME)).isTrue()
        assertThat(progress.isDone(FirstStepTask.ASK_BALI)).isFalse()
    }

    @Test
    fun `task ids survive a round trip and unknown ids are ignored`() {
        FirstStepTask.entries.forEach { task ->
            assertThat(FirstStepTask.fromId(task.id)).isEqualTo(task)
        }
        assertThat(FirstStepTask.fromId("a_task_from_the_future")).isNull()
    }

    @Test
    fun `task ids are unique`() {
        val ids = FirstStepTask.entries.map { it.id }

        assertThat(ids).containsNoDuplicates()
    }
}
