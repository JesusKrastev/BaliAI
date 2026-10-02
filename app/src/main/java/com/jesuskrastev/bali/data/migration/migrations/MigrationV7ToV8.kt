package com.jesuskrastev.bali.data.migration.migrations

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import com.jesuskrastev.bali.domain.model.DailyStreak
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Migración v7 → v8: la racha pasa de semanal a diaria.
 *
 * Cambios en `users/{userId}`:
 * - `currentStreak` pasa a contar días seguidos. Las semanas no se pueden convertir en días, así
 *   que se reconstruye con [DailyStreak.fromHistory] a partir de `practiceDays` (que la racha
 *   semanal solo guardaba de la semana en curso, así que como mucho da 7).
 * - `highestStreak` empieza de nuevo en el valor reconstruido: un récord en semanas no dice
 *   nada en días.
 * - Añade `frozenDays` (días que salvó un congelador), vacío.
 * - Borra `weekSessions` y `currentWeekStart`, que solo usaba la racha semanal.
 *
 * Los congeladores (`streakFreezes`) se mantienen tal cual.
 */
class MigrationV7ToV8 @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreMigration {

    override val targetVersion: Int = 8
    override val description: String = "Pasar la racha de semanal a diaria"

    /**
     * Rewrites the streak fields of one user.
     *
     * @param userId the user whose document is migrated
     */
    override suspend fun migrate(userId: String) {
        val userRef = firestore.collection("env")
            .document(BuildConfig.BUILD_TYPE)
            .collection("users")
            .document(userId)

        val practiceDays = (userRef.get().await().get("practiceDays") as? List<*>)
            .orEmpty()
            .mapNotNull { (it as? Number)?.toLong() }
        val streak = DailyStreak.fromHistory(practiceDays, System.currentTimeMillis())

        userRef.update(
            mapOf(
                "currentStreak" to streak,
                "highestStreak" to streak,
                "frozenDays" to emptyList<Long>(),
                "weekSessions" to FieldValue.delete(),
                "currentWeekStart" to FieldValue.delete()
            )
        ).await()
    }
}
