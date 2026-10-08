package com.jesuskrastev.bali.di

import com.jesuskrastev.bali.data.repository.OneSignalNotificationsRepository
import com.jesuskrastev.bali.domain.repository.NotificationsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {

    @Binds
    @Singleton
    abstract fun bindNotificationsRepository(
        impl: OneSignalNotificationsRepository
    ): NotificationsRepository
}
