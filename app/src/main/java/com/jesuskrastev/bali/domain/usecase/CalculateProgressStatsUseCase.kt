package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.DayActivity
import com.jesuskrastev.bali.domain.model.DrivingTopic
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.domain.model.ProgressStats
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.model.TopicMastery
import com.jesuskrastev.bali.domain.model.User
import javax.inject.Inject

/**
 * Turns the user's raw history (test results, answers and profile) into the figures the
 * statistics screen shows: the exam verdict, how each topic is going, how much they have
 * practiced and how consistent they are.
 *
 * Pure Kotlin over plain lists, so it runs offline over whatever Room or Firestore returned.
 */
class CalculateProgressStatsUseCase @Inject constructor(
    private val calculateReadiness: CalculateReadinessUseCase
) {

    /**
     * Builds the statistics.
     *
     * @param results every test result of the user
     * @param answers every answer of the user
     * @param user the profile, or null when there is none yet
     * @param nowMillis the current time
     * @return the figures to render
     */
    operator fun invoke(
        results: List<TestResult>,
        answers: List<Answer>,
        user: User?,
        nowMillis: Long
    ): ProgressStats {
        val profile = user ?: User()
        val streak = DailyStreak.of(profile).settledAt(nowMillis)
        val studyDays = studyDaysOf(streak.practiceDays, nowMillis)

        return ProgressStats(
            readiness = calculateReadiness(results, nowMillis),
            topics = topicsOf(results),
            totalQuestions = answers.size,
            correctQuestions = answers.count { it.isCorrect },
            practiceSessions = results.count { it.category != ExamRules.OFFICIAL_EXAM_CATEGORY },
            week = weekOf(answers, nowMillis),
            studyDays = studyDays,
            studyDaysCount = studyDays.count { it },
            currentStreak = streak.current,
            highestStreak = streak.highest,
            level = profile.level,
            xp = profile.xp,
            daysToExam = profile.examDateMillis?.let { daysUntil(it, nowMillis) }
        )
    }

    /**
     * Groups practice sessions by the topic their category names, weakest topic first. Mock
     * exams and categories that name no topic (a lesson with a free-form title) are skipped,
     * since neither says which topic a question belonged to.
     *
     * @param results every test result
     * @return one entry per topic with at least one session, weakest first, unreliable ones last
     */
    private fun topicsOf(results: List<TestResult>): List<TopicMastery> =
        results
            .filter { it.category != ExamRules.OFFICIAL_EXAM_CATEGORY && it.total > 0 }
            .mapNotNull { result -> DrivingTopic.fromCategory(result.category)?.let { it to result } }
            .groupBy({ it.first }, { it.second })
            .map { (topic, sessions) ->
                TopicMastery(
                    topic = topic,
                    correct = sessions.sumOf { it.score },
                    total = sessions.sumOf { it.total },
                    sessions = sessions.size
                )
            }
            .sortedWith(compareByDescending<TopicMastery> { it.isReliable }.thenBy { it.accuracy })

    /**
     * Counts the questions answered on each of the last [WEEK_DAYS] days, today included.
     *
     * @param answers every answer
     * @param nowMillis the current time
     * @return seven entries, oldest first
     */
    private fun weekOf(answers: List<Answer>, nowMillis: Long): List<DayActivity> {
        val today = DailyStreak.epochDay(nowMillis)
        val byDay = answers.groupBy { DailyStreak.epochDay(it.date.time) }
        return (today - (WEEK_DAYS - 1)..today).map { day ->
            val answered = byDay[day].orEmpty()
            DayActivity(
                dayMillis = DailyStreak.startOfDayMillis(day),
                questions = answered.size,
                correct = answered.count { it.isCorrect },
                isToday = day == today
            )
        }
    }

    /**
     * Flags which of the last [CALENDAR_DAYS] days the user studied.
     *
     * @param practiceDays local midnights of the study days
     * @param nowMillis the current time
     * @return one flag per day, oldest first
     */
    private fun studyDaysOf(practiceDays: List<Long>, nowMillis: Long): List<Boolean> {
        val today = DailyStreak.epochDay(nowMillis)
        val studied = practiceDays.map(DailyStreak::epochDay).toSet()
        return (today - (CALENDAR_DAYS - 1)..today).map { it in studied }
    }

    /**
     * Counts the calendar days from today to a date.
     *
     * @param targetMillis the date
     * @param nowMillis the current time
     * @return days remaining, 0 on the day itself, negative once it has passed
     */
    private fun daysUntil(targetMillis: Long, nowMillis: Long): Int =
        (DailyStreak.epochDay(targetMillis) - DailyStreak.epochDay(nowMillis)).toInt()

    companion object {
        /** Days the activity chart covers. */
        const val WEEK_DAYS = 7

        /** Days the study calendar covers: four full weeks, within [DailyStreak.HISTORY_DAYS]. */
        const val CALENDAR_DAYS = 28
    }
}
