package com.jesuskrastev.bali.data.migration.migrations

import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import javax.inject.Inject

/**
 * Migración v7 → v8
 *
 * Cambios:
 * - Registra tres campos nuevos en el documento de usuario, que alimentan la tarjeta
 *   "Tus primeros pasos" de Home:
 *   - `firstStepsStartedAt` (Long): cuándo se inscribió la cuenta en la tarjeta. 0 o ausente
 *     significa "nunca inscrita".
 *   - `firstStepsDone` (List<String>): ids de las tareas ya completadas y cobradas.
 *   - `firstStepsDismissed` (Boolean): true cuando el alumno ocultó la tarjeta.
 *
 * No hay nada que rellenar, y es a propósito: la inscripción se hace al crear la cuenta
 * (`AuthViewModel`), de modo que las cuentas que ya existen quedan sin `firstStepsStartedAt` y
 * no ven nunca la tarjeta. Esta migración NO debe escribir `firstStepsStartedAt`: también se
 * ejecuta sobre cuentas recién creadas por esta versión (su documento aún no trae
 * `schemaVersion`), y pisarlo les quitaría la tarjeta. Si algún día se decide enseñársela a los
 * suscriptores existentes, se hará en una migración posterior que inscriba solo a quien no
 * tenga el campo.
 *
 * Existe para que `schemaVersion` refleje la estructura real del documento y para dejar
 * registrado el cambio, tal y como exige el flujo de migraciones del proyecto.
 */
class MigrationV7ToV8 @Inject constructor() : FirestoreMigration {

    override val targetVersion: Int = 8
    override val description: String =
        "Registrar los campos firstStepsStartedAt/firstStepsDone/firstStepsDismissed (tarjeta de primeros pasos)"

    override suspend fun migrate(userId: String) {
        // Intencionadamente vacía: ver la documentación de la clase.
    }
}
