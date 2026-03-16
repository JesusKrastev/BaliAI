package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class RestoreEnergyUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository
) : RestoreEnergyUseCase {

    companion object {
        private const val MAX_ENERGY = 5
        private const val REGENERATION_INTERVAL_MILLIS = 2 * 60 * 60 * 1000L
    }

    override suspend fun invoke() {
        val user = userRepository.get().first() ?: return
        val currentTime = System.currentTimeMillis()
        if (user.energy >= MAX_ENERGY) {
            userRepository.updateEnergyAndTimestamp(user.energy, currentTime)
            return
        }
        if (user.lastEnergyUpdateTimestamp == 0L) {
            userRepository.updateEnergyAndTimestamp(user.energy, currentTime)
            return
        }
        val timePassed = currentTime - user.lastEnergyUpdateTimestamp
        if (timePassed >= REGENERATION_INTERVAL_MILLIS) {
            val energyToAdd = (timePassed / REGENERATION_INTERVAL_MILLIS).toInt()
            val newEnergy = minOf(MAX_ENERGY, user.energy + energyToAdd)
            val timeRemainder = timePassed % REGENERATION_INTERVAL_MILLIS
            val newTimestamp = if (newEnergy == MAX_ENERGY) currentTime
            else currentTime - timeRemainder
            userRepository.updateEnergyAndTimestamp(newEnergy, newTimestamp)
        }
    }
}
