package com.jesuskrastev.bali.ui.screens.test

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StreakCheerTest {

    @Test
    fun `bali cheers at three, five, ten and every five after`() {
        val cheers = (0..30).filter { isCheerRun(it) }

        assertThat(cheers).containsExactly(3, 5, 10, 15, 20, 25, 30).inOrder()
    }

    @Test
    fun `short runs never cheer`() {
        assertThat((0..2).none { isCheerRun(it) }).isTrue()
    }

    @Test
    fun `the message names the run once it is five or more`() {
        assertThat(cheerMessage(3)).doesNotContain("3")
        assertThat(cheerMessage(5)).contains("5 seguidas")
        assertThat(cheerMessage(10)).contains("10 seguidas")
        assertThat(cheerMessage(25)).contains("25 seguidas")
    }
}
