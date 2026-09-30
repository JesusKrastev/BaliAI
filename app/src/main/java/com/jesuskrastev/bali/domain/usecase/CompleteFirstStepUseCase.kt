package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.model.FirstStepReward
import com.jesuskrastev.bali.domain.model.FirstStepTask
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.util.PendingFirstStepRewards
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Marks a first-steps task as done and pays its coins, once per account.
 *
 * Called from the screens where each task actually happens (test, chat, mini-game). It is cheap to
 * call unconditionally: for any account that is not in the first-steps window — enrolled, not
 * dismissed, task not yet done — it returns without touching the network. The reward is a separate
 * prize on top of what the test or game already pays, so [IncrementCoinsUseCase] is untouched.
 */
open class CompleteFirstStepUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val pendingRewards: PendingFirstStepRewards
) {

    /**
     * Completes [task] for the current account.
     *
     * @param task the task the student just did.
     * @return the coins earned, which are also queued for Home to celebrate, or null when nothing
     *   was paid: not enrolled, dismissed, already done, or the write did not go through.
     */
    open suspend operator fun invoke(task: FirstStepTask): FirstStepReward? {
        val progress = userRepository.get().first()?.firstSteps ?: return null
        if (!progress.isActive || progress.isDone(task)) return null

        val coins = userRepository.completeFirstStep(task)
        if (coins <= 0) return null

        val reward = FirstStepReward(
            task = task,
            coins = coins,
            completedAll = (progress.completed + task).containsAll(FirstStepTask.entries)
        )
        pendingRewards.publish(reward)
        return reward
    }
}
