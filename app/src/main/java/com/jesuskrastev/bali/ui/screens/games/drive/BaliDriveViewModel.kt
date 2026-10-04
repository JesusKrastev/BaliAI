package com.jesuskrastev.bali.ui.screens.games.drive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.audio.SoundEffects
import com.jesuskrastev.bali.domain.model.FirstStepTask
import com.jesuskrastev.bali.domain.model.TestMode
import com.jesuskrastev.bali.domain.repository.GameRecordRepository
import com.jesuskrastev.bali.domain.usecase.CompleteFirstStepUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import com.jesuskrastev.bali.ui.screens.games.GameRewards
import com.jesuskrastev.bali.ui.screens.games.GameType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/** First-run hints (what each situation asks for) are shown until this many runs are finished. */
private const val HINT_RUNS = 3

/**
 * Screen state of Bali Drive.
 *
 * @param runId increases with every run; the screen rebuilds the simulation when it changes
 * @param runSeed seed of the current run's route
 * @param bestScore personal best before the current run
 * @param showHints whether the current run announces each situation (first runs only)
 * @param result the finished run, null while driving
 */
data class DriveUiState(
    val runId: Int = 0,
    val runSeed: Long = 0L,
    val bestScore: Int = 0,
    val showHints: Boolean = true,
    val result: DriveResult? = null,
)

/**
 * A finished run and what it earned.
 *
 * @param previousBest personal best before this run
 * @param rewards XP and coins, null until the reward use cases answer
 */
data class DriveResult(val summary: DriveSummary, val previousBest: Int, val rewards: GameRewards? = null) {
    /** True when this run beat the stored best (the first finished run always does). */
    val isNewRecord: Boolean get() = summary.score > previousBest
}

/**
 * Owns Bali Drive's runs: the personal best kept by [GameRecordRepository], the real XP, coins and
 * streak a finished run grants (the same use cases as the other mini-games), its analytics and
 * the sounds the screen asks for.
 *
 * The simulation itself lives in [DriveEngine] inside the screen: it ticks every frame and is pure
 * UI-time state, like the round timers of the other mini-games.
 */
@HiltViewModel
class BaliDriveViewModel @Inject constructor(
    private val incrementXpUseCase: IncrementXpUseCase,
    private val incrementCoinsUseCase: IncrementCoinsUseCase,
    private val incrementStreakUseCase: IncrementStreakUseCase,
    private val completeFirstStepUseCase: CompleteFirstStepUseCase,
    private val gameRecordRepository: GameRecordRepository,
    private val soundEffects: SoundEffects,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DriveUiState())
    val uiState: StateFlow<DriveUiState> = _uiState.asStateFlow()

    private val gameId = GameType.DRIVE.id

    /** Runs already rewarded since the screen opened: replays earn the reduced repeat XP. */
    private var rewardedRuns = 0

    init {
        viewModelScope.launch {
            val record = gameRecordRepository.record(gameId)
            _uiState.update { it.copy(bestScore = record.bestScore, showHints = record.runsPlayed < HINT_RUNS) }
            startRun()
        }
    }

    /** Starts a new run on a freshly shuffled route. */
    fun startRun() {
        _uiState.update { it.copy(runId = it.runId + 1, runSeed = Random.nextLong(), result = null) }
        analyticsTracker.gameStarted(gameId)
    }

    /**
     * Shows the finished run at once, then stores the record and grants its rewards.
     *
     * @param summary figures of the run that just crossed the finish line
     */
    fun finishRun(summary: DriveSummary) {
        val state = _uiState.value
        if (state.result != null) return
        _uiState.update { it.copy(result = DriveResult(summary, previousBest = state.bestScore)) }
        viewModelScope.launch {
            val previous = gameRecordRepository.submitRun(gameId, summary.score)
            val accuracy = summary.resolved * 100 / summary.situations.coerceAtLeast(1)
            val xpEarned = incrementXpUseCase(
                mode = TestMode.PRACTICE,
                correctAnswers = summary.resolved,
                totalQuestions = summary.situations,
                durationSeconds = summary.durationSeconds,
                isRepeat = rewardedRuns++ > 0,
            )
            val coinsGained = incrementCoinsUseCase(accuracy)
            incrementStreakUseCase()
            completeFirstStepUseCase(FirstStepTask.PLAY_GAME)?.let(analyticsTracker::firstStepRewarded)
            analyticsTracker.gameCompleted(gameId, summary.resolved, summary.situations, summary.durationSeconds)
            val rewards = GameRewards(xpEarned, coinsGained, accuracy, summary.durationSeconds)
            _uiState.update {
                it.copy(
                    bestScore = maxOf(previous.bestScore, summary.score),
                    showHints = previous.runsPlayed + 1 < HINT_RUNS,
                    result = it.result?.copy(rewards = rewards),
                )
            }
        }
    }

    /**
     * Logs that the player left mid-run.
     *
     * @param situationsCleared situations already behind the player
     */
    fun abandonRun(situationsCleared: Int) {
        if (_uiState.value.result == null) analyticsTracker.gameAbandoned(gameId, situationsCleared)
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
