package com.jesuskrastev.bali.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.FIRST_STEPS_BONUS_COINS
import com.jesuskrastev.bali.domain.model.FirstStepTask
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.util.PendingFirstStepRewards
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CompleteFirstStepUseCaseTest {

    private val userRepository = FakeUserRepository()
    private val pendingRewards = PendingFirstStepRewards()
    private val completeFirstStep = CompleteFirstStepUseCase(userRepository, pendingRewards)

    private suspend fun coins() = userRepository.get().first()!!.coins

    @Test
    fun `completing a task pays its coins, records it and queues the celebration`() = runTest {
        userRepository.enrollInFirstStepsForTest()
        val coinsBefore = coins()

        val reward = completeFirstStep(FirstStepTask.FIRST_TEST)

        assertThat(reward?.task).isEqualTo(FirstStepTask.FIRST_TEST)
        assertThat(reward?.coins).isEqualTo(FirstStepTask.FIRST_TEST.coins)
        assertThat(reward?.completedAll).isFalse()
        assertThat(coins()).isEqualTo(coinsBefore + FirstStepTask.FIRST_TEST.coins)
        assertThat(userRepository.get().first()!!.firstSteps.completed).containsExactly(FirstStepTask.FIRST_TEST)
        assertThat(pendingRewards.next.first()).isEqualTo(reward)
    }

    @Test
    fun `the same task is never paid twice`() = runTest {
        userRepository.enrollInFirstStepsForTest()
        completeFirstStep(FirstStepTask.ASK_BALI)
        val coinsAfterFirst = coins()

        val second = completeFirstStep(FirstStepTask.ASK_BALI)

        assertThat(second).isNull()
        assertThat(coins()).isEqualTo(coinsAfterFirst)
    }

    @Test
    fun `the last task also pays the completion bonus and says so`() = runTest {
        userRepository.enrollInFirstStepsForTest(setOf(FirstStepTask.FIRST_TEST, FirstStepTask.ASK_BALI))

        val reward = completeFirstStep(FirstStepTask.PLAY_GAME)

        assertThat(reward?.completedAll).isTrue()
        assertThat(reward?.coins).isEqualTo(FirstStepTask.PLAY_GAME.coins + FIRST_STEPS_BONUS_COINS)
    }

    @Test
    fun `the bonus is celebrated when the account paid it even if the local snapshot is behind`() = runTest {
        // Another phone already did the other two tasks: the cached profile here still shows
        // none, but the account (the repository) pays the last task plus the bonus.
        userRepository.enrollInFirstStepsForTest()
        val accountAhead = object : UserRepository by userRepository {
            override suspend fun completeFirstStep(task: FirstStepTask): Int = task.coins + FIRST_STEPS_BONUS_COINS
        }

        val reward = CompleteFirstStepUseCase(accountAhead, pendingRewards)(FirstStepTask.PLAY_GAME)

        assertThat(reward?.completedAll).isTrue()
        assertThat(reward?.coins).isEqualTo(FirstStepTask.PLAY_GAME.coins + FIRST_STEPS_BONUS_COINS)
    }

    @Test
    fun `a task the account refuses to pay is not celebrated`() = runTest {
        // The cached profile says pending, but the account already has it (done on another phone).
        userRepository.enrollInFirstStepsForTest()
        val accountAhead = object : UserRepository by userRepository {
            override suspend fun completeFirstStep(task: FirstStepTask): Int = 0
        }

        val reward = CompleteFirstStepUseCase(accountAhead, pendingRewards)(FirstStepTask.ASK_BALI)

        assertThat(reward).isNull()
        assertThat(pendingRewards.next.first()).isNull()
    }

    @Test
    fun `completing all three in any order pays 100 coins in total`() = runTest {
        userRepository.enrollInFirstStepsForTest()
        val coinsBefore = coins()

        listOf(FirstStepTask.PLAY_GAME, FirstStepTask.FIRST_TEST, FirstStepTask.ASK_BALI)
            .forEach { completeFirstStep(it) }

        assertThat(coins() - coinsBefore).isEqualTo(100)
    }

    @Test
    fun `an account that was never enrolled earns nothing`() = runTest {
        val coinsBefore = coins()

        val reward = completeFirstStep(FirstStepTask.FIRST_TEST)

        assertThat(reward).isNull()
        assertThat(coins()).isEqualTo(coinsBefore)
        assertThat(pendingRewards.next.first()).isNull()
    }

    @Test
    fun `a dismissed bar stops paying`() = runTest {
        userRepository.enrollInFirstStepsForTest()
        userRepository.dismissFirstSteps()
        val coinsBefore = coins()

        val reward = completeFirstStep(FirstStepTask.FIRST_TEST)

        assertThat(reward).isNull()
        assertThat(coins()).isEqualTo(coinsBefore)
    }

    @Test
    fun `with no user there is nothing to complete`() = runTest {
        userRepository.clear()

        assertThat(completeFirstStep(FirstStepTask.FIRST_TEST)).isNull()
    }
}
