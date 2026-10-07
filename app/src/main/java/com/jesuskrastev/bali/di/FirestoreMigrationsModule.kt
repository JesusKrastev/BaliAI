package com.jesuskrastev.bali.di

import com.jesuskrastev.bali.data.migration.migrations.MigrationV10ToV11
import com.jesuskrastev.bali.data.migration.migrations.MigrationV11ToV12
import com.jesuskrastev.bali.data.migration.migrations.MigrationV12ToV13
import com.jesuskrastev.bali.data.migration.migrations.MigrationV13ToV14
import com.jesuskrastev.bali.data.migration.migrations.MigrationV1ToV2
import com.jesuskrastev.bali.data.migration.migrations.MigrationV2ToV3
import com.jesuskrastev.bali.data.migration.migrations.MigrationV3ToV4
import com.jesuskrastev.bali.data.migration.migrations.MigrationV4ToV5
import com.jesuskrastev.bali.data.migration.migrations.MigrationV5ToV6
import com.jesuskrastev.bali.data.migration.migrations.MigrationV6ToV7
import com.jesuskrastev.bali.data.migration.migrations.MigrationV7ToV8
import com.jesuskrastev.bali.data.migration.migrations.MigrationV8ToV9
import com.jesuskrastev.bali.data.migration.migrations.MigrationV9ToV10
import com.jesuskrastev.bali.data.migration.migrations.MigrationV10ToV11
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

    @Provides
    @IntoSet
    @Singleton
    fun provideMigrationV5ToV6(migration: MigrationV5ToV6): FirestoreMigration = migration

    @Provides
    @IntoSet
    @Singleton
    fun provideMigrationV6ToV7(migration: MigrationV6ToV7): FirestoreMigration = migration

    @Provides
    @IntoSet
    @Singleton
    fun provideMigrationV7ToV8(migration: MigrationV7ToV8): FirestoreMigration = migration

    @Provides
    @IntoSet
    @Singleton
    fun provideMigrationV8ToV9(migration: MigrationV8ToV9): FirestoreMigration = migration

    @Provides
    @IntoSet
    @Singleton
    fun provideMigrationV9ToV10(migration: MigrationV9ToV10): FirestoreMigration = migration


    @Provides
    @IntoSet
    @Singleton
    fun provideMigrationV10ToV11(migration: MigrationV10ToV11): FirestoreMigration = migration

    @Provides
    @IntoSet
    @Singleton
    fun provideMigrationV11ToV12(migration: MigrationV11ToV12): FirestoreMigration = migration

    /** Registers the coin-shop inventory migration for authenticated profiles (v12 is reserved). */
    @Provides
    @IntoSet
    @Singleton
    fun provideMigrationV12ToV13(migration: MigrationV12ToV13): FirestoreMigration = migration

    /** Registers the optional XP reward claim ledger. */
    @Provides
    @IntoSet
    @Singleton
    fun provideMigrationV13ToV14(migration: MigrationV13ToV14): FirestoreMigration = migration
    // Proporcionar un Set vacío seguro si no hay migraciones activas (para que Hilt compile correctamente)
    @Provides
    @dagger.multibindings.ElementsIntoSet
    fun provideEmptyMigrations(): Set<FirestoreMigration> = emptySet()
}
