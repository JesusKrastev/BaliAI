package com.jesuskrastev.bali.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.DrivingTopic
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.model.User
import org.junit.Test
import java.util.Calendar
import java.util.Date

class CalculateProgressStatsUseCaseTest {

    private val useCase = CalculateProgressStatsUseCase(CalculateReadinessUseCase())

    /** Wednesday 7 October 2026, 18:00. */
    private val now = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 7, 18, 0) }.timeInMillis

    /** Local midnight of a day relative to [now]. */
    private fun day(offset: Int) = DailyStreak.startOfDayMillis(DailyStreak.epochDay(now) + offset)

    private fun practice(category: String, score: Int, total: Int = 10) =
        TestResult("", category, score, total, Date(now), score * 10 >= total * 9)

    private fun answer(daysAgo: Int, correct: Boolean) =
        Answer("", "t", "q", 0, correct, Date(day(-daysAgo) + 3_600_000))

    @Test
    fun `an empty history gives empty figures and no verdict`() {
        val stats = useCase(emptyList(), emptyList(), null, now)

        assertThat(stats.totalQuestions).isEqualTo(0)
        assertThat(stats.accuracy).isNull()
        assertThat(stats.topics).isEmpty()
        assertThat(stats.practiceSessions).isEqualTo(0)
        assertThat(stats.week).hasSize(7)
        assertThat(stats.studyDays).hasSize(28)
        assertThat(stats.studyDaysCount).isEqualTo(0)
    }

    @Test
    fun `accuracy counts every answer`() {
        val answers = listOf(answer(0, true), answer(0, true), answer(1, false), answer(2, true))

        val stats = useCase(emptyList(), answers, User(), now)

        assertThat(stats.totalQuestions).isEqualTo(4)
        assertThat(stats.correctQuestions).isEqualTo(3)
        assertThat(stats.accuracy).isWithin(0.001f).of(0.75f)
    }

    @Test
    fun `topics are grouped, summed and ordered weakest first`() {
        val results = listOf(
            practice("Señales", 9), practice("Señales de tráfico", 7),
            practice("Velocidad", 10), practice("Maniobras", 4),
            practice("Lección", 10)
        )

        val topics = useCase(results, emptyList(), User(), now).topics

        assertThat(topics.map { it.topic }).containsExactly(
            DrivingTopic.MANEUVERS, DrivingTopic.SIGNS, DrivingTopic.SPEED
        ).inOrder()
        val signs = topics.first { it.topic == DrivingTopic.SIGNS }
        assertThat(signs.correct).isEqualTo(16)
        assertThat(signs.total).isEqualTo(20)
        assertThat(signs.sessions).isEqualTo(2)
    }

    @Test
    fun `mock exams and unclassifiable lessons do not create topics`() {
        val exam = TestResult("", ExamRules.OFFICIAL_EXAM_CATEGORY, 28, 30, Date(now), true)

        val stats = useCase(listOf(exam, practice("Lección", 8)), emptyList(), User(), now)

        assertThat(stats.topics).isEmpty()
        assertThat(stats.practiceSessions).isEqualTo(1)
        assertThat(stats.readiness.mocksTaken).isEqualTo(1)
    }

    @Test
    fun `a topic with too few questions is listed after the reliable ones`() {
        val results = listOf(practice("Alumbrado", 1, total = 3), practice("Mecánica", 9))

        val topics = useCase(results, emptyList(), User(), now).topics

        assertThat(topics.map { it.topic }).containsExactly(DrivingTopic.MECHANICS, DrivingTopic.LIGHTING).inOrder()
        assertThat(topics.last().isReliable).isFalse()
    }

    @Test
    fun `the week lists seven days ending today with the questions of each`() {
        val answers = listOf(answer(0, true), answer(0, false), answer(3, true), answer(20, true))

        val week = useCase(emptyList(), answers, User(), now).week

        assertThat(week).hasSize(7)
        assertThat(week.last().isToday).isTrue()
        assertThat(week.last().questions).isEqualTo(2)
        assertThat(week.last().correct).isEqualTo(1)
        assertThat(week[3].questions).isEqualTo(1)
        assertThat(week.sumOf { it.questions }).isEqualTo(3)
    }

    @Test
    fun `study days are flagged on a calendar ending today`() {
        val user = User(practiceDays = listOf(day(0), day(-1), day(-5), day(-40)))

        val stats = useCase(emptyList(), emptyList(), user, now)

        assertThat(stats.studyDays.last()).isTrue()
        assertThat(stats.studyDays[26]).isTrue()
        assertThat(stats.studyDays[22]).isTrue()
        assertThat(stats.studyDaysCount).isEqualTo(3)
    }

    @Test
    fun `level, xp and streak come from the profile`() {
        val user = User(level = 4, xp = 1240, currentStreak = 3, highestStreak = 5, practiceDays = listOf(day(0)))

        val stats = useCase(emptyList(), emptyList(), user, now)

        assertThat(stats.level).isEqualTo(4)
        assertThat(stats.xp).isEqualTo(1240)
        assertThat(stats.highestStreak).isAtLeast(stats.currentStreak)
    }
}
