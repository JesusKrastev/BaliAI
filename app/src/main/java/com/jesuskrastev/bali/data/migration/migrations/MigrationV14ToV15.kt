package com.jesuskrastev.bali.data.migration.migrations

import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import javax.inject.Inject

/** Advances existing profiles to v15 for the optional active streak bet flag. */
class MigrationV14ToV15 @Inject constructor() : FirestoreMigration {
    override val targetVersion: Int = 15
    override val description: String = "Apuesta de racha activa (campo opcional)"

    /** Leaves [userId] unchanged because a missing flag reads as false; returns no value. */
    override suspend fun migrate(userId: String) = Unit
}
