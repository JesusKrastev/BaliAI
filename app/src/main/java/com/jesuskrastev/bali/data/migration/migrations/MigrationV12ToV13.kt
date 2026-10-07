package com.jesuskrastev.bali.data.migration.migrations

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Migrates user profiles to v13 by adding the coin-shop inventory, one-shot reward boosts and the
 * streak bet target.
 *
 * The defaults preserve current behaviour: existing users own no aids or boosts and have no active bet.
 * The number is 13 because v11 (answers) is already in `develop` and v12 is claimed by the
 * first-steps feature; it is final only once this branch merges (see the branches rule in CLAUDE.md).
 */
class MigrationV12ToV13 @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreMigration {

    override val targetVersion: Int = 13
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
                    "streakBetTarget" to 0,
                    "activeStreakBet" to false
                ),
                SetOptions.merge()
            )
            .await()
    }
}
