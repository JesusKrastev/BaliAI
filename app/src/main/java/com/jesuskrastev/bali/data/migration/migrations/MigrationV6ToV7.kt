package com.jesuskrastev.bali.data.migration.migrations

import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import javax.inject.Inject

/**
 * Migración v6 → v7
 *
 * Cambios:
 * - Añade el campo opcional `planTargetMillis` al documento `users/{userId}`: el día para el
 *   que el plan del onboarding prometió el carnet ("Puedes tener tu carnet antes del…"), que
 *   Home enseña en la tarjeta del plan.
 *
 * No hay datos que rellenar: la fecha solo se conoce al terminar el onboarding, así que los
 * usuarios anteriores no la tienen y no se puede reconstruir (el ritmo de estudio con el que se
 * calculó nunca se guardó). Un documento sin el campo se lee como 0, que el mapper convierte en
 * null, y Home les pide la fecha del examen. La migración existe para que `schemaVersion`
 * refleje la estructura real del documento y para dejar registrado el cambio, tal y como exige
 * el flujo de migraciones del proyecto.
 */
class MigrationV6ToV7 @Inject constructor() : FirestoreMigration {

    override val targetVersion: Int = 7
    override val description: String =
        "Registrar el campo planTargetMillis (fecha prometida por el plan del onboarding)"

    /** Nada que escribir: ver el KDoc de la clase. */
    override suspend fun migrate(userId: String) = Unit
}
