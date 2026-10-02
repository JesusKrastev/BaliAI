package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.model.ShopInventoryItem
import com.jesuskrastev.bali.domain.repository.UserRepository
import kotlinx.coroutines.CancellationException

/**
 * Spends one reward boost for the activity that just finished, without ever blocking its rewards.
 *
 * The repository is only asked when the profile says a boost is owned: spending one is a
 * transaction that needs a connection for a signed-in user, and finishing a test offline must
 * keep giving its normal XP and coins. If the spend fails the activity simply goes unboosted.
 *
 * @param item the boost to spend ([ShopInventoryItem.DOUBLE_XP] or [ShopInventoryItem.DOUBLE_COINS])
 * @param owned how many of that boost the profile holds
 * @return true when a boost was spent and the reward should be doubled
 */
internal suspend fun UserRepository.spendBoostIfOwned(item: ShopInventoryItem, owned: Int): Boolean {
    if (owned <= 0) return false
    return try {
        consumeInventoryItem(item)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        false
    }
}
