package com.jesuskrastev.bali.data.migration.migrations

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Migrates user profiles to v11 by adding the coin-shop inventory and one-shot reward boosts.
 *
 * The defaults preserve current behaviour: existing users own no aids, boosts or active wager.
 */
class MigrationV10ToV11 @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreMigration {

    override val targetVersion: Int = 11
    override val description: String = "Añadir inventario de la tienda de monedas"

    /**
     * Adds empty inventory fields for [userId] without replacing any existing profile data.
     *
     * @param userId owner of the user document to migrate
     */
    override suspend fun migrate(userId: String) {
        firestore.collection("env")
            .document(BuildConfig.BUILD_TYPE)
            .collection("users")
            .document(userId)
            .set(
                mapOf(
                    "hints" to 0,
                    "fiftyFifties" to 0,
                    "doubleXpBoosts" to 0,
                    "doubleCoinBoosts" to 0,
                    "activeStreakBet" to false
                ),
                SetOptions.merge()
            )
            .await()
    }
}
