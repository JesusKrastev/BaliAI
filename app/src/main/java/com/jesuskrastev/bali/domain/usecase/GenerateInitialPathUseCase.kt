package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.path.DgtLearningPathTemplate
import com.jesuskrastev.bali.domain.repository.PathRepository
import javax.inject.Inject

/**
 * Use case responsible for generating the initial learning path for a new user.
 * 
 * It uses the predefined layout from [DgtLearningPathTemplate] and saves
 * the resulting nodes to the user's data source via [PathRepository].
 * Works for both authenticated users (saves to Firestore) and guests (saves to local Room).
 */
open class GenerateInitialPathUseCase @Inject constructor(
    private val pathRepository: PathRepository,
    private val authRepository: AuthRepository
) {
    open suspend operator fun invoke() {
        // Use the authenticated userId, or empty string for guest (local storage)
        val userId = authRepository.currentUser() ?: ""
        val nodes = DgtLearningPathTemplate.buildInitialPath()
        pathRepository.saveGeneratedNodes(userId, nodes)
    }
}

