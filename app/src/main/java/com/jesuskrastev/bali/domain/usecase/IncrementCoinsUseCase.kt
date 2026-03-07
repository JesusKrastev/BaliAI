package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.random.Random

open class IncrementCoinsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(accuracy: Int = 50): Int {
        val user = userRepository.get().first() ?: return 0
        val coinsToGained = if (accuracy >= 50) {
            Random.nextInt(8, 11)   // 8, 9 o 10 monedas
        } else {
            Random.nextInt(3, 6)    // 3, 4 o 5 monedas
        }
        val newTotalCoins = user.coins + coinsToGained
        userRepository.updateCoins(newTotalCoins)
        return coinsToGained
    }
}
