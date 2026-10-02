package com.jesuskrastev.bali.ui.screens.test

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.util.LevelCalculator
import org.junit.Test

/** How a result is read, which sound it opens with, and the order its celebrations play in. */
class ResultFeedbackTest {

    private fun lesson(accuracy: Int) = ResultTier.of(ResultKind.LESSON, accuracy, 0, 0)
    private fun game(accuracy: Int) = ResultTier.of(ResultKind.GAME, accuracy, 0, 0)
    private fun exam(score: Int) = ResultTier.of(ResultKind.EXAM, score * 100 / 30, score, 30)

    @Test
    fun `a lesson is read by its accuracy band`() {
        assertThat(lesson(100)).isEqualTo(ResultTier.LESSON_PERFECT)
        assertThat(lesson(99)).isEqualTo(ResultTier.LESSON_EXCELLENT)
        assertThat(lesson(90)).isEqualTo(ResultTier.LESSON_EXCELLENT)
        assertThat(lesson(89)).isEqualTo(ResultTier.LESSON_GOOD)
        assertThat(lesson(70)).isEqualTo(ResultTier.LESSON_GOOD)
        assertThat(lesson(69)).isEqualTo(ResultTier.LESSON_FAIR)
        assertThat(lesson(50)).isEqualTo(ResultTier.LESSON_FAIR)
        assertThat(lesson(49)).isEqualTo(ResultTier.LESSON_LOW)
        assertThat(lesson(0)).isEqualTo(ResultTier.LESSON_LOW)
    }

    @Test
    fun `a game is read by its accuracy band`() {
        assertThat(game(100)).isEqualTo(ResultTier.GAME_PERFECT)
        assertThat(game(80)).isEqualTo(ResultTier.GAME_HIGH)
        assertThat(game(60)).isEqualTo(ResultTier.GAME_MID)
        assertThat(game(40)).isEqualTo(ResultTier.GAME_LOW)
    }

    @Test
    fun `an exam is read with the DGT pass mark, not the percentage`() {
        assertThat(exam(30)).isEqualTo(ResultTier.EXAM_PERFECT)
        assertThat(exam(29)).isEqualTo(ResultTier.EXAM_PASSED)
        assertThat(exam(27)).isEqualTo(ResultTier.EXAM_PASSED)
        assertThat(exam(26)).isEqualTo(ResultTier.EXAM_CLOSE)
        assertThat(exam(24)).isEqualTo(ResultTier.EXAM_CLOSE)
        assertThat(exam(23)).isEqualTo(ResultTier.EXAM_FAILED)
        assertThat(exam(18)).isEqualTo(ResultTier.EXAM_FAILED)
        assertThat(exam(17)).isEqualTo(ResultTier.EXAM_FAR)
        assertThat(exam(0)).isEqualTo(ResultTier.EXAM_FAR)
    }

    @Test
    fun `a failed exam is never celebrated, however high its percentage`() {
        // 26 of 30 is 86 %, above the 70 % that celebrates a lesson, and still a fail.
        assertThat(exam(26).isCelebrated).isFalse()
        for (score in 0..26) assertThat(exam(score).isCelebrated).isFalse()
        for (score in 27..30) assertThat(exam(score).isCelebrated).isTrue()
    }

    @Test
    fun `lessons and games celebrate from 70 percent, as the confetti does`() {
        assertThat(lesson(70).isCelebrated).isTrue()
        assertThat(lesson(69).isCelebrated).isFalse()
        assertThat(game(70).isCelebrated).isTrue()
        assertThat(game(60).isCelebrated).isFalse()
    }

    @Test
    fun `only a celebrated result opens with the fanfare`() {
        for (tier in ResultTier.entries) {
            val expected = if (tier.isCelebrated) ResultSound.LESSON_COMPLETE else ResultSound.SOFT_FINISH
            assertThat(tier.sound).isEqualTo(expected)
        }
        assertThat(exam(26).sound).isEqualTo(ResultSound.SOFT_FINISH)
    }

    @Test
    fun `every tier belongs to its own kind`() {
        assertThat(ResultTier.entries.count { it.kind == ResultKind.LESSON }).isEqualTo(5)
        assertThat(ResultTier.entries.count { it.kind == ResultKind.GAME }).isEqualTo(4)
        assertThat(ResultTier.entries.count { it.kind == ResultKind.EXAM }).isEqualTo(5)
    }

    @Test
    fun `pace is fast at 15 seconds a question or less`() {
        assertThat(ResultPace.of(durationSeconds = 150, questions = 10)).isEqualTo(ResultPace.FAST)
        assertThat(ResultPace.of(durationSeconds = 151, questions = 10)).isEqualTo(ResultPace.NORMAL)
        assertThat(ResultPace.of(durationSeconds = 100, questions = 0)).isEqualTo(ResultPace.NORMAL)
        assertThat(ResultPace.of(durationSeconds = 0, questions = 10)).isEqualTo(ResultPace.NORMAL)
    }

