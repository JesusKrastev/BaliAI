package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.repository.PathRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Records an attempt on a learning-path node and, when the score is good enough, opens the next one.
 *
 * The attempt is always recorded with its real score; only a score of at least
 * [PASSING_ACCURACY] unlocks what comes after, so a bad attempt can be repeated without losing
 * the way forward being held back by it.
 */
open class CompletePathNodeUseCase @Inject constructor(
    private val pathRepository: PathRepository
) {

    /**
     * Marks [nodeId] as completed with [accuracy] and unlocks the next node if the score passes.
     *
     * @param userId the signed-in user's id
     * @param nodeId the node that was just attempted
     * @param accuracy the score as a percentage, 0 to 100
     */
    open suspend operator fun invoke(userId: String, nodeId: String, accuracy: Int) {
        pathRepository.updateNodeStatus(userId, nodeId, NodeStatus.COMPLETED.name, accuracy)
        if (accuracy < PASSING_ACCURACY) return

        val nodes = pathRepository.getPathNodes(userId).first()
        val current = nodes.find { it.id == nodeId } ?: return
        val next = nodes.filter { it.orderIndex > current.orderIndex }.minByOrNull { it.orderIndex }
        if (next != null && next.status == NodeStatus.LOCKED) {
            pathRepository.updateNodeStatus(userId, next.id, NodeStatus.UNLOCKED.name, null)
        }
    }

    companion object {
        /** Score, as a percentage, that opens the next node. */
        const val PASSING_ACCURACY = 70
    }
}
