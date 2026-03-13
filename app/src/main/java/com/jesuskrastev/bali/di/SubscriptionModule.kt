package com.jesuskrastev.bali.di

import com.jesuskrastev.bali.data.repository.RevenueCatSubscriptionRepository
import com.jesuskrastev.bali.domain.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SubscriptionModule {

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(
        impl: RevenueCatSubscriptionRepository
    ): SubscriptionRepository
}
