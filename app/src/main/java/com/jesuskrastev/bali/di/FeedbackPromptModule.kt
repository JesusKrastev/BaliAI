package com.jesuskrastev.bali.di

import com.jesuskrastev.bali.data.repository.DataStoreFeedbackPromptRepository
import com.jesuskrastev.bali.domain.repository.FeedbackPromptRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FeedbackPromptModule {

    @Binds
    @Singleton
    abstract fun bindFeedbackPromptRepository(impl: DataStoreFeedbackPromptRepository): FeedbackPromptRepository
}
