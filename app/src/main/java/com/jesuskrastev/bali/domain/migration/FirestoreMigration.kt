package com.jesuskrastev.bali.domain.migration

/**
 * Interfaz base para todas las migraciones de Firestore.
 * Cada migración es responsable de actualizar la estructura de datos de una versión anterior a la actual.
 */
interface FirestoreMigration {
    /**
     * Versión inicial desde la que se aplica esta migración.
     * Por ejemplo, si la migración va de v1 a v2, targetVersion = 2
     */
    val targetVersion: Int

    /**
     * Descripción legible de qué cambios realiza esta migración
     */
    val description: String

    /**
     * Ejecuta la migración para un usuario específico.
     * @param userId El ID del usuario asociado a los datos a migrar.
     * @throws Exception Si algo falla durante la migración
     */
    suspend fun migrate(userId: String)
}
