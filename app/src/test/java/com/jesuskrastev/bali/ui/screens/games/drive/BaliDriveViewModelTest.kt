package com.jesuskrastev.bali.ui.screens.games.drive

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.repository.GameRecord
import com.jesuskrastev.bali.domain.repository.GameRecordRepository
import com.jesuskrastev.bali.domain.usecase.CompleteFirstStepUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import com.jesuskrastev.bali.domain.util.PendingFirstStepRewards
import com.jesuskrastev.bali.ui.screens.auth.FakeAnalyticsTracker
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.util.FakeSoundEffects
import com.jesuskrastev.bali.util.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

/** In-memory [GameRecordRepository]. */
class FakeGameRecordRepository(var stored: GameRecord = GameRecord()) : GameRecordRepository {
    override suspend fun record(gameId: String): GameRecord = stored

    override suspend fun submitRun(gameId: String, score: Int): GameRecord {
        val previous = stored
        stored = GameRecord(maxOf(previous.bestScore, score), previous.runsPlayed + 1)
        return previous
    }
}

class BaliDriveViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = FakeUserRepository()
    private val analytics = FakeAnalyticsTracker(mock(), mock())
    private val sounds = FakeSoundEffects()

    private val summary = DriveSummary(
        score = 2400, resolved = 10, situations = 11, faults = listOf(RULE_STOP),
        starsCollected = 20, starsTotal = 40, durationSeconds = 52,
    )

    /** Builds the ViewModel over fakes; [records] holds the stored personal best. */
    private fun viewModel(records: GameRecordRepository = FakeGameRecordRepository()) = BaliDriveViewModel(
        incrementXpUseCase = IncrementXpUseCase(userRepository),
        incrementCoinsUseCase = IncrementCoinsUseCase(userRepository),
        incrementStreakUseCase = IncrementStreakUseCase(userRepository),
        completeFirstStepUseCase = CompleteFirstStepUseCase(userRepository, PendingFirstStepRewards()),
        gameRecordRepository = records,
        soundEffects = sounds,
        analyticsTracker = analytics,
    )

    @Test
    fun `opening the game loads the record and starts the first run`() = runTest {
        val vm = viewModel(FakeGameRecordRepository(GameRecord(bestScore = 1800, runsPlayed = 5)))

        val state = vm.uiState.value
        assertThat(state.runId).isEqualTo(1)
        assertThat(state.bestScore).isEqualTo(1800)
        assertThat(state.showHints).isFalse()
        assertThat(analytics.gameStartedEvents).containsExactly("bali_drive")
    }

    @Test
    fun `finishing a run stores the record and grants XP, coins and analytics`() = runTest {
        val records = FakeGameRecordRepository(GameRecord(bestScore = 1800, runsPlayed = 1))
        val vm = viewModel(records)
        val coinsBefore = userRepository.get().first()!!.coins

        vm.finishRun(summary)

        val result = vm.uiState.value.result!!
        assertThat(result.isNewRecord).isTrue()
        assertThat(result.previousBest).isEqualTo(1800)
        assertThat(result.rewards!!.xpEarned.xpGained).isGreaterThan(0)
        assertThat(userRepository.get().first()!!.coins).isGreaterThan(coinsBefore)
        assertThat(records.stored).isEqualTo(GameRecord(bestScore = 2400, runsPlayed = 2))
        assertThat(vm.uiState.value.bestScore).isEqualTo(2400)
        assertThat(analytics.gameCompletedEvents.single().score).isEqualTo(10)
    }

    @Test
    fun `a run below the record is not a new record and a replay starts clean`() = runTest {
        val vm = viewModel(FakeGameRecordRepository(GameRecord(bestScore = 5000, runsPlayed = 9)))

        vm.finishRun(summary)
        assertThat(vm.uiState.value.result!!.isNewRecord).isFalse()

        vm.startRun()
        assertThat(vm.uiState.value.result).isNull()
        assertThat(vm.uiState.value.runId).isEqualTo(2)
        assertThat(vm.uiState.value.bestScore).isEqualTo(5000)
    }

    @Test
    fun `a finished run is only rewarded once`() = runTest {
        val vm = viewModel()

        vm.finishRun(summary)
        vm.finishRun(summary)

        assertThat(analytics.gameCompletedEvents).hasSize(1)
    }

    @Test
    fun `leaving mid-run is logged as abandoned, leaving the results is not`() = runTest {
        val vm = viewModel()

        vm.abandonRun(situationsCleared = 3)
        vm.finishRun(summary)
        vm.abandonRun(situationsCleared = 11)

        assertThat(analytics.gameAbandonedEvents).containsExactly("bali_drive" to 3)
    }

    @Test
    fun `star pickups climb in pitch along a streak`() {
        val vm = viewModel()

        (1..4).forEach(vm::playPickup)

        assertThat(sounds.pickupPitches).isInOrder()
        assertThat(sounds.pickupPitches.first()).isEqualTo(1f)
    }
}
