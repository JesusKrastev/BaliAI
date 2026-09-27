package com.jesuskrastev.bali.ui.screens.games

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import com.jesuskrastev.bali.ui.screens.auth.FakeAnalyticsTracker
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Firebase Performance's @AddTrace on GameViewModel weaves in real android.os.Process calls at
// bytecode level, which need Robolectric's Android shadows even for a plain JVM unit test.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeUserRepository = FakeUserRepository()
    private val fakeAnalyticsTracker = FakeAnalyticsTracker(mock(), mock(), mock())

    private val fakeIncrementXpUseCase = IncrementXpUseCase(fakeUserRepository)
    private val fakeIncrementCoinsUseCase = IncrementCoinsUseCase(fakeUserRepository)
    private val fakeIncrementStreakUseCase = IncrementStreakUseCase(fakeUserRepository, mock())

    private lateinit var viewModel: GameViewModel

    @Before
    fun setup() {
        viewModel = GameViewModel(
            incrementXpUseCase = fakeIncrementXpUseCase,
            incrementCoinsUseCase = fakeIncrementCoinsUseCase,
            incrementStreakUseCase = fakeIncrementStreakUseCase,
            analyticsTracker = fakeAnalyticsTracker,
        )
    }

    @Test
    fun `startSession resets score, round and rewards for the chosen game`() = runTest {
        viewModel.startSession(GameType.SENAL)

        val state = viewModel.uiState.value
        assertThat(state.game).isEqualTo(GameType.SENAL)
        assertThat(state.roundIndex).isEqualTo(0)
        assertThat(state.score).isEqualTo(0)
        assertThat(state.isFinished).isFalse()
        assertThat(state.rewards).isNull()
        assertThat(fakeAnalyticsTracker.gameStartedEvents).containsExactly("senal")
    }

    @Test
    fun `recordRound advances to the next round without finishing before the last one`() = runTest {
        viewModel.startSession(GameType.PUNTOS_CARNE)

        viewModel.recordRound(won = true)

        val state = viewModel.uiState.value
        assertThat(state.roundIndex).isEqualTo(1)
        assertThat(state.score).isEqualTo(1)
        assertThat(state.isFinished).isFalse()
        assertThat(fakeAnalyticsTracker.gameCompletedEvents).isEmpty()
    }

    @Test
    fun `recordRound on the last round finishes the session and grants rewards once`() = runTest {
        viewModel.startSession(GameType.SENAL)

        repeat(ROUNDS_PER_SESSION - 1) { viewModel.recordRound(won = true) }
        viewModel.recordRound(won = false)

        val state = viewModel.uiState.value
        assertThat(state.isFinished).isTrue()
        assertThat(state.score).isEqualTo(ROUNDS_PER_SESSION - 1)
        assertThat(state.rewards).isNotNull()
        assertThat(state.rewards!!.accuracy).isEqualTo(80)
        assertThat(fakeAnalyticsTracker.gameCompletedEvents).hasSize(1)
        assertThat(fakeAnalyticsTracker.gameCompletedEvents.first().score).isEqualTo(ROUNDS_PER_SESSION - 1)
    }

    @Test
    fun `replay restarts the same game with a clean session`() = runTest {
        viewModel.startSession(GameType.PUNTOS_CARNE)
        repeat(ROUNDS_PER_SESSION) { viewModel.recordRound(won = true) }
        assertThat(viewModel.uiState.value.isFinished).isTrue()

        viewModel.replay()

        val state = viewModel.uiState.value
        assertThat(state.game).isEqualTo(GameType.PUNTOS_CARNE)
        assertThat(state.roundIndex).isEqualTo(0)
        assertThat(state.score).isEqualTo(0)
        assertThat(state.isFinished).isFalse()
        assertThat(state.rewards).isNull()
    }

    @Test
    fun `replaying the same game earns less XP than the first completion`() = runTest {
        viewModel.startSession(GameType.SENAL)
        repeat(ROUNDS_PER_SESSION) { viewModel.recordRound(won = true) }
        val firstXp = viewModel.uiState.value.rewards!!.xpEarned.xpGained

        viewModel.replay()
        repeat(ROUNDS_PER_SESSION) { viewModel.recordRound(won = true) }
        val secondXp = viewModel.uiState.value.rewards!!.xpEarned.xpGained

        assertThat(secondXp).isLessThan(firstXp)
    }

    @Test
    fun `abandonSession logs the round reached only when the session was not finished`() = runTest {
        viewModel.startSession(GameType.SENAL)
        viewModel.recordRound(won = true)

        viewModel.abandonSession()

        assertThat(fakeAnalyticsTracker.gameAbandonedEvents).containsExactly("senal" to 1)
    }
}
