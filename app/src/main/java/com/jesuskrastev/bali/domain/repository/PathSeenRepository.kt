package com.jesuskrastev.bali.domain.repository

/**
 * Remembers, on this device, how far along the learning path Home last showed the user, so Home
 * can tell a node that was unlocked since then from one that was always open. Purely cosmetic
 * (it only decides whether to play an animation), which is why it lives in DataStore and not in
 * the user profile: no Room or Firestore migration, and a new install simply starts silent.
 */
interface PathSeenRepository {

    /**
     * The furthest unlocked node Home showed this user.
     *
     * @param userId the signed-in user; each account has its own memory
     * @return that node's `orderIndex`, or null when Home has never shown this user's path
     */
    suspend fun lastSeenFrontier(userId: String): Int?

    /**
     * Records the furthest unlocked node Home has now shown.
     *
     * @param userId the signed-in user
     * @param frontierOrder `orderIndex` of the furthest unlocked node
     */
    suspend fun markSeen(userId: String, frontierOrder: Int)
}
