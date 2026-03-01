package com.jesuskrastev.bali.data.migration.migrations

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Migración AVANZADA de ejemplo: Transformación compleja de datos
 *
 * Cambios:
 * - Convierte un campo string en un Map anidado
 * - Agrega validación y transformación de datos
 * - Combina información de múltiples documentos
 *
 * IMPORTANTE: Este es solo un ejemplo educativo.
 * Descomenta el @Provides en FirestoreMigrationsModule cuando lo necesites.
 *
 * Casos de uso:
 * - Conversión de tipos de datos
 * - Normalización de datos
 * - Consolidación de campos
 * - Limpieza o validación de datos
 */
class MigrationAdvancedExample @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreMigration {

    override val targetVersion: Int = 99  // Nombre ficticio para que no se ejecute
    override val description: String = "EJEMPLO AVANZADO - No ejecutar"

    override suspend fun migrate(userId: String) {
        // Este método contiene EJEMPLOS de patrones avanzados
        // Adapta según tus necesidades reales
    }

    // ========== PATRÓN 1: Dividir un campo string en componentes ==========
    private suspend fun splitStringField() {
        val usersRef = firestore.collection("users")
        val snapshot = usersRef.get().await()
        val batch = firestore.batch()

        for (document in snapshot.documents) {
            // Convertir: "John Doe" → firstName: "John", lastName: "Doe"
            val fullName = document.getString("fullName") ?: continue

            val parts = fullName.split(Regex("\\s+"), limit = 2)
            val firstName = parts.getOrNull(0) ?: ""
            val lastName = parts.getOrNull(1) ?: ""

            batch.update(
                document.reference,
                mapOf(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "fullName" to FieldValue.delete()  // Eliminar el campo viejo
                )
            )
        }

        if (snapshot.documents.isNotEmpty()) {
            batch.commit().await()
        }
    }

    // ========== PATRÓN 2: Agregar campos basados en lógica condicional ==========
    private suspend fun addConditionalFields() {
        val usersRef = firestore.collection("users")
        val snapshot = usersRef.get().await()
        val batch = firestore.batch()

        for (document in snapshot.documents) {
            val updates = mutableMapOf<String, Any?>()

            // Asignar badge según XP
            val xp = document.getLong("xp") ?: 0
            val badge = when {
                xp >= 1000 -> "legend"
                xp >= 500 -> "expert"
                xp >= 100 -> "master"
                else -> "starter"
            }
            updates["badge"] = badge

            // Premium si ha pagado
            val hasSubscription = document.getBoolean("hasSubscription") ?: false
            updates["isPremium"] = hasSubscription

            if (updates.isNotEmpty()) {
                batch.update(document.reference, updates)
            }
        }

        if (snapshot.documents.isNotEmpty()) {
            batch.commit().await()
        }
    }

    // ========== PATRÓN 3: Consolidar sub-colecciones ==========
    private suspend fun consolidateSubcollections() {
        val usersRef = firestore.collection("users")
        val userDocs = usersRef.get().await()

        val batch = firestore.batch()

        for (userDoc in userDocs.documents) {
            // Obtener datos de una sub-colección
            val achievementsRef = userDoc.reference.collection("achievements")
            val achievements = achievementsRef.get().await()

            val achievementIds = mutableListOf<String>()
            var totalAchievements = 0L

            for (achievement in achievements.documents) {
                achievementIds.add(achievement.id)
                totalAchievements++
            }

            // Guardar el consolidado en el documento principal
            batch.update(
                userDoc.reference,
                mapOf(
                    "achievementIds" to achievementIds,
                    "totalAchievements" to totalAchievements
                )
            )
        }

        if (userDocs.documents.isNotEmpty()) {
            batch.commit().await()
        }
    }

