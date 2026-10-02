package com.jesuskrastev.bali.data.migration.migrations

import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import javax.inject.Inject

/**
 * Migración v8 → v9: reservada, no hace nada.
 *
 * La build de internal del 1.2.2 usó este número para el velocímetro de racha (D-019), que
 * recortaba la racha a 7. Se descartó (D-021) y no debe llegar a producción, pero hay cuentas
 * que ya están en v9: reutilizar el número para otra cosa haría que se saltaran la migración
 * (E-017). Se queda vacía para que quien viene de v8 pase por la misma versión.
 */
class MigrationV8ToV9 @Inject constructor() : FirestoreMigration {

    override val targetVersion: Int = 9
    override val description: String = "Versión reservada (velocímetro de racha descartado)"

    /**
     * Does nothing: see the class comment.
     *
     * @param userId the user being migrated, unused
     */
    override suspend fun migrate(userId: String) = Unit
}
