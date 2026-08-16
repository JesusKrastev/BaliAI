package com.jesuskrastev.bali.data.migration.migrations

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Migración v5 → v6
 *
 * Cambios:
 * - Añade la subcolección `chat_messages` bajo `users/{userId}`, donde se guarda la
 *   conversación del alumno con el tutor de IA.
 *
 * No hay datos que rellenar: la subcolección la crea Firestore al escribir el primer
 * mensaje, y un usuario que nunca abra el chat simplemente no la tendrá. La migración
 * existe para que `schemaVersion` refleje la estructura real del documento y para dejar
 * registrado el cambio, tal y como exige el flujo de migraciones del proyecto.
 *
 * Sí limpia cualquier resto de una sesión anterior: si el documento de usuario tenía un
 * campo `chatMessages` embebido de una versión de prueba, se elimina para que la única
 * fuente de verdad sea la subcolección.
 */
class MigrationV5ToV6 @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreMigration {

    override val targetVersion: Int = 6
    override val description: String =
        "Registrar la subcolección chat_messages (conversación con el tutor de IA)"

    override suspend fun migrate(userId: String) {
        val userRef = firestore.collection("env")
            .document(BuildConfig.BUILD_TYPE)
            .collection("users")
            .document(userId)

        val snapshot = userRef.get().await()
        if (snapshot.contains("chatMessages")) {
            userRef.update("chatMessages", FieldValue.delete()).await()
        }
    }
}
