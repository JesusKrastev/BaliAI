package com.jesuskrastev.bali.ui.screens.auth

import com.jesuskrastev.bali.domain.migration.FirestoreMigrationManager

class FakeFirestoreMigrationManager : FirestoreMigrationManager {
    val executedMigrations = mutableListOf<String>()

    override suspend fun executePendingMigrations(userId: String) {
        executedMigrations.add(userId)
    }

    override suspend fun getCurrentSchemaVersion(userId: String): Int = 1

    override fun getTargetSchemaVersion(): Int = 1

    fun clear() {
        executedMigrations.clear()
    }
}
