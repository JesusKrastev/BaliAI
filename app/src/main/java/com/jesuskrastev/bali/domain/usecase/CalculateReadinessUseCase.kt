package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.domain.model.MockExam
import com.jesuskrastev.bali.domain.model.ReadinessLevel
import com.jesuskrastev.bali.domain.model.ReadinessResult
import com.jesuskrastev.bali.domain.model.TestResult
import javax.inject.Inject
import kotlin.math.pow

/**
 * Estimates the chance of passing the official exam from the mock exams taken so far.
 *
 * The model is deliberately simple so it can be explained to the user:
 * 1. The per-question accuracy is the recency-weighted share of correct answers over every mock
 *    exam (half-life [HALF_LIFE_DAYS] days, so a result from three weeks ago counts a quarter as
 *    much as one from today, which lets a student who is improving see it), pulled slightly toward [PRIOR_ACCURACY] so one great exam cannot
 *    make a beginner look ready.
 * 2. The chance of passing is the probability that a 30-question exam answered with that accuracy
 *    has at most [ExamRules.MAX_MISTAKES] mistakes (a binomial tail).
 *
 * Below [MIN_MOCKS] mock exams no percentage is produced at all: telling someone they are ready
 * and having them fail is worse than telling them to practice more.
 */
class CalculateReadinessUseCase @Inject constructor() {

    /**
     * Builds the verdict.
     *
     * @param results every test result of the user; only official-exam attempts count
     * @param nowMillis the current time, used to weigh recent exams more
     * @return the readiness verdict with the exam history it comes from
     */
    operator fun invoke(results: List<TestResult>, nowMillis: Long): ReadinessResult {
        val history = results
            .filter { it.category == ExamRules.OFFICIAL_EXAM_CATEGORY && it.total > 0 }
            .sortedBy { it.date.time }
            .map { MockExam(it.date.time, it.score, it.total, ExamRules.isPassed(it.score)) }
        val recent = history.takeLast(RECENT_WINDOW)

        val probability = if (history.size >= MIN_MOCKS) {
            passProbability(estimatedAccuracy(history, nowMillis))
        } else {
            null
        }

        return ReadinessResult(
            level = levelOf(probability, recent),
            passProbability = probability?.toFloat(),
            mocksTaken = history.size,
            mocksMissing = (MIN_MOCKS - history.size).coerceAtLeast(0),
            history = history,
            recent = recent,
            passedInRecent = recent.count { it.passed },
            averageScore = history.takeIf { it.isNotEmpty() }?.map { it.score }?.average()?.toFloat(),
            bestScore = history.maxOfOrNull { it.score },
            trend = trendOf(history)
        )
    }

    /**
     * Weighs every mock exam by how recent it is and returns the smoothed share of correct answers.
     *
     * @param history mock exams, any order
     * @param nowMillis the current time
     * @return accuracy per question from 0 to 1
     */
    private fun estimatedAccuracy(history: List<MockExam>, nowMillis: Long): Double {
        var weightedCorrect = PRIOR_ACCURACY * PRIOR_QUESTIONS
        var weightedTotal = PRIOR_QUESTIONS
        history.forEach { exam ->
            val ageDays = ((nowMillis - exam.dateMillis) / DAY_MILLIS.toDouble()).coerceAtLeast(0.0)
            val weight = 0.5.pow(ageDays / HALF_LIFE_DAYS)
            weightedCorrect += weight * exam.score
            weightedTotal += weight * exam.total
        }
        return weightedCorrect / weightedTotal
    }

    /**
     * Chance that an exam answered with [accuracy] per question has at most
     * [ExamRules.MAX_MISTAKES] mistakes.
     *
     * @param accuracy per-question accuracy from 0 to 1
     * @return probability from 0 to 1
     */
    internal fun passProbability(accuracy: Double): Double {
        val n = ExamRules.QUESTION_COUNT
        var tail = 0.0
        for (correct in ExamRules.PASS_SCORE..n) {
            tail += binomialCoefficient(n, correct) *
                accuracy.pow(correct) * (1 - accuracy).pow(n - correct)
        }
        return tail.coerceIn(0.0, 1.0)
    }

    /**
     * Computes the binomial coefficient "n choose k" as a double (exact for n = 30).
     *
     * @param n number of trials
     * @param k number of successes
     * @return n! / (k! (n - k)!)
     */
    private fun binomialCoefficient(n: Int, k: Int): Double {
        var result = 1.0
        for (i in 1..k) result = result * (n - k + i) / i
        return result
    }

    /**
     * Turns the probability into the semaphore. "Ready" additionally needs the latest mock exam
     * to be a pass, so a verdict never contradicts what the user just saw on the result screen.
     *
     * @param probability chance of passing, or null without enough data
     * @param recent the latest mock exams, oldest first
     * @return the level to show
     */
    private fun levelOf(probability: Double?, recent: List<MockExam>): ReadinessLevel = when {
        probability == null -> ReadinessLevel.NOT_ENOUGH_DATA
        probability >= READY_PROBABILITY && recent.lastOrNull()?.passed == true -> ReadinessLevel.READY
        probability >= ALMOST_PROBABILITY -> ReadinessLevel.ALMOST
        else -> ReadinessLevel.NOT_YET
    }

    /**
     * Compares the mean score of the latest three mock exams with the three before them.
     *
     * @param history mock exams, oldest first
     * @return the difference in correct answers, or null with fewer than four exams
     */
    private fun trendOf(history: List<MockExam>): Float? {
        if (history.size < TREND_MIN_MOCKS) return null
        val latest = history.takeLast(TREND_WINDOW).map { it.score }.average()
        val before = history.dropLast(TREND_WINDOW).takeLast(TREND_WINDOW).map { it.score }.average()
        return (latest - before).toFloat()
    }

    companion object {
        /** Mock exams needed before a verdict is shown. */
        const val MIN_MOCKS = 3

        /** Mock exams the "last results" row shows. */
        const val RECENT_WINDOW = 5

        /** Mock exams out of [RECENT_WINDOW] the user should be passing before the real exam. */
        const val RECENT_GOAL = 4

        /** Probability from which the verdict is "ready". */
        const val READY_PROBABILITY = 0.9

        /** Probability from which the verdict is "almost": below it the result is too uncertain to call it close. */
        const val ALMOST_PROBABILITY = 0.6

        private const val HALF_LIFE_DAYS = 10.0
        private const val PRIOR_ACCURACY = 0.7
        private const val PRIOR_QUESTIONS = 10.0
        private const val TREND_WINDOW = 3
        private const val TREND_MIN_MOCKS = 4
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
