package com.jesuskrastev.bali.di

import com.jesuskrastev.bali.data.audio.SoundPoolSoundEffects
import com.jesuskrastev.bali.domain.audio.SoundEffects
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SoundModule {

    @Binds
    @Singleton
    abstract fun bindSoundEffects(impl: SoundPoolSoundEffects): SoundEffects
}
