package com.jesuskrastev.bali.ui.screens.test

import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.domain.util.LevelCalculator

/** What was finished, which decides how its score is read on the result screen. */
enum class ResultKind {
    /** A lesson, review or section test from the learning path. */
    LESSON,

    /** An arcade mini-game. */
    GAME,

    /** The official 30-question mock exam, read with the DGT pass mark. */
    EXAM
}

/**
 * How a result is read, by kind and score band. Lessons and mini-games go by accuracy; the mock
 * exam goes by the DGT rule (at most [ExamRules.MAX_MISTAKES] mistakes), so a failed exam is
 * never read as a good result, whatever its percentage.
 *
 * @property kind what was finished
 * @property isCelebrated whether the result is celebrated (confetti or stamp, the fanfare); false
 *   for a failed exam and for lessons and games below the 70 % that unlocks the next lesson
 */
enum class ResultTier(val kind: ResultKind, val isCelebrated: Boolean) {
    /** 100 %. */
    LESSON_PERFECT(ResultKind.LESSON, true),

    /** 90–99 %. */
    LESSON_EXCELLENT(ResultKind.LESSON, true),

    /** 70–89 %: the lesson is passed and the next one opens. */
    LESSON_GOOD(ResultKind.LESSON, true),

    /** 50–69 %. */
    LESSON_FAIR(ResultKind.LESSON, false),

    /** Below 50 %. */
    LESSON_LOW(ResultKind.LESSON, false),

    /** Every round won. */
    GAME_PERFECT(ResultKind.GAME, true),

    /** 70–99 % (with five rounds, four won). */
    GAME_HIGH(ResultKind.GAME, true),

    /** 50–69 % (three rounds won). */
    GAME_MID(ResultKind.GAME, false),

    /** Below 50 %. */
    GAME_LOW(ResultKind.GAME, false),

    /** Passed without a single mistake. */
    EXAM_PERFECT(ResultKind.EXAM, true),

    /** Passed: 27–29 correct. */
    EXAM_PASSED(ResultKind.EXAM, true),

    /** Failed by a little: 24–26 correct. */
    EXAM_CLOSE(ResultKind.EXAM, false),

    /** Failed: 18–23 correct. */
    EXAM_FAILED(ResultKind.EXAM, false),

    /** Failed by a lot: 17 correct or fewer. */
    EXAM_FAR(ResultKind.EXAM, false);

    /** The sound that opens a result in this tier: the fanfare only when it is celebrated. */
    val sound: ResultSound
        get() = if (isCelebrated) ResultSound.LESSON_COMPLETE else ResultSound.SOFT_FINISH

    companion object {
        /** Lowest exam score that still counts as "failed by a little". */
        private const val EXAM_CLOSE_MIN_SCORE = ExamRules.PASS_SCORE - 3

        /** Lowest exam score that is not "failed by a lot" (60 % of the exam). */
        private const val EXAM_FAILED_MIN_SCORE = 18

        /**
         * Reads a result.
         *
         * @param kind what was finished
         * @param accuracy percentage of right answers, used for lessons and games
         * @param score right answers, used for the exam
         * @param total questions, used for the exam's "perfect" band
         * @return the tier the result belongs to
         */
        fun of(kind: ResultKind, accuracy: Int, score: Int, total: Int): ResultTier = when (kind) {
            ResultKind.LESSON -> when {
                accuracy >= 100 -> LESSON_PERFECT
                accuracy >= 90 -> LESSON_EXCELLENT
                accuracy >= 70 -> LESSON_GOOD
                accuracy >= 50 -> LESSON_FAIR
                else -> LESSON_LOW
            }
            ResultKind.GAME -> when {
                accuracy >= 100 -> GAME_PERFECT
                accuracy >= 70 -> GAME_HIGH
                accuracy >= 50 -> GAME_MID
                else -> GAME_LOW
            }
            ResultKind.EXAM -> when {
                ExamRules.isPassed(score) && score >= total -> EXAM_PERFECT
                ExamRules.isPassed(score) -> EXAM_PASSED
                score >= EXAM_CLOSE_MIN_SCORE -> EXAM_CLOSE
                score >= EXAM_FAILED_MIN_SCORE -> EXAM_FAILED
                else -> EXAM_FAR
            }
        }
    }
}

/** How quickly the questions were answered, which adds speed-themed messages to the pool. */
enum class ResultPace {
    NORMAL,

    /** At most [FAST_SECONDS_PER_QUESTION] seconds per question on average. */
    FAST;

