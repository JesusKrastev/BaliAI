package com.jesuskrastev.bali.di

import com.jesuskrastev.bali.domain.usecase.DecrementEnergyUseCase
import com.jesuskrastev.bali.domain.usecase.DecrementEnergyUseCaseImpl
import com.jesuskrastev.bali.domain.usecase.RestoreEnergyUseCase
import com.jesuskrastev.bali.domain.usecase.RestoreEnergyUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EnergyModule {

    @Binds
    @Singleton
    abstract fun bindDecrementEnergyUseCase(
        impl: DecrementEnergyUseCaseImpl
    ): DecrementEnergyUseCase

    @Binds
    @Singleton
    abstract fun bindRestoreEnergyUseCase(
        impl: RestoreEnergyUseCaseImpl
    ): RestoreEnergyUseCase
}
