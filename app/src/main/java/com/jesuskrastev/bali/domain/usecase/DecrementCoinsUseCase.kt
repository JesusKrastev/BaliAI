package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Spends coins from the user's balance to gate a paid action (e.g. entering the exam).
 *
 * The balance check and the deduction happen as one atomic operation in
 * [UserRepository.decrementCoinsIfEnough] rather than being read here and written back,
 * so two overlapping calls (a double tap, two screens spending at once) can't both read
 * the same starting balance and both succeed.
 */
open class DecrementCoinsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * Attempts to charge [amount] coins.
     *
     * @param amount the coin cost to charge.
     * @return true if the balance was sufficient and the coins were spent, false otherwise.
     */
    open suspend operator fun invoke(amount: Int): Boolean =
        userRepository.decrementCoinsIfEnough(amount)
}
