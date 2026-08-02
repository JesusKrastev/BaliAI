package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingData
import com.jesuskrastev.bali.ui.screens.onboarding.examCountdownLabel
import com.jesuskrastev.bali.ui.screens.onboarding.optionLabel
import com.jesuskrastev.bali.ui.theme.BaliTheme
import java.util.concurrent.TimeUnit

/** A single row of the plan summary grid. */
private data class SummaryItem(val icon: String, val label: String, val value: String)

/**
 * Reveals the personalised plan built from the user's answers, right before the
 * social proof and the paywall.
 *
 * It mirrors back the "why" the user gave earlier so the plan reads as *theirs*, and
 * closes with the cost of not starting today.
 *
 * @param data the answers collected during the onboarding flow
 */
@Composable
fun StepPlanReveal(data: OnboardingData) {
    val daysLeft = data.examDate?.let { examMillis ->
        val diff = examMillis - System.currentTimeMillis()
        if (diff > 0) TimeUnit.MILLISECONDS.toDays(diff) else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PlanHeader(name = data.name, daysLeft = daysLeft)

        data.motivation?.let { motivation ->
            MotivationCallback(motivation = optionLabel(motivation))
        }

        val summaryItems = buildList {
            data.dailyGoal?.let { add(SummaryItem("⏱️", "RITMO DIARIO", optionLabel(it))) }
            data.learningPreference?.let { add(SummaryItem("🧠", "MÉTODO", optionLabel(it))) }
            if (data.difficultTopics.isNotEmpty()) {
                add(SummaryItem("🔥", "TEMAS A REFORZAR", "${data.difficultTopics.size} temas clave"))
            }
            data.theoryBlocker?.let { add(SummaryItem("🛡️", "PUNTO DÉBIL", optionLabel(it))) }
        }

        summaryItems.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item -> SummaryCard(item, modifier = Modifier.weight(1f)) }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        UrgencyNote()
    }
}

/**
 * Hero card announcing that the plan is ready, personalised with the user's name
 * and the time left until their exam.
 *
 * @param name the user's name, or null if it was skipped
 * @param daysLeft whole days remaining until the exam, or null if no date was given
 */
@Composable
private fun PlanHeader(name: String?, daysLeft: Long?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFFE85A2A))
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(28.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Tu plan personalizado está listo",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = name ?: "futuro conductor",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (daysLeft != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.2f)) {
                    Text(
                        text = "⚡ ${examCountdownLabel(daysLeft)} para el examen",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Repeats the user's own reason for wanting the licence, so the plan is framed as
 * the path to their goal rather than as a study schedule.
 *
 * @param motivation the emoji-free motivation label the user picked
 */
@Composable
private fun MotivationCallback(motivation: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "TU OBJETIVO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = motivation,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Todo el plan está construido para llevarte hasta aquí.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/** Closing note that puts a price on postponing the plan. */
@Composable
private fun UrgencyNote() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF44336).copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color(0xFFF44336).copy(alpha = 0.4f))
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "⚠️", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "El 58% suspende el teórico a la primera",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Un plan que no empiezas hoy, no lo empiezas nunca.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Compact card for one attribute of the generated plan.
 *
 * @param item the icon, label and value to render
 * @param modifier modifier applied to the card surface
 */
@Composable
private fun SummaryCard(item: SummaryItem, modifier: Modifier = Modifier) {
    val containerColor = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFF8FAFC)

    Surface(shape = RoundedCornerShape(20.dp), color = containerColor, modifier = modifier) {
        Column(modifier = Modifier.padding(18.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.icon, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StepPlanRevealPreview() {
    BaliTheme(darkTheme = true) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            StepPlanReveal(
                data = OnboardingData(
                    name = "Jesús",
                    motivation = "💼 Tener mejores oportunidades de trabajo",
                    theoryBlocker = "🧭 Me falta un método claro",
                    dailyGoal = "🕓 30 min (Recomendado)",
                    learningPreference = "🎯 Tests adaptados a mis fallos",
                    difficultTopics = setOf("🛑 Señales y marcas", "🚦 Prioridades de paso"),
                    examDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(21)
                )
            )
        }
    }
}
