package com.jesuskrastev.bali.di

import com.jesuskrastev.bali.data.migration.migrations.MigrationV1ToV2
import com.jesuskrastev.bali.data.migration.migrations.MigrationV2ToV3
import com.jesuskrastev.bali.data.migration.migrations.MigrationV3ToV4
import com.jesuskrastev.bali.data.migration.migrations.MigrationV4ToV5
import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * Módulo para registrar todas las migraciones de Firestore.
 * Cada vez que crees una nueva migración, regístrala aquí con @IntoSet
 */
@Module
@InstallIn(SingletonComponent::class)
object FirestoreMigrationsModule {

    // Ejemplo: descomenta para habilitar migración
    @Provides
    @IntoSet
    @Singleton
    fun provideMigrationV1ToV2(migration: MigrationV1ToV2): FirestoreMigration = migration

    @Provides
    @IntoSet
    @Singleton
    fun provideMigrationV2ToV3(migration: MigrationV2ToV3): FirestoreMigration = migration

    @Provides
    @IntoSet
    @Singleton
    fun provideMigrationV3ToV4(migration: MigrationV3ToV4): FirestoreMigration = migration

    @Provides
    @IntoSet
    @Singleton
    fun provideMigrationV4ToV5(migration: MigrationV4ToV5): FirestoreMigration = migration

    // Proporcionar un Set vacío seguro si no hay migraciones activas (para que Hilt compile correctamente)
    @Provides
    @dagger.multibindings.ElementsIntoSet
    fun provideEmptyMigrations(): Set<FirestoreMigration> = emptySet()
}
