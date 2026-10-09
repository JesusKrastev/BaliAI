package com.jesuskrastev.bali.ui.screens.ranks

import androidx.lifecycle.ViewModelStore
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.RankReward
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.util.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

/** A controllable claim boundary backed by the existing hand-written user fake. */
private class FakeRankUserRepository(
    private val backing: FakeUserRepository = FakeUserRepository()
) : UserRepository by backing {
    var claimCalls = 0
    var gate: CompletableDeferred<Unit>? = null

    /** Waits for [gate] then grants [reward] through the fake; returns whether it was claimed. */
    override suspend fun claimRankReward(reward: RankReward): Boolean {
        claimCalls++
        gate?.await()
        return backing.claimRankReward(reward)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RankRewardsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val store = ViewModelStore()

    /** Cancels viewModelScope before the rule resets Main; returns Unit. */
    @After
    fun clearViewModels() { store.clear() }

    /** Grants a pack once even if the collect button is tapped twice while storage is pending. */
    @Test
    fun repeatedTapGrantsPackOnlyOnce() = runTest {
        val repository = FakeRankUserRepository()
        repository.insert(User(xp = 600, coins = 10))
        repository.gate = CompletableDeferred()
        val viewModel = RankRewardsViewModel(repository)
        store.put("rank", viewModel)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.claim("practice_pack_600")
        viewModel.claim("practice_pack_600")
        assertThat(repository.claimCalls).isEqualTo(1)
        repository.gate!!.complete(Unit)

        val user = repository.get().first()!!
        assertThat(user.coins).isEqualTo(10)
        assertThat(user.hints).isEqualTo(2)
        assertThat(user.fiftyFifties).isEqualTo(1)
        assertThat(viewModel.uiState.value.celebration!!.reward.id).isEqualTo("practice_pack_600")
        viewModel.claim("practice_pack_600")
        assertThat(repository.claimCalls).isEqualTo(1)
    }

    /** Unreached and unknown rewards must not start a storage operation. */
    @Test
    fun lockedAndUnknownPrizesAreIgnored() = runTest {
        val repository = FakeRankUserRepository()
        repository.insert(User(xp = 39))
        val viewModel = RankRewardsViewModel(repository)
        store.put("rank", viewModel)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        viewModel.claim("hint_40")
        viewModel.claim("unknown")
        assertThat(repository.claimCalls).isEqualTo(0)
    }

    /** Cancelling the screen grants nothing; reopening allows the same prize to be collected. */
    @Test
    fun cancellingAClaimDoesNotShowAnErrorOrGrantThePrize() = runTest {
        val repository = FakeRankUserRepository()
        repository.insert(User(xp = 600))
        repository.gate = CompletableDeferred()
        val viewModel = RankRewardsViewModel(repository)
        store.put("rank", viewModel)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        viewModel.claim("practice_pack_600")
        store.clear()
        assertThat(repository.get().first()!!.claimedRankRewards).isEmpty()
        assertThat(viewModel.uiState.value.message).isNull()
        assertThat(viewModel.uiState.value.celebration).isNull()
        val reopened = RankRewardsViewModel(repository)
        store.put("reopened", reopened)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { reopened.uiState.collect() }
        assertThat(reopened.uiState.value.claimingId).isNull()
        reopened.claim("practice_pack_600")
        repository.gate!!.complete(Unit)
        assertThat(repository.get().first()!!.hints).isEqualTo(2)
        assertThat(repository.get().first()!!.claimedRankRewards).containsExactly("practice_pack_600")
    }
}
