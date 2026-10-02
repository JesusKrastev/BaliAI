package com.jesuskrastev.bali.domain.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QuestionIdTest {

    @Test
    fun `the id has the q_ prefix and 12 hex characters`() {
        assertThat(QuestionId.of("¿Qué es la prioridad?")).matches("q_[0-9a-f]{12}")
    }

    @Test
    fun `the same text always gives the same id`() {
        assertThat(QuestionId.of("¿Qué es la prioridad?")).isEqualTo(QuestionId.of("¿Qué es la prioridad?"))
    }

    @Test
    fun `capitals, accents, punctuation and spacing do not change the id`() {
        val original = QuestionId.of("¿Qué factor psicológico puede afectar?")

        assertThat(QuestionId.of("que  FACTOR psicologico   puede afectar")).isEqualTo(original)
        assertThat(QuestionId.of("  ¡Qué factor psicológico puede afectar!  ")).isEqualTo(original)
    }

    @Test
    fun `different questions get different ids`() {
        assertThat(QuestionId.of("¿Qué es la prioridad?")).isNotEqualTo(QuestionId.of("¿Qué es la velocidad?"))
    }

    /**
     * Pins the recipe: ids are stored with every answer, so changing the normalization or the
     * hash would stop matching all of them. If this test fails, the change is wrong, not the test.
     */
    @Test
    fun `the recipe is frozen`() {
        assertThat(QuestionId.of("¿Qué es la prioridad?")).isEqualTo("q_8054e8d5fa8c")
        assertThat(
            QuestionId.of("¿Qué factor psicológico puede afectar más negativamente a la conducción?")
        ).isEqualTo("q_0ac30ab531fb")
    }
}