    companion object {
        /** Same mark as the top speed bonus, so "fast" means the same thing everywhere. */
        const val FAST_SECONDS_PER_QUESTION = 15

        /**
         * Reads the pace of a test.
         *
         * @param durationSeconds time the test took
         * @param questions questions in the test; 0 when unknown (mini-games, which are timed anyway)
         * @return [FAST] when both are known and the average is at or under the mark
         */
        fun of(durationSeconds: Int, questions: Int): ResultPace =
            if (questions > 0 && durationSeconds > 0 && durationSeconds <= questions * FAST_SECONDS_PER_QUESTION) {
                FAST
            } else {
                NORMAL
            }
    }
}

/** The one sound the result screen plays when it appears. */
enum class ResultSound {
    /** The fanfare (`sfx_lesson_complete`). */
    LESSON_COMPLETE,

    /** The quiet closing tone (`sfx_soft_finish`). */
    SOFT_FINISH
}

/**
 * The full-screen celebrations of a result. Only one is shown per result, so they never pile up
 * after a long exam; the others keep their small form on the screen (the level pill, the record
 * pill). Declared from most to least important.
 */
enum class ResultHeadline {
    /** The user's first win ever (a lesson at 70 % or a passed exam): it happens once in a lifetime. */
    FIRST_WIN,

    /** A mock exam better than every earlier one. */
    NEW_RECORD,

    /** A new level. The most frequent of the three, so it gives way to the others. */
    LEVEL_UP;

    companion object {
        /**
         * Picks the result's full-screen celebration.
         *
         * @param isFirstWin whether this is the user's first win
         * @param isNewRecord whether this exam beats every earlier one
         * @param leveledUp whether the result reached a new level
         * @return the most important one that applies, or null when none does
         */
        fun of(isFirstWin: Boolean, isNewRecord: Boolean, leveledUp: Boolean): ResultHeadline? = when {
            isFirstWin -> FIRST_WIN
            isNewRecord -> NEW_RECORD
            leveledUp -> LEVEL_UP
            else -> null
        }
    }
}

/**
 * The steps of the result screen, played in order so nothing covers anything else: the stamp of
 * a passed exam lands, the experience counts up, then the full-screen celebration, if any. Only
 * when it is [DONE] may the Play review be requested.
 */
enum class ResultPhase {
    /** The "APROBADO" stamp is coming down. */
    STAMP,

    /** The experience counts up and the level bar fills. */
    COUNTING,

    /** The [ResultHeadline] is on screen. */
    HEADLINE,

    /** Everything has played. */
    DONE;

    /**
     * Moves to the next step.
     *
     * @param headline the result's full-screen celebration, or null when it has none
     * @return the step after this one; [DONE] stays [DONE]
     */
    fun next(headline: ResultHeadline?): ResultPhase = when (this) {
        STAMP -> COUNTING
        COUNTING -> if (headline != null) HEADLINE else DONE
        HEADLINE, DONE -> DONE
    }

    companion object {
        /**
         * The step the screen opens on.
         *
         * @param isPassedExam whether a stamp has to land first
         * @return [STAMP] for a passed exam, [COUNTING] otherwise
         */
        fun first(isPassedExam: Boolean): ResultPhase = if (isPassedExam) STAMP else COUNTING
    }
}

/**
 * Where the level bar stands for an amount of experience, fractions included, so the bar moves
 * smoothly while the experience counts up and starts again from empty when a level is crossed.
 *
 * @property level the level reached with that experience
 * @property progress how full the bar towards the next level is, from 0 to 1
 * @property xpToNextLevel whole experience points still missing for the next level
 */
data class LevelProgress(val level: Int, val progress: Float, val xpToNextLevel: Int) {
    companion object {
        /**
         * Reads the level bar at an amount of experience.
         *
         * @param totalXp accumulated experience; negative values count as 0
         * @return the level, the bar's fill and what is missing for the next level
         */
        fun at(totalXp: Float): LevelProgress {
            val xp = totalXp.coerceAtLeast(0f)
            val level = LevelCalculator.calculateLevel(xp.toInt())
            val levelStart = LevelCalculator.totalXpForLevel(level)
            val levelSize = LevelCalculator.xpForLevel(level + 1).coerceAtLeast(1)
            val progress = ((xp - levelStart) / levelSize).coerceIn(0f, 1f)
            val missing = (levelStart + levelSize - xp.toInt()).coerceAtLeast(0)
            return LevelProgress(level, progress, missing)
        }
    }
}

/**
 * Writes an exam's score the way the DGT counts it, in mistakes.
 *
 * @param score correct answers
 * @param total questions in the exam
 * @return for example "28 de 30 · 2 fallos", or "29 de 30 · 1 fallo"
 */
internal fun examScoreLine(score: Int, total: Int): String {
    val mistakes = total - score
    return "$score de $total · ${if (mistakes == 1) "1 fallo" else "$mistakes fallos"}"
}
