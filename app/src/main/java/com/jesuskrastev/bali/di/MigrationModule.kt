package com.jesuskrastev.bali.di

import com.jesuskrastev.bali.data.migration.FirestoreMigrationManagerImpl
import com.jesuskrastev.bali.domain.migration.FirestoreMigrationManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MigrationModule {

    @Binds
    abstract fun bindMigrationManager(impl: FirestoreMigrationManagerImpl): FirestoreMigrationManager
}
