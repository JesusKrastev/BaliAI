package com.jesuskrastev.bali.data.migration.migrations

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Migración v1 → v2
 *
 * Cambios:
 * - Elimina el campo `id` suelto del documento de usuario (el id es el del documento).
 * - Borra las subcolecciones `test_results` y `answers`, que cambiaron de estructura.
 */
class MigrationV1ToV2 @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreMigration {

    override val targetVersion: Int = 2
    override val description: String = "Eliminar campo id suelto y borrar subcolecciones test_results y answers"

    /**
     * Removes the stray `id` field and clears both result subcollections of one user.
     *
     * @param userId the user whose document is migrated
     */
    override suspend fun migrate(userId: String) {
        val userRef = firestore.collection("env")
            .document(BuildConfig.BUILD_TYPE)
            .collection("users")
            .document(userId)

        userRef.update(mapOf("id" to FieldValue.delete())).await()
        userRef.collection("test_results").deleteAllDocuments(firestore)
        userRef.collection("answers").deleteAllDocuments(firestore)
    }
}
