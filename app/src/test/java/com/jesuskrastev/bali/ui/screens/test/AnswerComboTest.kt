package com.jesuskrastev.bali.ui.screens.test

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnswerComboTest {

    @Test
    fun `no label under two correct answers in a row`() {
        assertThat(ComboTier.of(0)).isEqualTo(ComboTier.HIDDEN)
        assertThat(ComboTier.of(1)).isEqualTo(ComboTier.HIDDEN)
    }

    @Test
    fun `two to four in a row is the plain tier`() {
        assertThat((2..4).map { ComboTier.of(it) }.toSet()).containsExactly(ComboTier.WARM)
    }

    @Test
    fun `five to nine in a row is the hot tier`() {
        assertThat((5..9).map { ComboTier.of(it) }.toSet()).containsExactly(ComboTier.HOT)
    }

    @Test
    fun `ten or more in a row is the on-fire tier`() {
        assertThat(listOf(10, 11, 30).map { ComboTier.of(it) }.toSet()).containsExactly(ComboTier.ON_FIRE)
    }

    @Test
    fun `a negative run never shows a label`() {
        assertThat(ComboTier.of(-3)).isEqualTo(ComboTier.HIDDEN)
    }

    @Test
    fun `the label only pulses on the answer that crosses into a louder tier`() {
        val pulses = (0..30).filter { isComboThreshold(it) }

        assertThat(pulses).containsExactly(5, 10).inOrder()
    }
}
