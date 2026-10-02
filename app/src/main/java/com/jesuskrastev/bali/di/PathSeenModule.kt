package com.jesuskrastev.bali.di

import com.jesuskrastev.bali.data.repository.DataStorePathSeenRepository
import com.jesuskrastev.bali.domain.repository.PathSeenRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PathSeenModule {

    @Binds
    @Singleton
    abstract fun bindPathSeenRepository(impl: DataStorePathSeenRepository): PathSeenRepository
}
