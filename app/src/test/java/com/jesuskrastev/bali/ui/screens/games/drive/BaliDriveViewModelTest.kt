package com.jesuskrastev.bali.ui.screens.games.drive

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.repository.GameRecord
import com.jesuskrastev.bali.domain.repository.GameRecordRepository
import com.jesuskrastev.bali.domain.usecase.PrepareGameUseCase
import com.jesuskrastev.bali.domain.usecase.CompleteGameTutorialUseCase
import com.jesuskrastev.bali.domain.usecase.SubmitGameRunUseCase
import com.jesuskrastev.bali.ui.screens.auth.FakeAuthRepository
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.mockito.kotlin.mock

/** In-memory [GameRecordRepository]. */
open class FakeGameRecordRepository(var stored: GameRecord = GameRecord(), var tutorialDone: Boolean = true) : GameRecordRepository {
    /** Returns the configured tutorial completion for this fixture. */
    override suspend fun tutorialCompleted(gameId: String, userId: String?) = tutorialDone
    /** Persists tutorial completion in the fixture. */
    override suspend fun completeTutorial(gameId: String, userId: String?) { tutorialDone = true }
    override suspend fun record(gameId: String): GameRecord = stored

    override suspend fun submitRun(gameId: String, score: Int): GameRecord {
        val previous = stored
        stored = GameRecord(maxOf(previous.bestScore, score), previous.runsPlayed + 1)
        return previous
    }
}

