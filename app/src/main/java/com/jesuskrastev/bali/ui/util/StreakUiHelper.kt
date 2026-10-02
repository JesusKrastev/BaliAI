package com.jesuskrastev.bali.ui.util

import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.ui.screens.home.DailyStreakState
import com.jesuskrastev.bali.ui.screens.home.StreakStatus
import java.util.Calendar

object StreakUiHelper {

    /** Initials of the days of the week, Monday first, as Spanish calendars write them. */
    private val dayInitials = listOf("L", "M", "X", "J", "V", "S", "D")

    /**
     * Lays out the current week, Monday to Sunday, for the streak screens.
     *
     * A day with study is [StreakStatus.COMPLETED] and one a freeze covered is
     * [StreakStatus.FROZEN]; otherwise today is [StreakStatus.TODAY], later days
     * [StreakStatus.FUTURE] and earlier ones [StreakStatus.FAILED].
     *
     * @param practiceDays local midnights of the days with study
     * @param frozenDays local midnights of the days a streak freeze covered
     * @param nowMillis the current time, which decides the week and today
     * @return seven entries, Monday first
     */
    fun generateWeeklyStreak(
        practiceDays: List<Long>,
        frozenDays: List<Long> = emptyList(),
        nowMillis: Long = System.currentTimeMillis()
    ): List<DailyStreakState> {
        val today = DailyStreak.epochDay(nowMillis)
        val monday = today - daysSinceMonday(nowMillis)
        val studied = practiceDays.map(DailyStreak::epochDay).toSet()
        val frozen = frozenDays.map(DailyStreak::epochDay).toSet()

        return dayInitials.mapIndexed { index, initial ->
            val day = monday + index
            DailyStreakState(
                dayOfWeek = initial,
                dayOfMonth = Calendar.getInstance()
                    .apply { timeInMillis = DailyStreak.startOfDayMillis(day) }
                    .get(Calendar.DAY_OF_MONTH),
                status = when {
                    day in studied -> StreakStatus.COMPLETED
                    day in frozen -> StreakStatus.FROZEN
                    day == today -> StreakStatus.TODAY
                    day > today -> StreakStatus.FUTURE
                    else -> StreakStatus.FAILED
                },
                isToday = day == today
            )
        }
    }

    /**
     * Counts the days between Monday and the day of [millis].
     *
     * @param millis any moment
     * @return 0 on Monday up to 6 on Sunday
     */
    private fun daysSinceMonday(millis: Long): Long {
        val dayOfWeek = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK)
        return ((dayOfWeek + 5) % 7).toLong()
    }
}
