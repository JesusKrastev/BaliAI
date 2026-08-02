package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingConfig
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingData
import com.jesuskrastev.bali.ui.screens.onboarding.components.ProcessingTaskRow
import com.jesuskrastev.bali.ui.screens.onboarding.examCountdownLabel
import com.jesuskrastev.bali.ui.screens.onboarding.optionLabel
import com.jesuskrastev.bali.ui.theme.BaliTheme
import java.util.concurrent.TimeUnit

private val RING_SIZE = 148.dp

/**
 * The "building your plan" screen.
 *
 * Every line names something the user actually told us, so the wait reads as work being
 * done on *their* answers rather than as a loading bar with decorative captions. The
 * screen hides the mascot and carries its own title, which leaves room for the ring.
 *
 * @param progress how far the fake build has got, between 0f and 1f
 * @param data the answers collected during the onboarding flow
 */
@Composable
fun StepProcessing(progress: Float, data: OnboardingData) {
    val tasks = remember(data) { buildProcessingTasks(data) }
    val percentage = (progress * 100).toInt().coerceIn(0, 100)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "processing_ring"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Construyendo tu plan",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = data.name?.let { "Lo estamos armando solo para ti, $it" }
                ?: "Lo estamos armando solo para ti",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        ProgressRing(progress = animatedProgress, percentage = percentage)

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                tasks.forEachIndexed { index, task ->
                    val completedAt = (index + 1).toFloat() / tasks.size
                    val startsAt = index.toFloat() / tasks.size

                    ProcessingTaskRow(
                        text = task,
                        isCompleted = progress >= completedAt,
                        isActive = progress in startsAt..<completedAt
                    )
                }
            }
        }
    }
}

/**
 * Circular progress with the percentage in the middle, so the wait has a visible end.
 *
 * @param progress current progress between 0f and 1f
 * @param percentage the same value as a whole number, shown in the centre
 */
@Composable
private fun ProgressRing(progress: Float, percentage: Int) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(RING_SIZE + 40.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.16f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(RING_SIZE),
            strokeWidth = 10.dp,
            strokeCap = StrokeCap.Round,
            color = primaryColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = primaryColor
        )
    }
}

/**
 * Builds the checklist from what the user answered.
 *
 * The list is always the same length so the layout never jumps, and every entry has a
 * neutral fallback for the questions that went unanswered.
 *
 * @param data the answers collected during the onboarding flow
 * @return one line per task, in the order they get ticked off
 */
private fun buildProcessingTasks(data: OnboardingData): List<String> = listOf(
    when (data.experience) {
        OnboardingConfig.EXPERIENCE_RETRY -> "Revisando por qué se falla a la primera"
        OnboardingConfig.EXPERIENCE_FIRST_TIME -> "Preparando tu arranque desde cero"
        else -> "Analizando tu punto de partida"
    },

    data.licenseType
        ?.let { "Cargando el temario oficial de ${optionLabel(it)}" }
        ?: "Cargando el temario oficial de la DGT",

    when {
        data.difficultTopics.size == 1 ->
            "Reforzando ${optionLabel(data.difficultTopics.first())}"
        data.difficultTopics.size > 1 ->
            "Reforzando tus ${data.difficultTopics.size} temas más flojos"
        else -> "Localizando tus puntos débiles"
    },

    examWorkLabel(data.examDate),

    data.dailyGoal
        ?.let { "Cuadrando tus ${optionLabel(it).substringBefore(" (")} al día" }
        ?: "Cuadrando tu agenda de estudio",

    data.motivation
        ?.let { "Apuntando todo a: ${optionLabel(it).replaceFirstChar { c -> c.lowercase() }}" }
        ?: "Rematando tu estrategia anti-trampa"
)

/**
 * Describes the scheduling task in terms of the user's own exam date.
 *
 * @param examDateMillis the chosen exam date, or null if it was skipped
 * @return the task line for the calendar step
 */
private fun examWorkLabel(examDateMillis: Long?): String {
    val days = examDateMillis
        ?.minus(System.currentTimeMillis())
        ?.let { TimeUnit.MILLISECONDS.toDays(it) }
        ?: return "Repartiendo el temario por semanas"

    return if (days > 0) {
        "Repartiendo el temario en ${examCountdownLabel(days)}"
    } else {
        "Repartiendo el temario por semanas"
    }
}

@Preview(showBackground = true)
@Composable
private fun StepProcessingPreview() {
    BaliTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            StepProcessing(
                progress = 0.45f,
                data = OnboardingData(
                    name = "Jesús",
                    licenseType = "🚗 Coche (B)",
                    experience = OnboardingConfig.EXPERIENCE_FIRST_TIME,
                    motivation = "💼 Tener mejores oportunidades de trabajo",
                    dailyGoal = "🕓 30 min (Recomendado)",
                    difficultTopics = setOf("🛑 Señales y marcas", "🚦 Prioridades de paso"),
                    examDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(21)
                )
            )
        }
    }
}
