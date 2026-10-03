package com.jesuskrastev.bali.ui.screens.subscription

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jesuskrastev.bali.domain.model.ProgressStats
import kotlin.math.roundToInt

private val FIRE = Color(0xFFFF7A00)

/**
 * The cancellation flow, in two steps: first what the subscriber has built with Bali and would
 * lose, then why they want to leave, with an answer for each reason. Keeping the plan is the
 * prominent action on both steps; going on to Google Play stays visible on both, so cancelling
 * never takes more than two taps from here.
 *
 * @param onClose leaves the flow, back to the subscription screen
 * @param onSuggestionsClick opens the suggestions screen, offered to whoever misses something
 * @param modifier layout modifier applied to the scaffold
 * @param viewModel supplies the user's progress and reports every choice
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CancelSubscriptionScreen(
    onClose: () -> Unit,
    onSuggestionsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CancelSubscriptionViewModel = hiltViewModel()
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val step by viewModel.step.collectAsStateWithLifecycle()
    val reason by viewModel.reason.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val keep = {
        viewModel.keepPlan()
        onClose()
    }
    val back = {
        if (step == CancelStep.Reason) viewModel.backToProgress() else keep()
    }
    BackHandler(onBack = back)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = keep,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (step == CancelStep.Progress) "Quiero seguir con Bali" else "Me quedo con Bali",
                        fontWeight = FontWeight.Black
                    )
                }
                TextButton(
                    onClick = {
                        if (step == CancelStep.Progress) {
                            viewModel.continueCancelling()
                        } else {
                            val url = viewModel.goToPlay()
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (_: ActivityNotFoundException) {
                            }
                            onClose()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (step == CancelStep.Progress) "Continuar con la cancelación" else "Cancelar en Google Play",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.padding(padding),
            label = "cancel_step"
        ) { current ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                when (current) {
                    CancelStep.Progress -> ProgressStep(progress)
                    CancelStep.Reason -> ReasonStep(
                        selected = reason,
                        onSelect = viewModel::selectReason,
                        onSuggestionsClick = onSuggestionsClick
                    )
                }
            }
        }
    }
}

/** Step 1: what the user has achieved, their exam, and what cancelling takes away. */
@Composable
private fun ProgressStep(state: CancelProgressUiState) {
    val stats = state.stats
    val hasProgress = stats != null && stats.totalQuestions > 0

    Header(
        title = "¿Seguro que quieres dejarlo?",
        subtitle = if (hasProgress) {
            "Mira todo lo que ya has conseguido. Sería una pena tirarlo ahora."
        } else {
            "Aún no le has sacado todo el partido a Bali. Esto es lo que te estás perdiendo."
        }
    )

    state.daysToExam?.let { days ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        if (days == 1L) "Tu examen es mañana" else "Tu examen es en $days días",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Ahora es cuando más cuenta cada día de práctica.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }

    if (hasProgress) StatsGrid(stats!!)

    LossCard(state)
}

/** The user's figures as a 2×2 grid, plus the pass probability once there is one. */
@Composable
private fun StatsGrid(stats: ProgressStats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        stats.readiness.passProbability?.let { probability ->
            StatTile(
                icon = Icons.Rounded.TrackChanges,
                tint = MaterialTheme.colorScheme.primary,
                value = "${(probability * 100).roundToInt()} %",
                label = "probabilidad de aprobar hoy",
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                icon = Icons.Rounded.LocalFireDepartment,
                tint = FIRE,
                value = if (stats.currentStreak == 1) "1 día" else "${stats.currentStreak} días",
                label = "de racha",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                icon = Icons.Rounded.Star,
                tint = Color(0xFFF2B705),
                value = "Nivel ${stats.level}",
                label = "${stats.xp} XP ganados",
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                icon = Icons.Rounded.QuestionAnswer,
                tint = MaterialTheme.colorScheme.primary,
                value = "${stats.totalQuestions}",
                label = "preguntas respondidas",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                icon = Icons.Rounded.CheckCircle,
                tint = Color(0xFF1B7F3B),
                value = stats.accuracy?.let { "${(it * 100).roundToInt()} %" } ?: "—",
                label = "de aciertos",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * One figure with its icon.
 *
 * @param icon icon shown in the tinted circle
 * @param tint accent colour of the icon
 * @param value the figure, large
 * @param label what the figure counts
 * @param modifier layout modifier
 */
@Composable
private fun StatTile(icon: ImageVector, tint: Color, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(tint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** What cancelling takes away, and from when. */
@Composable
private fun LossCard(state: CancelProgressUiState) {
    val streak = state.stats?.currentStreak ?: 0
    val losses = listOf(
        "Las explicaciones con IA de cada pregunta",
        if (streak > 1) "Tu racha de $streak días" else "Las rachas, el XP y las monedas",
        "Tu plan de estudio y tus estadísticas",
        "Tu progreso sincronizado entre dispositivos"
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                state.accessEndsOn?.let { "Si cancelas, a partir del $it perderás:" } ?: "Si cancelas, perderás:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black
            )
            losses.forEach { loss ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(loss, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/**
 * Step 2: the survey, and an answer to the reason picked.
 *
 * @param selected the reason picked so far, or null
 * @param onSelect invoked with the reason tapped
 * @param onSuggestionsClick opens the suggestions screen
 */
@Composable
private fun ReasonStep(
    selected: CancelReason?,
    onSelect: (CancelReason) -> Unit,
    onSuggestionsClick: () -> Unit
) {
    Header(
        title = "¿Por qué quieres cancelar?",
        subtitle = "Tu respuesta nos ayuda a mejorar Bali."
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CancelReason.entries.forEach { reason ->
            val isSelected = reason == selected
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(reason) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                },
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        reason.label,
                        modifier = Modifier.padding(vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }

    AnimatedVisibility(visible = selected != null && selected != CancelReason.Other) {
        selected?.let { ReasonReply(it, onSuggestionsClick) }
    }
}

/**
 * Bali's answer to a cancellation reason.
 *
 * @param reason the reason picked; [CancelReason.Other] has no answer
 * @param onSuggestionsClick opens the suggestions screen
 */
@Composable
private fun ReasonReply(reason: CancelReason, onSuggestionsClick: () -> Unit) {
    val (title, body) = when (reason) {
        CancelReason.PassedExam ->
            "¡Enhorabuena por el carnet!" to "Nos alegra muchísimo haberte ayudado a conseguirlo. Mucha suerte en la carretera."
        CancelReason.TooExpensive ->
            "Suspender sale más caro" to "Repetir el examen supone volver a pagar la tasa de la DGT y, muchas veces, más clases. Un mes de Bali cuesta menos que una clase práctica de autoescuela."
        CancelReason.NotUsing ->
            "Con 10 minutos al día basta" to "Una sesión corta al día mantiene tu racha y te deja listo para el examen. Activa los recordatorios en Ajustes y te avisamos."
        CancelReason.MissingSomething ->
            "Cuéntanos qué echas en falta" to "Leemos todas las sugerencias, y muchas acaban convertidas en funciones de la app."
        CancelReason.Other -> return
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            }
            Text(body, style = MaterialTheme.typography.bodyMedium)
            if (reason == CancelReason.MissingSomething) {
                TextButton(onClick = onSuggestionsClick) {
                    Text("Enviar una sugerencia", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Title and subtitle at the top of each step.
 *
 * @param title the question or statement, large
 * @param subtitle the line under it
 */
@Composable
private fun Header(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
