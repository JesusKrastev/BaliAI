package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.data.analytics.FirebaseAnalyticsTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AppInitializationUseCase @Inject constructor(
    private val executeFirestoreMigrationsUseCase: ExecuteFirestoreMigrationsUseCase,
    private val resetStreakUseCase: ResetStreakUseCase,
    private val restoreEnergyUseCase: RestoreEnergyUseCase,
    private val analyticsTracker: FirebaseAnalyticsTracker
) {
    suspend operator fun invoke(uid: String) = withContext(Dispatchers.IO) {
        // 1. One-shot Migration execution
        executeFirestoreMigrationsUseCase(uid)
        
        // 2. Local Calculations
        val freezersUsed = resetStreakUseCase()
        if (freezersUsed > 0) analyticsTracker.streakFreezerUsed(freezersUsed)
        
        restoreEnergyUseCase()
    }
}
