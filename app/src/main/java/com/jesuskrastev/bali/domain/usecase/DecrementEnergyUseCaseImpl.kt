package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Premium behavior (the only tier after introducing the hard paywall):
 * energy is unlimited, so this is a no-op that just returns the current value.
 */
class DecrementEnergyUseCaseImpl @Inject constructor(
    private val userRepository: UserRepository,
) : DecrementEnergyUseCase {
    override suspend fun invoke(): Int {
        return userRepository.get().first()?.energy ?: -1
    }
}
