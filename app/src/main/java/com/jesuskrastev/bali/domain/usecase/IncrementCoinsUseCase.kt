package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.model.ShopInventoryItem
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.random.Random

/**
 * Awards coins for finishing a test/exam/review session.
 *
 * The credit is applied as one atomic operation in [UserRepository.incrementCoins]
 * (a server-side increment, not a local read-then-write), so it's safe even if two
 * results get credited around the same time. An owned double-coins boost doubles the reward and
 * is spent by it (see [spendBoostIfOwned]).
 */
open class IncrementCoinsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Grants a random coin reward sized by [accuracy].
     *
     * @param accuracy the test/exam score percentage that determines the reward tier.
     * @return the number of coins granted, doubled when a boost was spent.
     */
    suspend operator fun invoke(accuracy: Int = 50): Int {
        val baseCoins = if (accuracy >= 50) {
            Random.nextInt(8, 11)   // 8, 9 o 10 monedas
        } else {
            Random.nextInt(3, 6)    // 3, 4 o 5 monedas
        }
        val boostsOwned = userRepository.get().first()?.doubleCoinBoosts ?: 0
        val coinsGained = if (userRepository.spendBoostIfOwned(ShopInventoryItem.DOUBLE_COINS, boostsOwned)) {
            baseCoins * 2
        } else {
            baseCoins
        }
        userRepository.incrementCoins(coinsGained)
        return coinsGained
    }
}
