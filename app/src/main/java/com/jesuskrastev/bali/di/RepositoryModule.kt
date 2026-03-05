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

    @Binds
    @Singleton
    abstract fun bindPathRepository(
        pathRepositoryImpl: com.jesuskrastev.bali.data.repository.PathRepositoryImpl
    ): com.jesuskrastev.bali.domain.repository.PathRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: com.jesuskrastev.bali.data.repository.UserRepositoryImpl
    ): com.jesuskrastev.bali.domain.repository.UserRepository

    @Binds
    @Singleton
    abstract fun bindAnswerRepository(
        answerRepositoryImpl: com.jesuskrastev.bali.data.repository.AnswerRepositoryImpl
    ): com.jesuskrastev.bali.domain.repository.AnswerRepository

    @Binds
    @Singleton
    abstract fun bindTestResultRepository(
        testResultRepositoryImpl: com.jesuskrastev.bali.data.repository.TestResultRepositoryImpl
    ): com.jesuskrastev.bali.domain.repository.TestResultRepository
}