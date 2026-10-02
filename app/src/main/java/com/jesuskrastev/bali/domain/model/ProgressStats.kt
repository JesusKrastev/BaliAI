package com.jesuskrastev.bali.domain.model

import com.jesuskrastev.bali.domain.usecase.CalculateProgressStatsUseCase
import com.jesuskrastev.bali.domain.usecase.CalculateReadinessUseCase

/**
 * How ready the user looks for the official exam, from the semaphore shown on the statistics
 * screen. Never promises a result: it describes how the recent mock exams went.
 */
enum class ReadinessLevel {
    /** Too few mock exams to say anything honest. */
    NOT_ENOUGH_DATA,

    /** The chance of passing is low. */
    NOT_YET,

    /** Close, but not safe yet. */
    ALMOST,

    /** The recent mock exams point to a pass. */
    READY
}

/**
 * One finished mock exam, as the history chart draws it.
 *
 * @property dateMillis when it was completed
 * @property score correct answers
 * @property total questions in the exam
 * @property passed whether it met [ExamRules.PASS_SCORE]
 */
data class MockExam(
    val dateMillis: Long,
    val score: Int,
    val total: Int,
    val passed: Boolean
)

/**
 * The answer to "will I pass?".
 *
 * @property level the semaphore
 * @property passProbability chance of passing a 30-question exam from 0 to 1, or null while
 *   [level] is [ReadinessLevel.NOT_ENOUGH_DATA]
 * @property mocksTaken mock exams completed
 * @property mocksMissing mock exams still needed before a verdict is shown
 * @property history every mock exam, oldest first
 * @property recent the last [CalculateReadinessUseCase.RECENT_WINDOW] mock exams, oldest first
 * @property passedInRecent how many of [recent] passed
 * @property averageScore mean correct answers over [history], or null without any
 * @property bestScore best correct answers over [history], or null without any
 * @property trend change in mean score between the latest three mock exams and the three before,
 *   or null with fewer than four
 */
data class ReadinessResult(
    val level: ReadinessLevel,
    val passProbability: Float?,
    val mocksTaken: Int,
    val mocksMissing: Int,
    val history: List<MockExam>,
    val recent: List<MockExam>,
    val passedInRecent: Int,
    val averageScore: Float?,
    val bestScore: Int?,
    val trend: Float?
)

/**
 * How well one topic is going, from the practice sessions whose category names it.
 *
 * @property topic the topic
 * @property correct correct answers across its sessions
 * @property total questions across its sessions
 * @property sessions practice sessions on it
 */
data class TopicMastery(
    val topic: DrivingTopic,
    val correct: Int,
    val total: Int,
    val sessions: Int
) {
    /** Share of correct answers from 0 to 1. */
    val accuracy: Float get() = if (total == 0) 0f else correct.toFloat() / total

    /** True when there are enough questions for [accuracy] to mean something. */
    val isReliable: Boolean get() = total >= MIN_QUESTIONS

    companion object {
        /** Questions a topic needs before it is rated, so one lucky session does not read as mastery. */
        const val MIN_QUESTIONS = 5

        /** Accuracy from which a topic counts as strong. */
        const val STRONG_ACCURACY = 0.9f

        /** Accuracy below which a topic counts as weak. */
        const val WEAK_ACCURACY = 0.7f
    }
}

/**
 * Questions answered on one calendar day, for the activity chart.
 *
 * @property dayMillis local midnight of the day
 * @property questions questions answered that day
 * @property correct correct ones among them
 * @property isToday whether it is today
 */
data class DayActivity(
    val dayMillis: Long,
    val questions: Int,
    val correct: Int,
    val isToday: Boolean
)

/**
 * Everything the statistics screen shows.
 *
 * @property readiness the verdict and mock exam history
 * @property topics topics with practice data, weakest first
 * @property totalQuestions questions answered, mock exams included
 * @property correctQuestions correct answers among them
 * @property practiceSessions practice sessions finished (mock exams excluded)
 * @property week the last seven days, oldest first
 * @property studyDays the last [CalculateProgressStatsUseCase.CALENDAR_DAYS] days, oldest first,
 *   each flagged true when the user studied
 * @property studyDaysCount how many of [studyDays] were study days
 * @property currentStreak current streak momentum, settled to today
 * @property highestStreak highest streak momentum ever reached
 * @property level the user's level
 * @property xp the user's experience points
 */
data class ProgressStats(
    val readiness: ReadinessResult,
    val topics: List<TopicMastery>,
    val totalQuestions: Int,
    val correctQuestions: Int,
    val practiceSessions: Int,
    val week: List<DayActivity>,
    val studyDays: List<Boolean>,
    val studyDaysCount: Int,
    val currentStreak: Int,
    val highestStreak: Int,
    val level: Int,
    val xp: Int
) {
    /** Share of correct answers from 0 to 1, or null before the first answer. */
    val accuracy: Float? get() =
        if (totalQuestions == 0) null else correctQuestions.toFloat() / totalQuestions
}
