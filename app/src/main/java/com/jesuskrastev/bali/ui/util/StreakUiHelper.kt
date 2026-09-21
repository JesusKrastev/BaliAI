package com.jesuskrastev.bali.ui.util

import com.jesuskrastev.bali.domain.util.DateTimeHelper
import com.jesuskrastev.bali.ui.screens.home.DailyStreakState
import com.jesuskrastev.bali.ui.screens.home.StreakStatus
import java.util.Calendar

object StreakUiHelper {
    private val dateTimeHelper = DateTimeHelper()
    
    /**
     * Generates a list of 7 DailyStreakState items representing the current week (Mon-Sun).
     * It marks days as COMPLETED if they exist in [practiceDays].
     */
    fun generateWeeklyStreak(practiceDays: List<Long>): List<DailyStreakState> {
        val currentTimestamp = System.currentTimeMillis()
        val startOfMonday = dateTimeHelper.getStartOfWeek(currentTimestamp)

        val daysOfWeek = listOf("L", "M", "X", "J", "V", "S", "D")
        
        return daysOfWeek.mapIndexed { index, dayName ->
            val dayCalendar = Calendar.getInstance().apply {
                timeInMillis = startOfMonday
                add(Calendar.DAY_OF_YEAR, index)
            }
            val dayTimestamp = dateTimeHelper.getStartOfDay(dayCalendar.timeInMillis)
            
            val isToday = dateTimeHelper.isSameDay(currentTimestamp, dayTimestamp)
            val isFuture = dayTimestamp > dateTimeHelper.getStartOfDay(currentTimestamp)
            val isCompleted = practiceDays.any { dateTimeHelper.isSameDay(it, dayTimestamp) }
            
            val status = when {
                isCompleted -> StreakStatus.COMPLETED
                isToday -> StreakStatus.TODAY
                isFuture -> StreakStatus.FUTURE
                else -> StreakStatus.FAILED
            }
            
            DailyStreakState(
                dayOfWeek = dayName,
                dayOfMonth = dayCalendar.get(Calendar.DAY_OF_MONTH),
                status = status,
                isToday = isToday
            )
        }
    }
}
