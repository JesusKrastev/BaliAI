package com.jesuskrastev.bali.data.migration.migrations

import com.google.firebase.firestore.FirebaseFirestore
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Migración v2 → v3
 *
 * Cambios:
 * - Reinicia a 0 el xp, el level y la racha del usuario
 * - Elimina la subcolección test_results
 * - Elimina la subcolección answers
 */
class MigrationV2ToV3 @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreMigration {

    override val targetVersion: Int = 3
    override val description: String = "Reiniciar xp/level y eliminar test_results/answers"

    /**
     * Resets the progress fields and clears both result subcollections of one user.
     *
     * @param userId the user whose document is migrated
     */
    override suspend fun migrate(userId: String) {
        val userRef = firestore.collection("env")
            .document(BuildConfig.BUILD_TYPE)
            .collection("users")
            .document(userId)

        userRef.update(mapOf("xp" to 0, "level" to 0, "currentStreak" to 0)).await()
        userRef.collection("test_results").deleteAllDocuments(firestore)
        userRef.collection("answers").deleteAllDocuments(firestore)
    }
}
