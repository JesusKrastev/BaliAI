package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class RestoreEnergyUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository
) : RestoreEnergyUseCase {

    companion object {
        private const val MAX_ENERGY = 5
    }

    override suspend fun invoke() {
        // Premium: always ensure energy is full, no timer needed
        val user = userRepository.get().first() ?: return
        if (user.energy < MAX_ENERGY) {
            userRepository.updateEnergy(MAX_ENERGY)
        }
    }
}
