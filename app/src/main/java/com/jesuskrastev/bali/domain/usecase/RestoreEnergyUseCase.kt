package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

open class RestoreEnergyUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    companion object {
        private const val MAX_ENERGY = 5
        private const val REGENERATION_INTERVAL_MILLIS = 2 * 60 * 60 * 1000L // 2 hours
    }

    suspend operator fun invoke() {
        val user = userRepository.get().first() ?: return

        val currentTime = System.currentTimeMillis()

        // If energy is already full, ensure the timestamp is synced so we don't start accumulating
        if (user.energy >= MAX_ENERGY) {
            userRepository.updateEnergyAndTimestamp(user.energy, currentTime)
            return
        }

        // Initialize tracking if user doesn't have it set but has energy < MAX_ENERGY
        if (user.lastEnergyUpdateTimestamp == 0L) {
            userRepository.updateEnergyAndTimestamp(user.energy, currentTime)
            return
        }

        val timePassed = currentTime - user.lastEnergyUpdateTimestamp

        if (timePassed >= REGENERATION_INTERVAL_MILLIS) {
            val energyToAdd = (timePassed / REGENERATION_INTERVAL_MILLIS).toInt()
            val newEnergy = minOf(MAX_ENERGY, user.energy + energyToAdd)
            
            val timeRemainder = timePassed % REGENERATION_INTERVAL_MILLIS
            val newTimestamp = if (newEnergy == MAX_ENERGY) {
                currentTime // Stop timer if we reached max
            } else {
                currentTime - timeRemainder // Keep the remainder for exactly spaced intervals
            }
            
            userRepository.updateEnergyAndTimestamp(newEnergy, newTimestamp)
        }
    }
}