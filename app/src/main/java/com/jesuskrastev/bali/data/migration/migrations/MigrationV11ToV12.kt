package com.jesuskrastev.bali.data.migration.migrations

import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import javax.inject.Inject

/**
 * Migración v11 → v12: el documento de usuario gana los campos de "Tus primeros pasos". No
 * reescribe nada.
 *
 * Campos nuevos y opcionales en `users/{userId}`:
 * - `firstStepsStartedAt` (Long): cuándo se inscribió la cuenta en la barra. 0 o ausente
 *   significa "nunca inscrita".
 * - `firstStepsDone` (List<String>): ids de las tareas ya completadas y cobradas.
 * - `firstStepsDismissed` (Boolean): true cuando el alumno ocultó la barra.
 *
 * No hay nada que rellenar, y es a propósito: la inscripción se hace al crear la cuenta
 * (`AuthViewModel`), así que las cuentas que ya existen se quedan sin `firstStepsStartedAt` y no
 * ven la barra. Esta migración NO debe escribir `firstStepsStartedAt`: también se ejecuta sobre
 * las cuentas creadas antes de que los documentos nuevos se sellaran con su `schemaVersion` (su
 * documento empieza en v1) y pisarlo les quitaría la barra. Si algún día se decide enseñársela a los suscriptores que ya existen, se
 * hará en una migración posterior que inscriba solo a quien no tenga el campo.
 *
 * La clase existe porque la regla del repo pide una migración para cada cambio de estructura y
 * para que la versión del esquema avance igual en todas las cuentas.
 *
 * Una build de internal de la rama de primeros pasos usó la v7 → v8 para esto mismo (E-017): las
 * cuentas que la abrieron ya pueden tener estos campos, y se leen igual.
 */
class MigrationV11ToV12 @Inject constructor() : FirestoreMigration {

    override val targetVersion: Int = 12
    override val description: String =
        "Campos de primeros pasos: firstStepsStartedAt, firstStepsDone, firstStepsDismissed (opcionales)"

    /**
     * Does nothing: see the class comment.
     *
     * @param userId the user being migrated, unused
     */
    override suspend fun migrate(userId: String) = Unit
}
