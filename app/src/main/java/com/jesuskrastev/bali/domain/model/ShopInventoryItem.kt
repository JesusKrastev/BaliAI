package com.jesuskrastev.bali.domain.model

/**
 * Consumable items that can be bought in the coin shop and kept in a user's inventory.
 *
 * Each value maps to one persisted counter, except [STREAK_BET], which is limited to one
 * active wager at a time.
 */
enum class ShopInventoryItem {
    HINT,
    FIFTY_FIFTY,
    DOUBLE_XP,
    DOUBLE_COINS,
    STREAK_BET
}
