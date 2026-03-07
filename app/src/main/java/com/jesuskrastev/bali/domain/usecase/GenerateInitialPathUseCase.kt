package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.path.DgtLearningPathTemplate
import com.jesuskrastev.bali.domain.repository.PathRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case responsible for generating the initial learning path for a new user.
 * 
 * It uses the predefined layout from [DgtLearningPathTemplate] and saves
 * the resulting nodes to the user's data source via [PathRepository].
 */
open class GenerateInitialPathUseCase @Inject constructor(
    private val pathRepository: PathRepository,
    private val userRepository: UserRepository
) {
    open suspend operator fun invoke() {
        val user = userRepository.get().first() ?: return
        val nodes = DgtLearningPathTemplate.buildInitialPath()
        pathRepository.saveGeneratedNodes(user.id, nodes)
    }
}
