package com.jesuskrastev.bali.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExamRulesTest {

    @Test
    fun `three mistakes are allowed in a thirty question exam`() {
        assertThat(ExamRules.PASS_SCORE).isEqualTo(27)
    }

    @Test
    fun `twenty six correct answers fail and twenty seven pass`() {
        assertThat(ExamRules.isPassed(26)).isFalse()
        assertThat(ExamRules.isPassed(27)).isTrue()
        assertThat(ExamRules.isPassed(30)).isTrue()
    }
}
