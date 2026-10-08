package com.jesuskrastev.bali.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.domain.model.ReadinessLevel
import com.jesuskrastev.bali.domain.model.TestResult
import org.junit.Test
import java.util.Date

class CalculateReadinessUseCaseTest {

    private val useCase = CalculateReadinessUseCase()
    private val now = 1_800_000_000_000L
    private val dayMillis = 24 * 60 * 60 * 1000L

    /** An official-exam result with [score] correct answers, finished [daysAgo] days before [now]. */
    private fun exam(score: Int, daysAgo: Int = 0, total: Int = 30) = TestResult(
        category = ExamRules.OFFICIAL_EXAM_CATEGORY,
        score = score,
        total = total,
        date = Date(now - daysAgo * dayMillis),
        isPassed = ExamRules.isPassed(score)
    )

    @Test
    fun `without mock exams there is no verdict and three are missing`() {
        val result = useCase(emptyList(), now)

        assertThat(result.level).isEqualTo(ReadinessLevel.NOT_ENOUGH_DATA)
        assertThat(result.passProbability).isNull()
        assertThat(result.mocksMissing).isEqualTo(3)
        assertThat(result.averageScore).isNull()
        assertThat(result.bestScore).isNull()
    }

    @Test
    fun `two perfect mock exams are still not enough data`() {
        val result = useCase(listOf(exam(30, 1), exam(30, 0)), now)

        assertThat(result.level).isEqualTo(ReadinessLevel.NOT_ENOUGH_DATA)
        assertThat(result.passProbability).isNull()
        assertThat(result.mocksMissing).isEqualTo(1)
    }

    @Test
    fun `practice sessions never count as mock exams`() {
        val practice = TestResult("", "Señales", 10, 10, Date(now), true)

        val result = useCase(listOf(practice, practice, practice, practice), now)

        assertThat(result.mocksTaken).isEqualTo(0)
        assertThat(result.level).isEqualTo(ReadinessLevel.NOT_ENOUGH_DATA)
    }

    @Test
    fun `three near perfect mock exams mean ready`() {
        val result = useCase(listOf(exam(29, 2), exam(30, 1), exam(29, 0)), now)

        assertThat(result.level).isEqualTo(ReadinessLevel.READY)
        assertThat(result.passProbability).isGreaterThan(0.9f)
    }

    @Test
    fun `three comfortable but unsafe mock exams mean almost`() {
        val result = useCase(listOf(exam(28, 2), exam(28, 1), exam(28, 0)), now)

        assertThat(result.level).isEqualTo(ReadinessLevel.ALMOST)
        assertThat(result.passProbability).isAtLeast(0.6f)
        assertThat(result.passProbability).isLessThan(0.9f)
    }

    @Test
    fun `scores far from the pass mark mean not yet`() {
        val result = useCase(listOf(exam(20, 2), exam(21, 1), exam(19, 0)), now)

        assertThat(result.level).isEqualTo(ReadinessLevel.NOT_YET)
        assertThat(result.passProbability).isLessThan(0.1f)
    }

    @Test
    fun `ready needs the latest mock exam to be a pass`() {
        val result = useCase(listOf(exam(30, 3), exam(30, 2), exam(30, 1), exam(26, 0)), now)

        assertThat(result.passProbability).isGreaterThan(0.9f)
        assertThat(result.level).isEqualTo(ReadinessLevel.ALMOST)
    }

    @Test
    fun `old mock exams weigh less than recent ones`() {
        val recent = useCase(listOf(exam(29, 2), exam(29, 1), exam(29, 0)), now)
        val stale = useCase(listOf(exam(29, 62), exam(29, 61), exam(29, 60)), now)

        assertThat(stale.passProbability).isLessThan(recent.passProbability)
    }

    @Test
    fun `only the last five mock exams are recent and they are listed oldest first`() {
        val results = (7 downTo 1).map { exam(score = 20 + it, daysAgo = it) }

        val result = useCase(results, now)

        assertThat(result.history).hasSize(7)
        assertThat(result.recent).hasSize(5)
        assertThat(result.recent.map { it.score }).containsExactly(25, 24, 23, 22, 21).inOrder()
    }

    @Test
    fun `passed in recent counts only the passes among the last five`() {
        val results = listOf(exam(20, 5), exam(28, 4), exam(27, 3), exam(26, 2), exam(30, 1), exam(29, 0))

        val result = useCase(results, now)

        assertThat(result.recent.map { it.passed }).containsExactly(true, true, false, true, true).inOrder()
        assertThat(result.passedInRecent).isEqualTo(4)
    }

    @Test
    fun `average best and trend come from the whole history`() {
        val results = listOf(exam(20, 6), exam(22, 5), exam(24, 4), exam(26, 3), exam(28, 2), exam(30, 1))

        val result = useCase(results, now)

        assertThat(result.averageScore).isWithin(0.001f).of(25f)
        assertThat(result.bestScore).isEqualTo(30)
        // Latest three average 28, the three before average 22.
        assertThat(result.trend).isWithin(0.001f).of(6f)
    }

    @Test
    fun `trend needs four mock exams`() {
        val result = useCase(listOf(exam(20, 2), exam(25, 1), exam(30, 0)), now)

        assertThat(result.trend).isNull()
    }

    @Test
    fun `the pass probability rises with accuracy and stays within zero and one`() {
        val zero = useCase.passProbability(0.0)
        val low = useCase.passProbability(0.6)
        val high = useCase.passProbability(0.95)
        val one = useCase.passProbability(1.0)

        assertThat(zero).isEqualTo(0.0)
        assertThat(one).isWithin(1e-9).of(1.0)
        assertThat(low).isLessThan(high)
        assertThat(high).isAtMost(1.0)
        assertThat(high).isAtLeast(0.0)
    }

    @Test
    fun `a pass at exactly twenty seven correct answers is counted`() {
        val result = useCase(listOf(exam(27, 2), exam(27, 1), exam(27, 0)), now)

        assertThat(result.recent.all { it.passed }).isTrue()
        assertThat(result.passedInRecent).isEqualTo(3)
    }
}
