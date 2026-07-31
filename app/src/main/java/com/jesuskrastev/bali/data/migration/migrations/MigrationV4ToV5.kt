package com.jesuskrastev.bali.data.migration.migrations

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Migración v4 → v5
 *
 * Cambios:
 * - Elimina los campos `reasons`, `dailyGoal`, `learningPreference`, `concern` y `studyTime`
 *   del documento de usuario. Son respuestas de onboarding que nunca se leen tras completar
 *   el onboarding, así que dejan de persistirse.
 */
class MigrationV4ToV5 @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreMigration {

    override val targetVersion: Int = 5
    override val description: String = "Eliminar campos de onboarding no usados (reasons, dailyGoal, learningPreference, concern, studyTime)"

    override suspend fun migrate(userId: String) {
        val userRef = firestore.collection("env")
            .document(BuildConfig.BUILD_TYPE)
            .collection("users")
            .document(userId)

        val updates = hashMapOf<String, Any>(
            "reasons" to FieldValue.delete(),
            "dailyGoal" to FieldValue.delete(),
            "learningPreference" to FieldValue.delete(),
            "concern" to FieldValue.delete(),
            "studyTime" to FieldValue.delete()
        )
        userRef.update(updates).await()
        println("Migration V5: Removed unused onboarding fields for user $userId")
    }
}