    @Test
    fun `the first win comes before a record, which comes before a level`() {
        assertThat(ResultHeadline.of(isFirstWin = true, isNewRecord = true, leveledUp = true))
            .isEqualTo(ResultHeadline.FIRST_WIN)
        assertThat(ResultHeadline.of(isFirstWin = false, isNewRecord = true, leveledUp = true))
            .isEqualTo(ResultHeadline.NEW_RECORD)
        assertThat(ResultHeadline.of(isFirstWin = false, isNewRecord = false, leveledUp = true))
            .isEqualTo(ResultHeadline.LEVEL_UP)
        assertThat(ResultHeadline.of(isFirstWin = false, isNewRecord = false, leveledUp = false)).isNull()
    }

    @Test
    fun `only one full-screen celebration is ever chosen`() {
        // A passed exam that is a record and a new level: one headline, not three in a row.
        val headline = ResultHeadline.of(isFirstWin = false, isNewRecord = true, leveledUp = true)
        assertThat(ResultPhase.COUNTING.next(headline)).isEqualTo(ResultPhase.HEADLINE)
        assertThat(ResultPhase.HEADLINE.next(headline)).isEqualTo(ResultPhase.DONE)
    }

    @Test
    fun `the phases go stamp, counting, headline, done and skip what is not there`() {
        assertThat(ResultPhase.first(isPassedExam = true)).isEqualTo(ResultPhase.STAMP)
        assertThat(ResultPhase.first(isPassedExam = false)).isEqualTo(ResultPhase.COUNTING)
        assertThat(ResultPhase.STAMP.next(ResultHeadline.LEVEL_UP)).isEqualTo(ResultPhase.COUNTING)
        assertThat(ResultPhase.COUNTING.next(null)).isEqualTo(ResultPhase.DONE)
        assertThat(ResultPhase.DONE.next(ResultHeadline.LEVEL_UP)).isEqualTo(ResultPhase.DONE)
    }

    @Test
    fun `the review waits for done, which comes only after the headline`() {
        var phase = ResultPhase.first(isPassedExam = true)
        val seen = mutableListOf(phase)
        while (phase != ResultPhase.DONE) {
            phase = phase.next(ResultHeadline.NEW_RECORD)
            seen += phase
        }
        assertThat(seen).containsExactly(
            ResultPhase.STAMP, ResultPhase.COUNTING, ResultPhase.HEADLINE, ResultPhase.DONE
        ).inOrder()
    }

    @Test
    fun `the level bar starts where the user was and fills with the experience`() {
        val start = LevelCalculator.totalXpForLevel(3) + 5
        val before = LevelProgress.at(start.toFloat())
        val after = LevelProgress.at((start + 10).toFloat())
        assertThat(before.level).isEqualTo(3)
        assertThat(after.progress).isGreaterThan(before.progress)
        assertThat(after.xpToNextLevel).isEqualTo(before.xpToNextLevel - 10)
    }

    @Test
    fun `the level bar starts again from empty when a level is crossed`() {
        val levelFour = LevelCalculator.totalXpForLevel(4)
        val justBefore = LevelProgress.at(levelFour - 1f)
        val atLevel = LevelProgress.at(levelFour.toFloat())
        assertThat(justBefore.level).isEqualTo(3)
        assertThat(justBefore.progress).isGreaterThan(0.9f)
        assertThat(atLevel.level).isEqualTo(4)
        assertThat(atLevel.progress).isEqualTo(0f)
    }

    @Test
    fun `the level bar reads zero experience and never goes outside 0 to 1`() {
        val zero = LevelProgress.at(0f)
        assertThat(zero.level).isEqualTo(0)
        assertThat(zero.progress).isEqualTo(0f)
        assertThat(LevelProgress.at(-50f)).isEqualTo(zero)
        for (xp in 0..2_000 step 7) {
            assertThat(LevelProgress.at(xp.toFloat()).progress).isIn(com.google.common.collect.Range.closed(0f, 1f))
        }
    }

    @Test
    fun `the experience count takes longer for more experience within limits`() {
        assertThat(xpCountDurationMs(0)).isEqualTo(600)
        assertThat(xpCountDurationMs(20)).isEqualTo(900)
        assertThat(xpCountDurationMs(1_000)).isEqualTo(1_500)
    }

    @Test
    fun `the exam score line counts mistakes`() {
        assertThat(examScoreLine(28, 30)).isEqualTo("28 de 30 · 2 fallos")
        assertThat(examScoreLine(29, 30)).isEqualTo("29 de 30 · 1 fallo")
        assertThat(examScoreLine(30, 30)).isEqualTo("30 de 30 · 0 fallos")
    }
}
