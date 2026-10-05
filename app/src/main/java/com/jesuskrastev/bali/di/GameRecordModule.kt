package com.jesuskrastev.bali.di

import com.jesuskrastev.bali.data.repository.DataStoreGameRecordRepository
import com.jesuskrastev.bali.domain.repository.GameRecordRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GameRecordModule {

    @Binds
    @Singleton
    abstract fun bindGameRecordRepository(impl: DataStoreGameRecordRepository): GameRecordRepository
}
