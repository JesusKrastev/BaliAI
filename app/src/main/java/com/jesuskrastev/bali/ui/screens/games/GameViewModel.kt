package com.jesuskrastev.bali.ui.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.perf.metrics.AddTrace
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.model.FirstStepTask
import com.jesuskrastev.bali.domain.model.TestMode
import com.jesuskrastev.bali.domain.model.XpEarned
import com.jesuskrastev.bali.domain.usecase.CompleteFirstStepUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/** Number of rounds played in a single arcade mini-game session. */
const val ROUNDS_PER_SESSION = 5

/** Real progression rewards granted once a mini-game session finishes. */
data class GameRewards(
    val xpEarned: XpEarned,
    val coinsGained: Int,
    val accuracy: Int,
    val durationSeconds: Int,
)

/**
 * Holds the session-level state of a five-round mini-game: current round, score and rewards.
 *
 * @param sessionSeed shared by every round's mechanic (via [pickForRound]) so the 5 questions
 *   drawn this session are a shuffle of its pool rather than 5 independent, repeat-prone picks
 */
data class GameSessionUiState(
    val game: GameType = GameType.PUNTOS_CARNE,
    val roundIndex: Int = 0,
    val totalRounds: Int = ROUNDS_PER_SESSION,
    val score: Int = 0,
    val isFinished: Boolean = false,
    val rewards: GameRewards? = null,
    val sessionSeed: Long = 0L,
)

/**
 * Owns the session state of the DGT arcade mini-games.
 *
 * Each mini-game composable drives its own round-by-round timing and interaction locally
 * (reflexes, timers, gestures are ephemeral UI concerns); this ViewModel only tracks how many
 * rounds were won and, once the session ends, applies the same XP/coins/streak rewards a full
 * test grants via [IncrementXpUseCase], [IncrementCoinsUseCase] and [IncrementStreakUseCase],
 * plus the one-off first-steps prize through [CompleteFirstStepUseCase].
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val incrementXpUseCase: IncrementXpUseCase,
    private val incrementCoinsUseCase: IncrementCoinsUseCase,
    private val incrementStreakUseCase: IncrementStreakUseCase,
    private val completeFirstStepUseCase: CompleteFirstStepUseCase,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameSessionUiState())
    val uiState: StateFlow<GameSessionUiState> = _uiState.asStateFlow()

    private var sessionStartMs = 0L

    /** Game types already rewarded once this ViewModel's lifetime — replaying one earns reduced XP. */
    private val rewardedGames = mutableSetOf<GameType>()

    /** Starts a fresh five-round session for [game], resetting score and rewards. */
    @AddTrace(name = "start_dgt_minigame")
    fun startSession(game: GameType) {
        sessionStartMs = System.currentTimeMillis()
        _uiState.value = GameSessionUiState(game = game, sessionSeed = Random.nextLong())
        analyticsTracker.gameStarted(game.id)
    }

    /**
     * Records the outcome of the round in progress and advances to the next one, or finishes
     * the session and grants rewards when it was the last round.
     *
     * @param won whether the player beat this round's challenge in time
     */
    fun recordRound(won: Boolean) {
        val state = _uiState.value
        if (state.isFinished) return
        val newScore = state.score + if (won) 1 else 0
        if (state.roundIndex == state.totalRounds - 1) {
            _uiState.update { it.copy(score = newScore) }
            viewModelScope.launch { grantSessionRewards(newScore) }
        } else {
            _uiState.update { it.copy(roundIndex = it.roundIndex + 1, score = newScore) }
        }
    }

    /** Calls the real reward use cases for the finished session and publishes its rewards. */
    @AddTrace(name = "finish_dgt_minigame")
    private suspend fun grantSessionRewards(finalScore: Int) {
        val game = _uiState.value.game
        val durationSeconds = ((System.currentTimeMillis() - sessionStartMs) / 1000).toInt().coerceAtLeast(1)
        val accuracy = ((finalScore.toFloat() / ROUNDS_PER_SESSION) * 100).toInt()

        // add() returns false when `game` was already rewarded, i.e. this is a replay.
        val isRepeat = !rewardedGames.add(game)
        val xpEarned = incrementXpUseCase(
            mode = TestMode.PRACTICE,
            correctAnswers = finalScore,
            totalQuestions = ROUNDS_PER_SESSION,
            durationSeconds = durationSeconds,
            isRepeat = isRepeat,
        )
        val coinsGained = incrementCoinsUseCase(accuracy)
        incrementStreakUseCase()
        // Separate prize on top of coinsGained: the first finished session is the "Juega un
        // minijuego" step of Home's first-steps bar.
        completeFirstStepUseCase(FirstStepTask.PLAY_GAME)?.let(analyticsTracker::firstStepRewarded)
        analyticsTracker.gameCompleted(game.id, finalScore, ROUNDS_PER_SESSION, durationSeconds)

        _uiState.update {
            it.copy(isFinished = true, rewards = GameRewards(xpEarned, coinsGained, accuracy, durationSeconds))
        }
    }

    /** Logs that the player left the arcade before finishing the current session. */
    fun abandonSession() {
        val state = _uiState.value
        if (!state.isFinished) analyticsTracker.gameAbandoned(state.game.id, state.roundIndex)
    }

    /** Restarts the current mini-game without leaving its route. */
    fun replay() = startSession(_uiState.value.game)
}
