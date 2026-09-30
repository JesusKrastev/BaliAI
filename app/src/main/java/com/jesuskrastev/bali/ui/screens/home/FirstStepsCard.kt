package com.jesuskrastev.bali.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.model.FIRST_STEPS_BONUS_COINS
import com.jesuskrastev.bali.domain.model.FirstStepReward
import com.jesuskrastev.bali.domain.model.FirstStepTask
import com.jesuskrastev.bali.domain.model.FirstStepsProgress
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen

/** Headline of a [FirstStepTask] row. */
private val FirstStepTask.title: String
    get() = when (this) {
        FirstStepTask.FIRST_TEST -> "Haz tu primer test"
        FirstStepTask.ASK_BALI -> "Pregúntale una duda a Bali"
        FirstStepTask.PLAY_GAME -> "Juega un minijuego"
    }

/** One-line reason to do a [FirstStepTask], shown under its title. */
private val FirstStepTask.hint: String
    get() = when (this) {
        FirstStepTask.FIRST_TEST -> "Verás las explicaciones de la IA"
        FirstStepTask.ASK_BALI -> "Tu profesor de teórico 24/7"
        FirstStepTask.PLAY_GAME -> "Aprende en un par de minutos"
    }

/**
 * Day-0 card pinned to the top of Home: three small tasks that each pay coins, then a closing
 * invitation to take the first simulacro. Once the three are done the task list gives way to that
 * invitation, so the card always shows a single next action.
 *
 * @param progress how far the student is; drives the counter, the bar and each row's state
 * @param onTaskClick invoked when a pending task's row is tapped, to take the student to it
 * @param onExamClick invoked when the closing "haz tu primer simulacro" button is tapped
 * @param onDismissClick invoked when the student taps the close button
 * @param modifier layout modifier applied to the card
 * @param isTaskEnabled whether a pending task can be started right now (the first test has to
 *   wait for the learning path to exist)
 */
@Composable
fun FirstStepsCard(
    progress: FirstStepsProgress,
    onTaskClick: (FirstStepTask) -> Unit,
    onExamClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
    isTaskEnabled: (FirstStepTask) -> Boolean = { true }
) {
    val total = FirstStepTask.entries.size
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 8.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Tus primeros pasos · ${progress.doneCount}/$total",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                IconButton(onClick = onDismissClick) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Ocultar primeros pasos",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(modifier = Modifier.padding(end = 12.dp)) {
                LinearProgressIndicator(
                    progress = { progress.doneCount.toFloat() / total },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (progress.isComplete) {
                    ExamInvitation(onExamClick = onExamClick)
                } else {
                    Text(
                        text = "Completa los $total y haz tu primer simulacro",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FirstStepTask.entries.forEach { task ->
                            FirstStepRow(
                                task = task,
                                isDone = progress.isDone(task),
                                isEnabled = isTaskEnabled(task),
                                onClick = { onTaskClick(task) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Y +$FIRST_STEPS_BONUS_COINS monedas extra por completar los $total",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Closing state of [FirstStepsCard]: with every task done, the next thing worth doing is the
 * first simulacro, the closest thing to the real exam.
 *
 * @param onExamClick invoked when the button is tapped
 */
@Composable
private fun ExamInvitation(onExamClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.bali),
            contentDescription = null,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "¿Aprobarías hoy?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Has completado tus primeros pasos. Haz tu primer simulacro y descúbrelo.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onExamClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("HAZ TU PRIMER SIMULACRO", fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * One task of the card: a check that fills in once it is done, its title and hint, and the coins
 * it pays. A done row is dimmed and inert.
 *
 * @param task the task the row stands for
 * @param isDone whether it was already completed (and paid)
 * @param isEnabled whether it can be started right now; ignored once done
 * @param onClick invoked when a pending, enabled row is tapped
 */
@Composable
private fun FirstStepRow(
    task: FirstStepTask,
    isDone: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = !isDone && isEnabled,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDone) 0.25f else 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CheckMark(isDone = isDone)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDone) 0.5f else 1f)
                )
                Text(
                    text = if (!isDone && !isEnabled) "Preparando tu ruta…" else task.hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDone) 0.5f else 1f)
                )
            }
            CoinsChip(coins = task.coins, isDone = isDone)
        }
    }
}

/**
 * Round tick box: hollow while the task is pending, green with a check once done.
 *
 * @param isDone whether to draw the completed state
 */
@Composable
private fun CheckMark(isDone: Boolean) {
    val outline = MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .then(
                if (isDone) Modifier.background(BaliAccentGreen)
                else Modifier.border(2.dp, outline, CircleShape)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isDone) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Hecho",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/**
 * Small coin pill showing what a task pays.
 *
 * @param coins reward shown as "+N"
 * @param isDone dims the pill once the reward has been collected
 */
@Composable
private fun CoinsChip(coins: Int, isDone: Boolean) {
    Row(
        modifier = Modifier.graphicsLayer { alpha = if (isDone) 0.4f else 1f },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.coin),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "+$coins",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black
        )
    }
}

/**
 * The small celebration shown on Home when a first step has just paid out: a coin that pops in,
 * the amount and a line about what was achieved. When it was the last step it also says so and
 * points at the closing simulacro.
 *
 * @param reward what was just earned
 * @param onDismiss invoked when the student closes the dialog
 */
@Composable
fun FirstStepRewardDialog(
    reward: FirstStepReward,
    onDismiss: () -> Unit
) {
    val coinScale = remember { Animatable(0f) }
    LaunchedEffect(reward) {
        coinScale.snapTo(0f)
        coinScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.coin),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer {
                            scaleX = coinScale.value
                            scaleY = coinScale.value
                        }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("¡+${reward.coins} monedas!", fontWeight = FontWeight.Black)
            }
        },
        text = {
            Text(
                text = if (reward.completedAll) {
                    "¡Primeros pasos completados! Incluye +$FIRST_STEPS_BONUS_COINS de bonus. " +
                        "Ahora, ¿aprobarías hoy? Haz tu primer simulacro."
                } else {
                    reward.task.celebration
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("¡GENIAL!", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/** What the celebration says about a [FirstStepTask] that was not the last one. */
private val FirstStepTask.celebration: String
    get() = when (this) {
        FirstStepTask.FIRST_TEST -> "Primer test hecho. ¡Así se empieza!"
        FirstStepTask.ASK_BALI -> "Ya sabes que Bali resuelve tus dudas cuando quieras."
        FirstStepTask.PLAY_GAME -> "Minijuego completado. ¡Aprender también puede ser divertido!"
    }

/**
 * Confirmation shown before hiding the card while coins are still up for grabs, since hiding it
 * stops the remaining tasks from paying.
 *
 * @param pendingCoins coins the student would give up
 * @param onConfirm invoked when they choose to hide the card anyway
 * @param onCancel invoked when they keep it
 */
@Composable
fun DismissFirstStepsDialog(
    pendingCoins: Int,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("¿Ocultar tus primeros pasos?", fontWeight = FontWeight.Black) },
        text = {
            Text("Si los ocultas, dejarás de poder ganar las $pendingCoins monedas que te quedan.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("OCULTAR", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("CONTINUAR", fontWeight = FontWeight.Bold) }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
