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

    @Test
    fun `the streak is lost at midnight, not before`() {
        val streak = studiedOn(5, 6)

        assertThat(streak.settledAt(at(10, 7, hour = 23)).current).isEqualTo(2)
        assertThat(streak.settledAt(at(10, 8, hour = 0)).current).isEqualTo(0)
    }

    @Test
    fun `missing one day without a freeze keeps the lost streak to recover`() {
        val settled = studiedOn(5, 6, 7).settledAt(at(10, 9))

        assertThat(settled.current).isEqualTo(0)
        assertThat(settled.lostStreak).isEqualTo(3)
        assertThat(DailyStreak.epochDay(settled.lostStreakDayMillis))
            .isEqualTo(DailyStreak.epochDay(at(10, 8)))
        assertThat(settled.recoverableStreakAt(at(10, 9))).isEqualTo(3)
    }

    @Test
    fun `a streak lost more than a day ago cannot be recovered`() {
        val settled = studiedOn(5, 6, 7).settledAt(at(10, 10))

        assertThat(settled.lostStreak).isEqualTo(0)
        assertThat(settled.recoverableStreakAt(at(10, 10))).isEqualTo(0)
    }

    @Test
    fun `the recovery window closes at the next midnight`() {
        val settled = studiedOn(5, 6, 7).settledAt(at(10, 9))

        assertThat(settled.recoverableStreakAt(at(10, 9, hour = 23))).isEqualTo(3)
        assertThat(settled.settledAt(at(10, 10, hour = 0)).lostStreak).isEqualTo(0)
        assertThat(settled.settledAt(at(10, 10, hour = 0)).recoverableStreakAt(at(10, 10, hour = 0)))
            .isEqualTo(0)
    }

    @Test
    fun `settling twice keeps the same lost streak`() {
        val once = studiedOn(5, 6, 7).settledAt(at(10, 9))

        assertThat(once.settledAt(at(10, 9, hour = 20))).isEqualTo(once)
    }

    @Test
    fun `a covered day loses nothing and leaves nothing to recover`() {
        val settled = studiedOn(5, 6, start = empty.copy(freezes = 1)).settledAt(at(10, 8))

        assertThat(settled.current).isEqualTo(2)
        assertThat(settled.lostStreak).isEqualTo(0)
    }

    @Test
    fun `recovering restores the whole count and covers the missed day`() {
        val recovered = studiedOn(5, 6, 7).recoveredAt(at(10, 9))

        assertThat(recovered.current).isEqualTo(3)
        assertThat(recovered.highest).isEqualTo(3)
        assertThat(recovered.lostStreak).isEqualTo(0)
        assertThat(recovered.frozenDays.map(DailyStreak::epochDay))
            .containsExactly(DailyStreak.epochDay(at(10, 8)))
        assertThat(recovered.freezes).isEqualTo(0)
    }

    @Test
    fun `studying after recovering adds to the restored streak`() {
        val streak = studiedOn(5, 6, 7).recoveredAt(at(10, 9)).practicedAt(at(10, 9, hour = 20))

        assertThat(streak.current).isEqualTo(4)
        assertThat(streak.highest).isEqualTo(4)
    }

    @Test
    fun `recovering after already studying today counts today too`() {
        val studiedToday = studiedOn(5, 6, 7).practicedAt(at(10, 9))
        assertThat(studiedToday.current).isEqualTo(1)
        assertThat(studiedToday.recoverableStreakAt(at(10, 9, hour = 20))).isEqualTo(3)

        val recovered = studiedToday.recoveredAt(at(10, 9, hour = 20))

        assertThat(recovered.current).isEqualTo(4)
        assertThat(recovered.highest).isEqualTo(4)
        assertThat(recovered.lostStreak).isEqualTo(0)
    }

    @Test
    fun `a recovered streak survives the following days like any other`() {
        val recovered = studiedOn(5, 6, 7).recoveredAt(at(10, 9))

        assertThat(recovered.settledAt(at(10, 9, hour = 23)).current).isEqualTo(3)
        assertThat(recovered.settledAt(at(10, 10)).current).isEqualTo(0)
        assertThat(recovered.settledAt(at(10, 10)).recoverableStreakAt(at(10, 10))).isEqualTo(3)
    }

    @Test
    fun `recovering twice or with nothing lost changes nothing`() {
        val recovered = studiedOn(5, 6, 7).recoveredAt(at(10, 9))

        assertThat(recovered.recoveredAt(at(10, 9, hour = 12))).isEqualTo(recovered)
        assertThat(studiedOn(5, 6).recoveredAt(at(10, 7))).isEqualTo(studiedOn(5, 6))
    }

    @Test
    fun `recovering too late changes nothing`() {
        val lost = studiedOn(5, 6, 7).settledAt(at(10, 9))

        assertThat(lost.recoveredAt(at(10, 10)).current).isEqualTo(0)
    }

    @Test
    fun `a recovery does not touch the record when the streak was below it`() {
        val record = studiedOn(1, 2, 3, 4, 5, 6)
        val lower = studiedOn(8, 9, start = record)

        val recovered = lower.recoveredAt(at(10, 11))

        assertThat(recovered.current).isEqualTo(2)
        assertThat(recovered.highest).isEqualTo(6)
    }
}
