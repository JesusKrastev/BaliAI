package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.model.ShopInventoryItem
import javax.inject.Inject
import kotlin.random.Random

/**
 * Awards coins for finishing a test/exam/review session.
 *
 * The credit is applied as one atomic operation in [UserRepository.incrementCoins]
 * (a server-side increment, not a local read-then-write), so it's safe even if two
 * results get credited around the same time.
 */
open class IncrementCoinsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Grants a random coin reward sized by [accuracy].
     *
     * @param accuracy the test/exam score percentage that determines the reward tier.
     * @return the number of coins granted.
     */
    suspend operator fun invoke(accuracy: Int = 50): Int {
        val baseCoins = if (accuracy >= 50) {
            Random.nextInt(8, 11)   // 8, 9 o 10 monedas
        } else {
            Random.nextInt(3, 6)    // 3, 4 o 5 monedas
        }
        val coinsGained = if (userRepository.consumeInventoryItem(ShopInventoryItem.DOUBLE_COINS)) {
            baseCoins * 2
        } else {
            baseCoins
        }
        userRepository.incrementCoins(coinsGained)
        return coinsGained
    }
}
