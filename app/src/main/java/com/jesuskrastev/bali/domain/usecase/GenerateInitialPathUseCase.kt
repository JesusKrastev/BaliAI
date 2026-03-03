package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.data.repository.UserRepositoryImpl
import com.jesuskrastev.bali.domain.path.DgtLearningPathTemplate
import com.jesuskrastev.bali.domain.repository.PathRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GenerateInitialPathUseCase @Inject constructor(
    private val pathRepository: PathRepository,
    private val userRepository: UserRepositoryImpl
) {
    suspend operator fun invoke() {
        val user = userRepository.get().first() ?: return
        val nodes = DgtLearningPathTemplate.buildInitialPath()
        pathRepository.saveGeneratedNodes(user.id, nodes)
    }
}
