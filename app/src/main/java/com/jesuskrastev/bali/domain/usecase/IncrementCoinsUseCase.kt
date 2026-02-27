package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.data.repository.UserRepositoryImpl
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.random.Random

class IncrementCoinsUseCase @Inject constructor(
    private val userRepository: UserRepositoryImpl
) {
    suspend operator fun invoke(): Int {
        val user = userRepository.get().first() ?: return 0
        val coinsToGained = Random.nextInt(8, 11) // Random between 8 and 10
        val newTotalCoins = user.coins + coinsToGained
        userRepository.updateCoins(newTotalCoins)
        return coinsToGained
    }
}
