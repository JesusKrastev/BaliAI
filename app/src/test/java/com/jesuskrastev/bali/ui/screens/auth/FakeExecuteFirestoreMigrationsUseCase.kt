package com.jesuskrastev.bali.ui.screens.auth

import com.jesuskrastev.bali.domain.usecase.ExecuteFirestoreMigrationsUseCase

/**
 * Fake implementation for ExecuteFirestoreMigrationsUseCase testing
 */
class FakeExecuteFirestoreMigrationsUseCase(
    migrationManager: com.jesuskrastev.bali.domain.migration.FirestoreMigrationManager
) : ExecuteFirestoreMigrationsUseCase(migrationManager) {
    var invocations = mutableListOf<String>()
    
    override suspend fun invoke(userId: String) {
        invocations.add(userId)
    }

    
    fun clear() {
        invocations.clear()
    }
}
