package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.data.repository.UserRepositoryImpl
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DecrementEnergyUseCase @Inject constructor(
    private val userRepository: UserRepositoryImpl,
) {
    suspend operator fun invoke(): Int {
        val user = userRepository.get().first() ?: return -1
        if (user.energy > 0) {
            val newEnergy = user.energy - 1
            if (user.energy >= 5) {
                userRepository.updateEnergyAndTimestamp(newEnergy, System.currentTimeMillis())
            } else {
                userRepository.updateEnergy(newEnergy)
            }
            return newEnergy
        }
        return user.energy
    }
}
