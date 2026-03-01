package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.migration.FirestoreMigrationManager
import javax.inject.Inject

/**
 * Use case para ejecutar las migraciones de Firestore al iniciar la app.
 * Puede fallar silenciosamente (log) o ser más estricto según la configuración.
 */
class ExecuteFirestoreMigrationsUseCase @Inject constructor(
    private val migrationManager: FirestoreMigrationManager
) {
    suspend operator fun invoke(userId: String) {
        try {
            migrationManager.executePendingMigrations(userId)
        } catch (e: Exception) {
            // Log la excepción pero no crasha la app
            // Lanzamos la excepción para el ViewModel maneje el error
            println("Error durante migraciones de Firestore: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
