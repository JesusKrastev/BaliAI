package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingData
import java.util.concurrent.TimeUnit

@Composable
fun StepLossAversion(data: OnboardingData) {
    val scrollState = rememberScrollState()

    val weeksLeft = data.examDate?.let { examMillis ->
        val diff = examMillis - System.currentTimeMillis()
        if (diff > 0) TimeUnit.MILLISECONDS.toDays(diff) / 7 else null
    }

    // Calcula completion ratio
    val filledFields = listOfNotNull(
        data.dailyGoal, data.learningPreference, 
        if (data.difficultTopics.isNotEmpty()) "filled" else null,
        data.concern, data.studyTime
    ).size
    val completionRatio = (filledFields.toFloat() / 5f).coerceIn(0f, 1f)
    val progressColor = when {
        completionRatio >= 0.7f -> Color(0xFF4CAF50)
        completionRatio >= 0.5f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Hero header card
        val primaryContainer = MaterialTheme.colorScheme.primaryContainer
        val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
        val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
        
        Surface(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .background(Brush.linearGradient(listOf(primaryContainer, secondaryContainer)))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "🎯 Tu plan está listo, ${data.name ?: "futuro conductor"}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    
                    if (weeksLeft != null && weeksLeft > 0L) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "⚡ Solo te quedan $weeksLeft semanas",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Grid de cards de resumen
        val summaryItems = buildList {
            data.dailyGoal?.let { add(SummaryItem("⏱️", "OBJETIVO DIARIO", it.removePrefix("⚡ ").removePrefix("🕐 ").removePrefix("🔥 "))) }
            data.learningPreference?.let {
                val clean = it.drop(if (it.firstOrNull()?.toString()?.length == 2) 3 else 0)
                add(SummaryItem("🧠", "MÉTODO", clean))
            }
            if (data.difficultTopics.isNotEmpty()) {
                add(SummaryItem("🔥", "TEMAS A REFORZAR", "${data.difficultTopics.size} temas clave"))
            }
            data.concern?.let {
                val clean = it.drop(if (it.firstOrNull()?.toString()?.length == 2) 3 else 0)
                add(SummaryItem("🛡️", "PUNTO DÉBIL", clean))
            }
            data.studyTime?.let {
                val clean = it.drop(if (it.firstOrNull()?.toString()?.length == 2) 3 else 0)
                add(SummaryItem("⏰", "MOMENTO ÓPTIMO", clean))
            }
        }

        if (summaryItems.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val chunkedItems = summaryItems.chunked(2)
                chunkedItems.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { item ->
                            SummaryCard(item, modifier = Modifier.weight(1f))
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 3. Barra de progreso del plan
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nivel de preparación de tu plan",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(completionRatio * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = progressColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(12.dp)
            ) {
                LinearProgressIndicator(
                    progress = { completionRatio },
                    modifier = Modifier.fillMaxSize(),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }

        // 4. Card de motivación/urgencia final
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🏆 Estás listo para aprobar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Los alumnos con plan personalizado aprueban 3x más rápido. No lo pierdas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private data class SummaryItem(val icon: String, val label: String, val value: String)

@Composable
private fun SummaryCard(item: SummaryItem, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = item.icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
