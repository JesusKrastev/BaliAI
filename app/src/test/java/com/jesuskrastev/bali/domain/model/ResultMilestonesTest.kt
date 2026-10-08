package com.jesuskrastev.bali.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Date

/** What the saved results say about the test being finished: first win and record. */
class ResultMilestonesTest {

    private fun lesson(score: Int, total: Int = 10) = TestResult(
        category = "Conductor", score = score, total = total, date = Date(), isPassed = total > 0 && score * 100 / total >= 90
    )

    private fun exam(score: Int) = TestResult(
        category = ExamRules.OFFICIAL_EXAM_CATEGORY, score = score, total = 30, date = Date(),
        isPassed = ExamRules.isPassed(score)
    )

    @Test
    fun `a lesson is a win from 70 percent and an exam only when passed`() {
        assertThat(ResultMilestones.isWin(lesson(7))).isTrue()
        assertThat(ResultMilestones.isWin(lesson(6))).isFalse()
        assertThat(ResultMilestones.isWin(exam(27))).isTrue()
        assertThat(ResultMilestones.isWin(exam(26))).isFalse()
    }

    @Test
    fun `a result without questions is not a win`() {
        assertThat(ResultMilestones.isWin(lesson(score = 0, total = 0))).isFalse()
    }

    @Test
    fun `the first win is the first test that is a win with nothing saved`() {
        assertThat(ResultMilestones.isFirstWin(emptyList(), isWin = true)).isTrue()
    }

    @Test
    fun `a first test that goes badly is not a win, and does not use up the celebration`() {
        assertThat(ResultMilestones.isFirstWin(emptyList(), isWin = false)).isFalse()
        // The next test, with only that failure saved, can still be the first win.
        assertThat(ResultMilestones.isFirstWin(listOf(lesson(2), exam(10)), isWin = true)).isTrue()
    }

    @Test
    fun `it is not the first win once an earlier win exists, in a lesson or an exam`() {
        assertThat(ResultMilestones.isFirstWin(listOf(lesson(2), lesson(8)), isWin = true)).isFalse()
        assertThat(ResultMilestones.isFirstWin(listOf(exam(28)), isWin = true)).isFalse()
    }

    @Test
    fun `the best exam score ignores lessons`() {
        val results = listOf(lesson(10), exam(20), exam(25), exam(22))
        assertThat(ResultMilestones.bestExamScore(results)).isEqualTo(25)
    }

    @Test
    fun `there is no best exam score before the first exam`() {
        assertThat(ResultMilestones.bestExamScore(emptyList())).isNull()
        assertThat(ResultMilestones.bestExamScore(listOf(lesson(10)))).isNull()
    }

    @Test
    fun `the first exam is not a record, there is nothing to beat`() {
        assertThat(ResultMilestones.isNewExamRecord(score = 30, previousBest = null)).isFalse()
    }

    @Test
    fun `an exam is a record only when it beats the best one strictly`() {
        assertThat(ResultMilestones.isNewExamRecord(score = 26, previousBest = 25)).isTrue()
        assertThat(ResultMilestones.isNewExamRecord(score = 25, previousBest = 25)).isFalse()
        assertThat(ResultMilestones.isNewExamRecord(score = 20, previousBest = 25)).isFalse()
    }

    @Test
    fun `a record can be set by an exam that is still a fail`() {
        val previousBest = ResultMilestones.bestExamScore(listOf(exam(15)))
        assertThat(ResultMilestones.isNewExamRecord(score = 20, previousBest = previousBest)).isTrue()
    }
}