// Firebase Performance instruments saveRewards with Android calls, as in GameViewModelTest.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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

    /** Builds the ViewModel over fakes with [records] and an optional [xpUseCase]. */
    internal fun viewModel(
        records: GameRecordRepository = FakeGameRecordRepository(),
        xpUseCase: IncrementXpUseCase = IncrementXpUseCase(userRepository),
    ) = BaliDriveViewModel(
        incrementXpUseCase = xpUseCase,
        incrementCoinsUseCase = IncrementCoinsUseCase(userRepository),
        incrementStreakUseCase = IncrementStreakUseCase(userRepository),
        completeFirstStepUseCase = CompleteFirstStepUseCase(userRepository, PendingFirstStepRewards()),
        prepareGame = PrepareGameUseCase(records, FakeAuthRepository()),
        completeTutorial = CompleteGameTutorialUseCase(records),
        submitRun = SubmitGameRunUseCase(records),
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
    /** Completing both steps persists the preference and starts the countdown exactly once. */
    @Test
    fun `tutorial stays pending until both steps are completed and is skipped on reopening`() = runTest {
        val records = FakeGameRecordRepository(tutorialDone = false)
        val vm = viewModel(records)
        assertThat(vm.uiState.value.phase).isEqualTo(DriveScreenPhase.TUTORIAL)
        assertThat(vm.uiState.value.runId).isEqualTo(0)
        vm.onEvent(BaliDriveEvent.CompleteTutorial)
        assertThat(records.tutorialDone).isFalse()
        vm.onEvent(BaliDriveEvent.NextTutorialStep)
        vm.onEvent(BaliDriveEvent.CompleteTutorial)
        vm.onEvent(BaliDriveEvent.CompleteTutorial)
        assertThat(records.tutorialDone).isTrue()
        assertThat(vm.uiState.value.runId).isEqualTo(1)
        assertThat(viewModel(records).uiState.value.phase).isEqualTo(DriveScreenPhase.PLAYING)
    }

    /** A failed preference write leaves the second step available for a safe retry. */
    @Test
    fun `failed tutorial persistence does not start a run or mark completion`() = runTest {
        val records = object : FakeGameRecordRepository(tutorialDone = false) {
            /** Fails the [gameId]/[userId] write so completion remains pending. */
            override suspend fun completeTutorial(gameId: String, userId: String?) { error("disk unavailable") }
        }
        val vm = viewModel(records)
        vm.onEvent(BaliDriveEvent.NextTutorialStep)
        vm.onEvent(BaliDriveEvent.CompleteTutorial)
        assertThat(vm.uiState.value.error).isTrue()
        assertThat(vm.uiState.value.phase).isEqualTo(DriveScreenPhase.TUTORIAL)
        assertThat(vm.uiState.value.runId).isEqualTo(0)
        assertThat(records.tutorialDone).isFalse()
    }

    /** A pending run cannot be replayed and a duplicate finish does not start another write. */
    @Test
    fun `slow rewards block replay and duplicate finish until saved`() = runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        var writes = 0
        val records = object : FakeGameRecordRepository() {
            /** Waits before storing [score] for [gameId], then returns the previous record. */
            override suspend fun submitRun(gameId: String, score: Int): GameRecord {
                writes++
                gate.await()
                return super.submitRun(gameId, score)
            }
        }
        val vm = viewModel(records)
        vm.finishRun(summary)
        vm.startRun()
        vm.finishRun(summary)
        assertThat(vm.uiState.value.runId).isEqualTo(1)
        assertThat(vm.uiState.value.result!!.rewardStatus).isEqualTo(DriveRewardStatus.PENDING)
        assertThat(writes).isEqualTo(1)
        gate.complete(Unit)
        assertThat(vm.uiState.value.result!!.rewardStatus).isEqualTo(DriveRewardStatus.COMPLETE)
        vm.startRun()
        vm.finishRun(summary, runId = 1)
        assertThat(vm.uiState.value.runId).isEqualTo(2)
        assertThat(vm.uiState.value.result).isNull()
    }

    /** An unresponsive write is bounded and leaves replay/exit available without retrying rewards. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `reward timeout exposes failed status and allows a fresh run`() = runTest {
        val records = object : FakeGameRecordRepository() {
            /** Delays [gameId]'s [score] write beyond the reward timeout; returns the prior record. */
            override suspend fun submitRun(gameId: String, score: Int): GameRecord {
                kotlinx.coroutines.delay(60_000)
                return super.submitRun(gameId, score)
            }
        }
        val vm = viewModel(records)
        vm.finishRun(summary)
        testScheduler.advanceTimeBy(15_001)
        assertThat(vm.uiState.value.result!!.rewardStatus).isEqualTo(DriveRewardStatus.FAILED)
        assertThat(vm.uiState.value.result!!.rewards).isNull()
        vm.startRun()
        assertThat(vm.uiState.value.runId).isEqualTo(2)
    }

    /** A failed XP write must not make the next successfully rewarded run a reduced-XP replay. */
    @Test
    fun `failed XP award does not consume the first run XP rate`() = runTest {
        val repeats = mutableListOf<Boolean>()
        val xpUseCase = object : IncrementXpUseCase(userRepository) {
            /** Records [isRepeat], fails once, then awards XP for the same run parameters. */
            override suspend fun invoke(
                mode: com.jesuskrastev.bali.domain.model.TestMode,
                correctAnswers: Int,
                totalQuestions: Int,
                durationSeconds: Int,
                isRepeat: Boolean,
            ): com.jesuskrastev.bali.domain.model.XpEarned {
                repeats += isRepeat
                if (repeats.size == 1) error("XP temporarily unavailable")
                return super.invoke(mode, correctAnswers, totalQuestions, durationSeconds, isRepeat)
            }
        }
        val vm = viewModel(xpUseCase = xpUseCase)

        vm.finishRun(summary)
        assertThat(vm.uiState.value.result!!.rewardStatus).isEqualTo(DriveRewardStatus.FAILED)
        vm.startRun()
        vm.finishRun(summary)

        assertThat(repeats).containsExactly(false, false).inOrder()
        assertThat(vm.uiState.value.result!!.rewardStatus).isEqualTo(DriveRewardStatus.COMPLETE)
    }

    /** The retained engine continues the same run when a new screen tree asks for its session. */
    @Test
    fun `session identity is retained until replay`() = runTest {
        val vm = viewModel()
        val original = vm.session(vm.uiState.value)
        original.engine.update(4f, 0f, false)
        assertThat(vm.session(vm.uiState.value)).isSameInstanceAs(original)
        vm.finishRun(summary)
        vm.startRun()
        assertThat(vm.session(vm.uiState.value)).isNotSameInstanceAs(original)
        vm.startRun()
        assertThat(vm.uiState.value.runId).isEqualTo(2)
    }

}
