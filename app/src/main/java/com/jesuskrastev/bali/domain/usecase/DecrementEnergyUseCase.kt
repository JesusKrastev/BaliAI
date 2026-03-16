package com.jesuskrastev.bali.domain.usecase

interface DecrementEnergyUseCase {
    suspend operator fun invoke(): Int
}
