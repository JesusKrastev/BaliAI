package com.jesuskrastev.bali.ui.screens.games.drive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.perf.metrics.AddTrace
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.audio.SoundEffects
import com.jesuskrastev.bali.domain.model.FirstStepTask
import com.jesuskrastev.bali.domain.model.TestMode
import com.jesuskrastev.bali.domain.usecase.CompleteFirstStepUseCase
import com.jesuskrastev.bali.domain.usecase.CompleteGameTutorialUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import com.jesuskrastev.bali.domain.usecase.PrepareGameUseCase
import com.jesuskrastev.bali.domain.usecase.SubmitGameRunUseCase
import com.jesuskrastev.bali.ui.screens.games.GameRewards
import com.jesuskrastev.bali.ui.screens.games.GameType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** First-run hints (what each situation asks for) are shown until this many runs are finished. */
private const val HINT_RUNS = 3

/**
 * Owns Bali Drive's runs: the personal best kept by the game record use cases, the real XP, coins and
 * streak a finished run grants (the same use cases as the other mini-games), its analytics and
 * the sounds the screen asks for.
 *
 * The [DriveSession] retains the engine across configuration changes; Compose owns its frame loop.
 */
@HiltViewModel
class BaliDriveViewModel @Inject constructor(
    private val incrementXpUseCase: IncrementXpUseCase,
    private val incrementCoinsUseCase: IncrementCoinsUseCase,
    private val incrementStreakUseCase: IncrementStreakUseCase,
    private val completeFirstStepUseCase: CompleteFirstStepUseCase,
    private val prepareGame: PrepareGameUseCase,
    private val completeTutorial: CompleteGameTutorialUseCase,
    private val submitRun: SubmitGameRunUseCase,
    private val soundEffects: SoundEffects,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DriveUiState())
    val uiState: StateFlow<DriveUiState> = _uiState.asStateFlow()

    private val gameId = GameType.DRIVE.id

    /** Runs already rewarded since the screen opened: replays earn the reduced repeat XP. */
    private var rewardedRuns = 0

    private var tutorialUserId: String? = null
    private var session: DriveSession? = null

    init { prepare() }

    /** Loads the record and persistent tutorial preference; exposes a retry on read failure. */
    fun prepare() {
        _uiState.update { it.copy(phase = DriveScreenPhase.LOADING, error = false) }
        viewModelScope.launch {
            try {
                val prepared = prepareGame(gameId)
                tutorialUserId = prepared.userId
                _uiState.update {
                    it.copy(bestScore = prepared.record.bestScore, showHints = prepared.record.runsPlayed < HINT_RUNS,
                        phase = if (prepared.tutorialCompleted) DriveScreenPhase.PLAYING else DriveScreenPhase.TUTORIAL)
                }
                if (prepared.tutorialCompleted) startRun()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _uiState.update { it.copy(error = true) }
            }
        }
    }

    /** Applies [event] through the pure reducer, persisting only the completed second step. */
    fun onEvent(event: BaliDriveEvent) {
        val before = _uiState.value
        _uiState.update { BaliDriveReducer.reduce(it, event) }
        if (before.phase == DriveScreenPhase.TUTORIAL && _uiState.value.phase == DriveScreenPhase.SAVING_TUTORIAL) {
            viewModelScope.launch {
                try {
                    completeTutorial(gameId, tutorialUserId)
                    _uiState.update { it.copy(phase = DriveScreenPhase.PLAYING) }
                    startRun()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    _uiState.update { it.copy(phase = DriveScreenPhase.TUTORIAL, error = true) }
                }
            }
        }
    }

    /** Starts a shuffled run once preparation and any previous reward attempt have finished. */
    fun startRun() {
        val state = _uiState.value
        if (state.phase !in listOf(DriveScreenPhase.PLAYING, DriveScreenPhase.RESULTS) ||
            state.result?.rewardStatus == DriveRewardStatus.PENDING ||
            (state.phase == DriveScreenPhase.PLAYING && state.runId > 0)) return
        _uiState.update { it.copy(runId = it.runId + 1, runSeed = Random.nextLong(), result = null, phase = DriveScreenPhase.PLAYING) }
        session = null
        analyticsTracker.gameStarted(gameId)
    }

    /** Returns the simulation for [state], retaining it through configuration changes. */
    internal fun session(state: DriveUiState): DriveSession {
        return session?.takeIf { it.runId == state.runId } ?: DriveSession(state.runId,
            DriveEngine(state.runSeed, state.bestScore, state.showHints)).also { session = it }
    }

    /**
     * Shows the finished run at once, then stores the record and grants its rewards.
     *
     * @param summary figures of the run that just crossed the finish line
     * @param runId identity of its originating engine; stale finishes are ignored
     */
    fun finishRun(summary: DriveSummary, runId: Int = _uiState.value.runId) {
        val state = _uiState.value
        if (state.result != null || state.runId != runId || state.phase != DriveScreenPhase.PLAYING) return
        _uiState.update { it.copy(phase = DriveScreenPhase.RESULTS, result = DriveResult(summary, previousBest = state.bestScore)) }
        viewModelScope.launch {
            try {
                withTimeout(15_000) { saveRewards(summary, runId) }
            } catch (timeout: TimeoutCancellationException) {
                markRewardsFailed(runId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                markRewardsFailed(runId)
            }
        }
    }

    /** Saves [summary] and its rewards for [runId], retaining the record even if a later reward fails. */
    @AddTrace(name = "drive_run_rewards")
    private suspend fun saveRewards(summary: DriveSummary, runId: Int) {
        val previous = submitRun(gameId, summary.score)
        _uiState.update {
            if (it.runId != runId) it else it.copy(
                bestScore = maxOf(previous.bestScore, summary.score),
                showHints = previous.runsPlayed + 1 < HINT_RUNS,
            )
        }
        val accuracy = summary.resolved * 100 / summary.situations.coerceAtLeast(1)
        val xpEarned = incrementXpUseCase(
            mode = TestMode.PRACTICE,
            correctAnswers = summary.resolved,
            totalQuestions = summary.situations,
            durationSeconds = summary.durationSeconds,
            isRepeat = rewardedRuns > 0,
        )
        rewardedRuns++
        val coinsGained = incrementCoinsUseCase(accuracy)
        incrementStreakUseCase()
        completeFirstStepUseCase(FirstStepTask.PLAY_GAME)?.let(analyticsTracker::firstStepRewarded)
        analyticsTracker.gameCompleted(gameId, summary.resolved, summary.situations, summary.durationSeconds)
        val rewards = GameRewards(xpEarned, coinsGained, accuracy, summary.durationSeconds)
        _uiState.update {
            if (it.runId != runId) it else it.copy(result = it.result?.copy(rewards = rewards, rewardStatus = DriveRewardStatus.COMPLETE))
        }
    }

    /** Marks the current [runId]'s reward attempt failed without retrying non-idempotent increments. */
    private fun markRewardsFailed(runId: Int) {
        _uiState.update {
            if (it.runId != runId) it else it.copy(result = it.result?.copy(rewardStatus = DriveRewardStatus.FAILED))
        }
    }

    /**
     * Logs that the player left mid-run.
     *
     * @param situationsCleared situations already behind the player
     */
    fun abandonRun(situationsCleared: Int) {
        if (_uiState.value.phase == DriveScreenPhase.PLAYING && _uiState.value.result == null) analyticsTracker.gameAbandoned(gameId, situationsCleared)
    }

    /** Chime of a situation handled well. */
    fun playResolved() = soundEffects.playCorrect()

    /** Soft low notes of a broken rule. */
    fun playFault() = soundEffects.playWrong()

    /**
     * "Pling" of a collected star, a little higher for each star of the same streak.
     *
     * @param streak consecutive stars collected, from 1
     */
    fun playPickup(streak: Int) = soundEffects.playPickup(1f + (streak - 1).coerceIn(0, 12) * 0.06f)

    /** Rising sweep of a collected power-up. */
    fun playPowerUp() = soundEffects.playPowerUp()

    /** Two-tone siren of the ambulance coming from behind. */
    fun playSiren() = soundEffects.playSiren()

    /**
     * Beep of the countdown: low for 3, 2, 1 and high for "go".
     *
     * @param number the number reached, 0 for "go"
     */
    fun playCountdown(number: Int) = soundEffects.playPickup(if (number == 0) 1.5f else 0.6f)

    /**
     * Closing sound of a run: a fanfare when it went well, a calm tone otherwise.
     *
     * @param rating 1–3 stars
     */
    fun playFinish(rating: Int) = if (rating >= 2) soundEffects.playLessonComplete() else soundEffects.playSoftFinish()
}
