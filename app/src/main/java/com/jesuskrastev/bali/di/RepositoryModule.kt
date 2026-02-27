package com.jesuskrastev.bali.di

import com.jesuskrastev.bali.data.repository.AuthRepositoryImpl
import com.jesuskrastev.bali.data.repository.SuggestionsRepositoryImpl
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.repository.SuggestionsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSuggestionsRepository(
        suggestionsRepositoryImpl: SuggestionsRepositoryImpl
    ): SuggestionsRepository
}