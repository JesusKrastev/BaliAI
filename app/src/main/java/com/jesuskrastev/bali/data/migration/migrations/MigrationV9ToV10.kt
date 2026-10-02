package com.jesuskrastev.bali.data.migration.migrations

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Migración v9 → v10: la racha perdida se puede recuperar con monedas (D-021).
 *
 * Cambios en `users/{userId}`:
 * - Añade `lostStreak` y `lostStreakDayMillis`, ambos a 0: nadie tiene una racha recuperable
 *   todavía.
 * - Borra `lastStreakSettledDayMillis`, que solo escribió la build de internal con el
 *   velocímetro descartado.
 *
 * La racha que esa build recortó a 7 no se reconstruye: el dato por encima de 7 se perdió.
 */
class MigrationV9ToV10 @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreMigration {

    override val targetVersion: Int = 10
    override val description: String = "Añadir la racha perdida que se puede recuperar"

    /**
     * Writes the new streak-recovery fields of one user.
     *
     * @param userId the user whose document is migrated
     */
    override suspend fun migrate(userId: String) {
        val userRef = firestore.collection("env")
            .document(BuildConfig.BUILD_TYPE)
            .collection("users")
            .document(userId)

        userRef.update(
            mapOf(
                "lostStreak" to 0,
                "lostStreakDayMillis" to 0L,
                "lastStreakSettledDayMillis" to FieldValue.delete()
            )
        ).await()
    }
}
