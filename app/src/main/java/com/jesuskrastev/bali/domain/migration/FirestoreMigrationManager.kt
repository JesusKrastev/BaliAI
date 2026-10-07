package com.jesuskrastev.bali.domain.migration

/**
 * Manager encargado de:
 * - Obtener la versión actual de esquema de Firestore
 * - Ejecutar solo las migraciones necesarias
 * - Actualizar la versión de esquema una vez completadas
 */
interface FirestoreMigrationManager {
    /**
     * Ejecuta todas las migraciones pendientes necesarias para llevar
     * la base de datos a la versión actual.
     *
     * @param userId El ID del usuario en Firestore a migrar
     * @throws Exception Si alguna migración falla, la ejecución se detiene
     */
    suspend fun executePendingMigrations(userId: String)

    /**
     * Obtiene la versión de esquema actual desde Firestore para un usuario específico.
     *
     * @return la versión guardada; 1 si el documento no existe o no tiene el campo
     * @throws Exception si no se puede leer: quien llama no debe suponer ninguna versión, porque
     *   las primeras migraciones borran datos
     */
    suspend fun getCurrentSchemaVersion(userId: String): Int

    /**
     * Obtiene la versión de esquema objetivo de la app
     */
    fun getTargetSchemaVersion(): Int
}
