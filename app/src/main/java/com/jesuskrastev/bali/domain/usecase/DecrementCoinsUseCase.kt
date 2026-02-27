package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.data.repository.UserRepositoryImpl
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DecrementCoinsUseCase @Inject constructor(
    private val userRepository: UserRepositoryImpl
) {
    suspend operator fun invoke(amount: Int): Boolean {
        val user = userRepository.get().first() ?: return false
        if (user.coins < amount) return false
        
        val newTotal = user.coins - amount
        userRepository.updateCoins(newTotal)
        return true
    }
}
