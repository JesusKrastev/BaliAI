package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

open class DecrementCoinsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    open suspend operator fun invoke(amount: Int): Boolean {
        val user = userRepository.get().first() ?: return false
        if (user.coins < amount) return false
        
        val newTotal = user.coins - amount
        userRepository.updateCoins(newTotal)
        return true
    }
}
