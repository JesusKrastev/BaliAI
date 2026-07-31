package com.jesuskrastev.bali.data.migration.migrations

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Migración v3 → v4
 *
 * Cambios:
 * - Elimina los campos `energy` y `lastEnergyUpdateTimestamp` del documento de usuario,
 *   ya que la funcionalidad de energía se ha retirado de la app.
 */
class MigrationV3ToV4 @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreMigration {

    override val targetVersion: Int = 4
    override val description: String = "Eliminar campos energy/lastEnergyUpdateTimestamp del usuario"

    override suspend fun migrate(userId: String) {
        val userRef = firestore.collection("env")
            .document(BuildConfig.BUILD_TYPE)
            .collection("users")
            .document(userId)

        val updates = hashMapOf<String, Any>(
            "energy" to FieldValue.delete(),
            "lastEnergyUpdateTimestamp" to FieldValue.delete()
        )
        userRef.update(updates).await()
        println("Migration V4: Removed 'energy' and 'lastEnergyUpdateTimestamp' fields for user $userId")
    }
}
