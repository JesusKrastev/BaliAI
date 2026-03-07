package com.jesuskrastev.bali.domain.util

import java.util.Calendar
import javax.inject.Inject

open class DateTimeHelper @Inject constructor() {
    companion object {
        private const val MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000L
    }

    open fun getDaysBetween(fromTimestamp: Long, toTimestamp: Long): Long {
        val startOfFromDay = getStartOfDay(fromTimestamp)
        val startOfToDay = getStartOfDay(toTimestamp)

        val differenceInMillis = startOfToDay - startOfFromDay
        return differenceInMillis / MILLISECONDS_PER_DAY
    }

    open fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        if (timestamp1 == 0L || timestamp2 == 0L) return false

        val day1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val day2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

        return day1.get(Calendar.YEAR) == day2.get(Calendar.YEAR) &&
                day1.get(Calendar.DAY_OF_YEAR) == day2.get(Calendar.DAY_OF_YEAR)
    }

    open fun getStartOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    open fun getStartOfWeek(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Move to Monday of current week
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val daysToSubtract = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
            add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        }.timeInMillis
    }
}