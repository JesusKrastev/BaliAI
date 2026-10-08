package com.jesuskrastev.bali.data.migration.migrations

import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import javax.inject.Inject

/**
 * Migración v10 → v11: las respuestas ganan `questionId`, `topic` y `mode`. No reescribe nada.
 *
 * Los tres campos son nuevos y opcionales en `users/{userId}/answers`: un documento anterior se
 * lee con los valores por defecto (`questionId` vacío, `topic` y `mode` nulos), y el id de esas
 * respuestas se calcula del texto al leerlas (`Answer.resolvedQuestionId`). Rellenarlas aquí
 * tocaría los datos de todos los usuarios en el arranque, y una migración que falla se reintenta
 * en cada arranque (`MigrationException`), así que el riesgo no compensa: el tema de una
 * respuesta antigua tampoco se puede deducir de ella sola.
 *
 * La clase existe porque la regla del repo pide una migración para cada cambio de estructura y
 * para que la versión del esquema avance igual en todas las cuentas.
 */
class MigrationV10ToV11 @Inject constructor() : FirestoreMigration {

    override val targetVersion: Int = 11
    override val description: String = "Respuestas con id de pregunta, tema y modo (campos opcionales)"

    /**
     * Does nothing: see the class comment.
     *
     * @param userId the user being migrated, unused
     */
    override suspend fun migrate(userId: String) = Unit
}
