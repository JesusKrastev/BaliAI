package com.jesuskrastev.bali.data.migration.migrations

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Migración de ejemplo: v1 → v2
 *
 * Cambios:
 * - Agrega un campo 'createdAt' a todos los documentos de usuarios que no lo tengan
 * - Agrega un campo 'updatedAt' a todos los documentos de usuarios
 *
 * Uso:
 * 1. Implementa esta clase siguiendo el patrón
 * 2. Regístrala en un módulo Hilt (ver MigrationV1ToV2Module)
 */
class MigrationV1ToV2 @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreMigration {

    override val targetVersion: Int = 2
    override val description: String = "Eliminar campo id suelto y borrar subcolecciones test_results y answers"

    override suspend fun migrate(userId: String) {
        val userRef = firestore.collection("env")
            .document(BuildConfig.BUILD_TYPE)
            .collection("users")
            .document(userId)

        // 1. Delete 'id' field from user document
        val updates = hashMapOf<String, Any>(
            "id" to FieldValue.delete()
        )
        userRef.update(updates).await()
        println("Migration V2: Deleted 'id' field from user $userId")

        // 2. Delete test_results subcollection
        val testResultsRef = userRef.collection("test_results")
        val testResultsSnapshot = testResultsRef.get().await()
        
        if (!testResultsSnapshot.isEmpty) {
            val batch = firestore.batch()
            for (doc in testResultsSnapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            println("Migration V2: Deleted ${testResultsSnapshot.size()} documents from test_results subcollection for user $userId")
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
            println("Migration V2: Deleted ${answersSnapshot.size()} documents from answers subcollection for user $userId")
        }
    }
}
