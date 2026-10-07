package com.jesuskrastev.bali.ui.screens.games

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jesuskrastev.bali.ui.screens.games.mechanics.EncuentraElPeligroGame
import com.jesuskrastev.bali.ui.screens.games.mechanics.LegalOMultaGame
import com.jesuskrastev.bali.ui.screens.games.mechanics.PrioridadCruceGame
import com.jesuskrastev.bali.ui.screens.games.mechanics.PuntosCarneGame
import com.jesuskrastev.bali.ui.screens.games.mechanics.SenalRelampagoGame
import com.jesuskrastev.bali.ui.screens.test.ResultKind
import com.jesuskrastev.bali.ui.screens.test.ResultSoundViewModel
import com.jesuskrastev.bali.ui.screens.test.TestResultScreen

/**
 * Hosts an active arcade mini-game session: shows the round chrome and the mechanic matching
 * [game] while the session is in progress, then shows [GameResultScreen] with full arcade
 * styling once all rounds are done.
 */
@Composable
fun GamePlayScreen(
    game: GameType,
    onBackClick: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(game) { viewModel.startSession(game) }

    AnimatedContent(
        targetState = state.isFinished,
        transitionSpec = { (fadeIn() + slideInHorizontally { it / 5 }) togetherWith (fadeOut() + slideOutHorizontally { -it / 5 }) },
        label = "game_result_transition",
        modifier = modifier.fillMaxSize(),
    ) { isFinished ->
        if (isFinished) {
            val rewards = state.rewards
            if (rewards == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                GameResultScreen(
                    xpGained = rewards.xpEarned.xpGained,
                    baseXp = rewards.xpEarned.baseXp,
                    bonusPerfection = rewards.xpEarned.bonusPerfection,
                    bonusFast = rewards.xpEarned.bonusFast,
                    bonusStreak = rewards.xpEarned.bonusStreak,
                    leveledUp = rewards.xpEarned.levelUp,
                    newLevel = rewards.xpEarned.newLevel,
                    score = state.score,
                    accuracy = rewards.accuracy,
                    durationSeconds = rewards.durationSeconds,
                    onContinueClick = onBackClick,
                    onPlayAgainClick = viewModel::replay,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GameSessionHeader(
                    game = game,
                    roundIndex = state.roundIndex,
                    totalRounds = state.totalRounds,
                    score = state.score,
                    onBackClick = {
                        viewModel.abandonSession()
                        onBackClick()
                    },
                )
                // Keying on the round index remounts a fresh mechanic instance per round, so each
                // one only needs to manage the state of a single round rather than a 5-round loop.
                key(state.roundIndex) {
                    when (game) {
                        GameType.PUNTOS_CARNE -> PuntosCarneGame(sessionSeed = state.sessionSeed, roundIndex = state.roundIndex, onRoundResult = viewModel::recordRound, modifier = Modifier.weight(1f))
                        GameType.SENAL -> SenalRelampagoGame(sessionSeed = state.sessionSeed, roundIndex = state.roundIndex, onRoundResult = viewModel::recordRound, modifier = Modifier.weight(1f))
                        GameType.LEGAL_O_MULTA -> LegalOMultaGame(sessionSeed = state.sessionSeed, roundIndex = state.roundIndex, onRoundResult = viewModel::recordRound, modifier = Modifier.weight(1f))
                        GameType.PELIGRO -> EncuentraElPeligroGame(sessionSeed = state.sessionSeed, roundIndex = state.roundIndex, onRoundResult = viewModel::recordRound, modifier = Modifier.weight(1f))
                        // Routed to BaliDriveScreen by AppNavigation: a continuous run, not five rounds.
                        GameType.DRIVE -> Unit
                        GameType.PRIORIDAD_CRUCE -> PrioridadCruceGame(sessionSeed = state.sessionSeed, roundIndex = state.roundIndex, onRoundResult = viewModel::recordRound, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
