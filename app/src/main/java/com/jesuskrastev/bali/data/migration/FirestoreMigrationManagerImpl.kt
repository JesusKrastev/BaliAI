package com.jesuskrastev.bali.data.migration

import android.util.Log
import com.jesuskrastev.bali.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import com.jesuskrastev.bali.domain.migration.FirestoreMigrationManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirestoreMigration"

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
@Singleton
class FirestoreMigrationManagerImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val migrations: Set<@JvmSuppressWildcards FirestoreMigration>
) : FirestoreMigrationManager {

    private val migrationMutex = Mutex()

    private val collection =
        firestore.collection("env").document(BuildConfig.BUILD_TYPE).collection("users")

    /**
     * Runs every migration between the user's current schema version and [getTargetSchemaVersion],
     * in order, persisting the new version after each one so a failure partway through resumes
     * from the last completed step rather than re-running it.
     *
     * @param userId the Firestore user to migrate.
     * @throws MigrationException if a migration or the version write that follows it fails.
     */
    override suspend fun executePendingMigrations(userId: String) {
        migrationMutex.withLock {
            val currentVersion = getCurrentSchemaVersion(userId)
            val targetVersion = getTargetSchemaVersion()

            if (currentVersion >= targetVersion) {
                Log.d(TAG, "Base de datos ya está en la versión $currentVersion. No hay migraciones que ejecutar.")
                return
            }

            Log.d(TAG, "Migraciones necesarias: v$currentVersion → v$targetVersion")

            // Obtener las migraciones necesarias en orden
            val pendingMigrations = migrations
                .filter { it.targetVersion > currentVersion && it.targetVersion <= targetVersion }
                .sortedBy { it.targetVersion }

            if (pendingMigrations.isEmpty()) {
                Log.d(TAG, "No se encontraron migraciones para aplicar.")
                return
            }

            // Ejecutar cada migración en orden
            for (migration in pendingMigrations) {
                try {
                    Log.d(TAG, "Ejecutando migración: ${migration.description} (v${migration.targetVersion})")
                    migration.migrate(userId)

                    // Actualizar la versión de esquema
                    updateSchemaVersion(userId, migration.targetVersion)
                    Log.d(TAG, "Migración completada: ${migration.description}")
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(TAG, "Error en migración ${migration.description}: ${e.message}", e)
                    FirebaseCrashlytics.getInstance().recordException(e)
                    throw MigrationException(
                        "Falló la migración a v${migration.targetVersion}: ${e.message}",
                        e
                    )
                }
            }

            Log.d(TAG, "Todas las migraciones completadas. Versión final: $targetVersion")
        }
    }

    /**
     * Reads the user's current schema version from their Firestore document.
     *
     * @param userId the Firestore user to check.
     * @return the stored version, 1 for a user with no document yet, or 1 (reported to
     *   Crashlytics) if the read itself fails.
     */
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
            if (e is CancellationException) throw e
            Log.e(TAG, "Error obteniendo versión de esquema: ${e.message}", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            1  // Default a v1 en caso de error
        }
    }

    /** @return the highest [FirestoreMigration.targetVersion] registered, or 1 when none are. */
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
            if (e is CancellationException) throw e
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
