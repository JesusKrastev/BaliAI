package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DecrementEnergyUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository,
) : DecrementEnergyUseCase {
    override suspend fun invoke(): Int {
        // Premium: energy is never consumed
        return userRepository.get().first()?.energy ?: -1
    }
}
