package com.jesuskrastev.bali.data.migration.migrations

import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import javax.inject.Inject

/** Adds the optional claimedRankRewards list; older profiles read it as empty. */
class MigrationV13ToV14 @Inject constructor() : FirestoreMigration {
    override val targetVersion: Int = 14
    override val description: String = "Premios de rango reclamados (lista opcional)"

    /** Existing user documents need no rewrite because the field has an empty default. */
    override suspend fun migrate(userId: String) = Unit
}
