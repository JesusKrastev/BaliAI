package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.util.DateTimeHelper
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class IncrementStreakUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val dateTimeHelper: DateTimeHelper
) {
    suspend operator fun invoke(): Int {
        val user = userRepository.get().first() ?: return -1
        val currentTimestamp = System.currentTimeMillis()

        // 2. Calcular el timestamp del inicio de la semana actual (lunes 00:00)
        val actualWeekStart = dateTimeHelper.getStartOfWeek(currentTimestamp)

        // 3. Manejar paso de semana (el usuario está en una semana nueva pero la app/Cloud Function no se ha actualizado)
        var currentWeekSessions = user.weekSessions
        var userWeekStart = user.currentWeekStart
        var practiceDays = user.practiceDays

        if (userWeekStart != actualWeekStart) {
            // Week has changed - reset sessions and filter out old practice days
            currentWeekSessions = 0
            userWeekStart = actualWeekStart
            // Remove practice days from previous weeks
            practiceDays = practiceDays.filter { day ->
                dateTimeHelper.getStartOfWeek(day) == actualWeekStart
            }
        }

        // 3b. Validate that weekSessions matches the actual count of practice days this week
        val actualSessionsThisWeek = practiceDays.size
        if (currentWeekSessions != actualSessionsThisWeek) {
            // Sync weekSessions with actual practice day count
            currentWeekSessions = actualSessionsThisWeek
        }

        // 4. Calcular el inicio del día actual (para ver si ya practicó hoy)
        val startOfToday = dateTimeHelper.getStartOfDay(currentTimestamp)

        // 5. Si ya practicó hoy, retorna -1
        val hasAlreadyPracticedToday = practiceDays.contains(startOfToday)
        if (hasAlreadyPracticedToday) return -1

        // 6. Si no practicó hoy, incrementa weekSessions y añade a practiceDays
        currentWeekSessions += 1
        val updatedPracticeDays = practiceDays + startOfToday

        // 7. Actualizar repositorio
        userRepository.updateWeeklyProgress(
            weekSessions = currentWeekSessions,
            currentWeekStart = userWeekStart,
            lastPracticeTimestamp = currentTimestamp,
            practiceDays = updatedPracticeDays
        )

        // 8 y 9. Retorna el nuevo weekSessions (el currentStreak se maneja en Cloud Function)
        return currentWeekSessions
    }
}