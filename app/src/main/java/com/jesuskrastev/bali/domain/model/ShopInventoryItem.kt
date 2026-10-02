package com.jesuskrastev.bali.domain.model

/**
 * Consumable items that can be bought in the coin shop and kept in a user's inventory.
 *
 * Each value maps to one persisted counter. The streak bet is not here: it is a single
 * target number, see [StreakBet].
 */
enum class ShopInventoryItem {
    HINT,
    FIFTY_FIFTY,
    DOUBLE_XP,
    DOUBLE_COINS
}
