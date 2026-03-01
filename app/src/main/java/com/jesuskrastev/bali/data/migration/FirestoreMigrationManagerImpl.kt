package com.jesuskrastev.bali.data.migration

import com.jesuskrastev.bali.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import com.jesuskrastev.bali.domain.migration.FirestoreMigrationManager
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Implementación del manager de migraciones de Firestore.
 * Gestiona la ejecución secuencial de migraciones y la actualización de versiones.
 *
 * Estructura en Firestore:
 * env/
 *   debug/
 *     schemaVersion: 1 (actualizable según sea necesario)
 *   release/
 *     schemaVersion: 1 (actualizable según sea necesario)
 */
class FirestoreMigrationManagerImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val migrations: Set<@JvmSuppressWildcards FirestoreMigration>
) : FirestoreMigrationManager {

    private val collection =
        firestore.collection("env").document(BuildConfig.BUILD_TYPE).collection("users")

    override suspend fun executePendingMigrations(userId: String) {
        val currentVersion = getCurrentSchemaVersion(userId)
        val targetVersion = getTargetSchemaVersion()

        if (currentVersion >= targetVersion) {
            println("Base de datos ya está en la versión $currentVersion. No hay migraciones que ejecutar.")
            return
        }

        println("Migraciones necesarias: v$currentVersion → v$targetVersion")

        // Obtener las migraciones necesarias en orden
        val pendingMigrations = migrations
            .filter { it.targetVersion > currentVersion && it.targetVersion <= targetVersion }
            .sortedBy { it.targetVersion }

        if (pendingMigrations.isEmpty()) {
            println("No se encontraron migraciones para aplicar.")
            return
        }

        // Ejecutar cada migración en orden
        for (migration in pendingMigrations) {
            try {
                println("Ejecutando migración: ${migration.description} (v${migration.targetVersion})")
                migration.migrate(userId)

                // Actualizar la versión de esquema
                updateSchemaVersion(userId, migration.targetVersion)
                println("Migración completada: ${migration.description}")

            } catch (e: Exception) {
                println("Error en migración ${migration.description}: ${e.message}")
                throw MigrationException(
                    "Falló la migración a v${migration.targetVersion}: ${e.message}",
                    e
                )
            }
        }

        println("Todas las migraciones completadas. Versión final: $targetVersion")
    }

    override suspend fun getCurrentSchemaVersion(userId: String): Int {
        return try {
            val userDocRef = collection.document(userId)
            val userDoc = userDocRef.get().await()
            
            if (userDoc.exists()) {
                userDoc.getLong("schemaVersion")?.toInt() ?: 1
            } else {
                1 // Usuario nuevo o sin datos aún
            }
        } catch (e: Exception) {
            println("Error obteniendo versión de esquema: ${e.message}")
            1  // Default a v1 en caso de error
        }
    }

    override fun getTargetSchemaVersion(): Int {
        // La versión objetivo de la app es siempre la versión de migración más alta que tenemos
        return migrations.maxOfOrNull { it.targetVersion } ?: 1
    }

    /**
     * Actualiza el campo schemaVersion en el documento del usuario (users/{userId})
     */
    private suspend fun updateSchemaVersion(userId: String, version: Int) {
        try {
            val userDocRef = collection.document(userId)
            // Usamos merge(true) o update para no sobreescribir el document entero si falta algo en la app vieja, aunque Update es mas seguro si el doc ya existe
            userDocRef.update("schemaVersion", version).await()
        } catch (e: Exception) {
            throw MigrationException(
                "No se pudo actualizar schemaVersion a $version en el usuario $userId: ${e.message}",
                e
            )
        }
    }
}

/**
 * Excepciones personalizadas para migraciones
 */
class MigrationException(message: String, cause: Throwable? = null) : Exception(message, cause)
