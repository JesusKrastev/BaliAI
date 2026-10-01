package com.jesuskrastev.bali.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DailyStreakTest {

    private lateinit var originalTimeZone: TimeZone

    @Before
    fun useSpanishTime() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    /**
     * A moment on a given day.
     *
     * @param month 1-based month
     * @param day day of the month
     * @param hour local hour
     * @return epoch millis of that local moment in 2026
     */
    private fun at(month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, month - 1, day, hour, 0)
        }.timeInMillis

    private val empty = DailyStreak(
        current = 0,
        highest = 0,
        freezes = 0,
        lastPracticeMillis = 0,
        practiceDays = emptyList(),
        frozenDays = emptyList()
    )

    /**
     * Studies on each of [days] of October at noon, starting from [start].
     *
     * @return the streak after the last session
     */
    private fun studiedOn(vararg days: Int, start: DailyStreak = empty): DailyStreak =
        days.fold(start) { streak, day -> streak.practicedAt(at(10, day)) }

    @Test
    fun `the first session starts the streak and sets the record`() {
        val streak = studiedOn(5)

        assertThat(streak.current).isEqualTo(1)
        assertThat(streak.highest).isEqualTo(1)
        assertThat(streak.hasPracticedOn(at(10, 5, hour = 22))).isTrue()
    }

    @Test
    fun `a second session on the same day changes nothing`() {
        val once = studiedOn(5)

        assertThat(once.practicedAt(at(10, 5, hour = 20))).isEqualTo(once)
    }

    @Test
    fun `studying on consecutive days adds one per day`() {
        assertThat(studiedOn(5, 6, 7).current).isEqualTo(3)
    }

    @Test
    fun `a missed day is covered by a freeze and shown as frozen`() {
        val streak = studiedOn(5, 6, start = empty.copy(freezes = 2)).practicedAt(at(10, 8))

        assertThat(streak.current).isEqualTo(3)
        assertThat(streak.freezes).isEqualTo(1)
        assertThat(streak.frozenDays.map(DailyStreak::epochDay))
            .containsExactly(DailyStreak.epochDay(at(10, 7)))
    }

    @Test
    fun `more missed days than freezes end the streak and keep the freezes`() {
        val settled = studiedOn(5, 6, start = empty.copy(freezes = 1)).settledAt(at(10, 9))

        assertThat(settled.current).isEqualTo(0)
        assertThat(settled.freezes).isEqualTo(1)
        assertThat(settled.frozenDays).isEmpty()
    }

    @Test
    fun `after the streak ends, the next session starts again at one and keeps the record`() {
        val streak = studiedOn(5, 6, 7, 10)

        assertThat(streak.current).isEqualTo(1)
        assertThat(streak.highest).isEqualTo(3)
    }

    @Test
    fun `yesterday's study keeps the streak alive until today ends`() {
        val settled = studiedOn(5, 6).settledAt(at(10, 7, hour = 23))

        assertThat(settled.current).isEqualTo(2)
        assertThat(settled.hasPracticedOn(at(10, 7))).isFalse()
    }

    @Test
    fun `settling twice spends a freeze only once`() {
        val once = studiedOn(5, start = empty.copy(freezes = 2)).settledAt(at(10, 7))

        assertThat(once.settledAt(at(10, 7, hour = 18))).isEqualTo(once)
        assertThat(once.freezes).isEqualTo(1)
    }

    @Test
    fun `without a streak there is nothing to protect`() {
        val settled = empty.copy(freezes = 2).settledAt(at(10, 20))

        assertThat(settled.freezes).isEqualTo(2)
        assertThat(settled.frozenDays).isEmpty()
    }

    @Test
    fun `the change to summer time does not break the streak`() {
        // In Spain, 29 March 2026 has 23 hours.
        val streak = listOf(at(3, 28, hour = 23), at(3, 29, hour = 23), at(3, 30, hour = 0))
            .fold(empty) { acc, moment -> acc.practicedAt(moment) }

        assertThat(streak.current).isEqualTo(3)
    }

    @Test
    fun `history older than the kept window is dropped`() {
        val streak = studiedOn(1).practicedAt(at(11, 20))

        assertThat(streak.practiceDays.map(DailyStreak::epochDay))
            .containsExactly(DailyStreak.epochDay(at(11, 20)))
    }

    @Test
    fun `history rebuilds the run ending today`() {
        val days = listOf(at(10, 3), at(10, 5), at(10, 6), at(10, 7)).map { DailyStreak.startOfDayMillis(DailyStreak.epochDay(it)) }

        assertThat(DailyStreak.fromHistory(days, at(10, 7, hour = 20))).isEqualTo(3)
    }

    @Test
    fun `history rebuilds the run ending yesterday when today has no study yet`() {
        val days = listOf(at(10, 5), at(10, 6)).map { DailyStreak.startOfDayMillis(DailyStreak.epochDay(it)) }

        assertThat(DailyStreak.fromHistory(days, at(10, 7, hour = 9))).isEqualTo(2)
    }

    @Test
    fun `history with a gap before yesterday gives no streak`() {
        val days = listOf(at(10, 4)).map { DailyStreak.startOfDayMillis(DailyStreak.epochDay(it)) }

        assertThat(DailyStreak.fromHistory(days, at(10, 7))).isEqualTo(0)
    }

    @Test
    fun `days round-trip through their stored form`() {
        val day = DailyStreak.epochDay(at(10, 25, hour = 15))

        assertThat(DailyStreak.epochDay(DailyStreak.startOfDayMillis(day))).isEqualTo(day)
    }
}
