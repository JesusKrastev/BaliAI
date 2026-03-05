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
 * - Reinicia a 0 el xp del usuario
 * - Reinicia a 0 el level del usuario
 * - Elimina la subcolección test_results
 * - Elimina la subcolección answers
 */
class MigrationV2ToV3 @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreMigration {

    override val targetVersion: Int = 3
    override val description: String = "Reiniciar xp/level y eliminar test_results/answers"

    override suspend fun migrate(userId: String) {
        val userRef = firestore.collection("env")
            .document(BuildConfig.BUILD_TYPE)
            .collection("users")
            .document(userId)

        // 1. Reset xp and level
        val updates = hashMapOf<String, Any>(
            "xp" to 0,
            "level" to 0
        )
        userRef.update(updates).await()
        println("Migration V3: Reset 'xp' and 'level' to 0 for user $userId")

        // 2. Delete test_results subcollection
        val testResultsRef = userRef.collection("test_results")
        val testResultsSnapshot = testResultsRef.get().await()
        
        if (!testResultsSnapshot.isEmpty) {
            val batch = firestore.batch()
            for (doc in testResultsSnapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            println("Migration V3: Deleted ${testResultsSnapshot.size()} documents from test_results subcollection for user $userId")
        }

        // 3. Delete answers subcollection
        val answersRef = userRef.collection("answers")
        val answersSnapshot = answersRef.get().await()
        
        if (!answersSnapshot.isEmpty) {
            val answersBatch = firestore.batch()
            for (doc in answersSnapshot.documents) {
                answersBatch.delete(doc.reference)
            }
            answersBatch.commit().await()
            println("Migration V3: Deleted ${answersSnapshot.size()} documents from answers subcollection for user $userId")
        }
    }
}
