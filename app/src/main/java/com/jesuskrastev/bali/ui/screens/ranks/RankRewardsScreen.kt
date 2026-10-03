package com.jesuskrastev.bali.ui.screens.ranks

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.model.RankProgression
import com.jesuskrastev.bali.domain.model.RankReward
import com.jesuskrastev.bali.domain.model.RankTier

/** Renders the rank path, using [viewModel] for claims and [onBackClick] for navigation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankRewardsScreen(onBackClick: () -> Unit, viewModel: RankRewardsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }
    val currentRank = RankProgression.rankFor(state.xp)
    val nextRank = RankProgression.ranks.firstOrNull { it.requiredXp > state.xp }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camino de rangos", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "progress") {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Image(painterResource(currentRank.badgeRes()), null, Modifier.size(72.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currentRank.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text("${state.xp} XP", style = MaterialTheme.typography.titleMedium)
                            nextRank?.let { next ->
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { (state.xp - currentRank.requiredXp).toFloat() /
                                        (next.requiredXp - currentRank.requiredXp) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("Siguiente: ${next.name} · ${next.requiredXp} XP", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            item(key = "intro") {
                Text(
                    "Sube de rango y toca cada premio para recoger tus monedas.",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            itemsIndexed(RankProgression.rewards, key = { _, reward -> reward.id }) { index, reward ->
                val rank = RankProgression.ranks.last { it.requiredXp <= reward.requiredXp }
                Column(Modifier.fillMaxWidth()) {
                    if (index > 0) {
                        val primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        Canvas(Modifier.fillMaxWidth().height(24.dp)) {
                            val left = size.width * 0.36f
                            val right = size.width * 0.64f
                            drawLine(
                                color = primary,
                                start = Offset(if (index % 2 == 0) right else left, 0f),
                                end = Offset(if (index % 2 == 0) left else right, size.height),
                                strokeWidth = 8.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    RewardStop(
                        reward = reward,
                        rank = rank,
                        unlocked = state.xp >= reward.requiredXp,
                        claimed = reward.id in state.claimedIds,
                        claiming = state.claimingId == reward.id,
                        onClaim = { viewModel.claim(reward.id) },
                        modifier = Modifier.fillMaxWidth().padding(
                            start = if (index % 2 == 0) 16.dp else 48.dp,
                            end = if (index % 2 == 0) 48.dp else 16.dp
                        )
                    )
                }
            }
            item(key = "end") { Spacer(Modifier.height(20.dp)) }
        }
    }
}

/** Draws [reward] with [rank], its claim state, and the [onClaim] action in [modifier]. */
@Composable
private fun RewardStop(
    reward: RankReward,
    rank: RankTier,
    unlocked: Boolean,
    claimed: Boolean,
    claiming: Boolean,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked && !claimed) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(painterResource(rank.badgeRes()), null, Modifier.size(60.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${reward.requiredXp} XP · ${rank.name}", fontWeight = FontWeight.Black)
                Text("${reward.coins} monedas", style = MaterialTheme.typography.bodyMedium)
            }
            when {
                claimed -> Text("Recogido", style = MaterialTheme.typography.labelLarge)
                unlocked -> Button(onClick = onClaim, enabled = !claiming) {
                    if (claiming) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Recoger")
                }
                else -> Text("Bloqueado", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** Returns the generated badge drawable for this rank's stable id. */
fun RankTier.badgeRes(): Int = when (id) {
    "aprendiz" -> R.drawable.rank_aprendiz
    "conductor" -> R.drawable.rank_conductor
    "explorador" -> R.drawable.rank_explorador
    "piloto" -> R.drawable.rank_piloto
    "experto" -> R.drawable.rank_experto
    "maestro" -> R.drawable.rank_maestro
    else -> R.drawable.rank_leyenda
}