    // ========== PATRÓN 4: Validar y limpiar datos ==========
    private suspend fun validateAndCleanData() {
        val usersRef = firestore.collection("users")
        val snapshot = usersRef.get().await()
        val batch = firestore.batch()

        for (document in snapshot.documents) {
            val updates = mutableMapOf<String, Any?>()

            // Limpiar email (conversión a minúsculas, validación)
            val email = document.getString("email")
            if (!email.isNullOrBlank()) {
                val cleanEmail = email.trim().lowercase()
                if (cleanEmail.contains("@")) {
                    updates["email"] = cleanEmail
                }
            }

            // Asegurar que level es válido
            val level = document.getLong("level") ?: 1
            if (level < 1) {
                updates["level"] = 1L
            } else if (level > 100) {
                updates["level"] = 100L
            }

            // Eliminar campos nulos o inválidos
            if (document.get("deletedAt") == null && document.getBoolean("deleted") == true) {
                updates["deleted"] = FieldValue.delete()
            }

            if (updates.isNotEmpty()) {
                batch.update(document.reference, updates)
            }
        }

        if (snapshot.documents.isNotEmpty()) {
            batch.commit().await()
        }
    }

    // ========== PATRÓN 5: Migrar entre colecciones ==========
    private suspend fun migrateToNewCollection() {
        // Copiar documentos de "oldCollection" a "newCollection"
        val oldRef = firestore.collection("oldCollection")
        val newRef = firestore.collection("newCollection")

        val oldDocs = oldRef.get().await()
        val batch = firestore.batch()

        for (oldDoc in oldDocs.documents) {
            val data = oldDoc.data?.toMutableMap() ?: mutableMapOf()
            
            // Transformar datos si es necesario
            data["migratedFrom"] = "oldCollection"
            data["migratedAt"] = FieldValue.serverTimestamp()

            // Crear documento en nueva colección
            batch.set(newRef.document(oldDoc.id), data)
        }

        // Opcionalmente: eliminar la vieja colección
        // for (oldDoc in oldDocs.documents) {
        //     batch.delete(oldDoc.reference)
        // }

        if (oldDocs.documents.isNotEmpty()) {
            batch.commit().await()
        }
    }

    // ========== PATRÓN 6: Agregar datos embebidos (nested objects) ==========
    private suspend fun addNestedStructures() {
        val usersRef = firestore.collection("users")
        val snapshot = usersRef.get().await()
        val batch = firestore.batch()

        for (document in snapshot.documents) {
            if (document.get("stats") == null) {
                // Crear estructura compleja
                batch.update(
                    document.reference,
                    "stats",
                    mapOf(
                        "level" to 1L,
                        "xp" to 0L,
                        "streak" to 0L,
                        "achievements" to mapOf(
                            "total" to 0L,
                            "ids" to emptyList<String>()
                        ),
                        "achievements.completed" to false
                    )
                )
            }
        }

        if (snapshot.documents.isNotEmpty()) {
            batch.commit().await()
        }
    }

    // ========== PATRÓN 7: Usar transacciones para consistencia ==========
    private suspend fun transactionExample() {
        // Para operaciones muy complejas que necesitan garantías ACID
        firestore.runTransaction { transaction ->
            val userRef = firestore.collection("users").document("exampleUser")
            val snapshot = transaction.get(userRef)

            if (snapshot.exists()) {
                val updates = mapOf("migrationVersion" to 99L)
                transaction.update(userRef, updates)
            }
        }.await()
    }
}

/**
 * NOTAS IMPORTANTES:
 *
 * 1. BATCH SIZE: Firestore permite máximo 500 operaciones por batch
 *    Si tienes más de 500 docs, divide en varios batches:
 *
 *    var batch = firestore.batch()
 *    var count = 0
 *    for (doc in documents) {
 *        batch.update(doc.reference, data)
 *        count++
 *        if (count >= 500) {
 *            batch.commit().await()
 *            batch = firestore.batch()
 *            count = 0
 *        }
 *    }
 *    if (count > 0) batch.commit().await()
 *
 * 2. PERFORMANCE: Para colecciones muy grandes (>10k docs), considera:
 *    - Ejecutar en background si es posible
 *    - Logear progreso
 *    - Usar índices de Firestore si haces queries complejas
 *
 * 3. TESTING: Siempre prueba en un documento antes de migrar toda la colección:
 *    val testDoc = usersRef.document("test-user").get().await()
 *    // Aplica la lógica al testDoc, verifica resultados
 *
 * 4. ROLLBACK: Si algo falla, debes revertir manualmente o crear una migración inversa.
 *    Por eso es importante tener backups antes de migrar datos críticos.
 */
