package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero header card
        Surface(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                Color(0xFFE85A2A)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(28.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Tu plan personalizado está listo",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.82f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = data.name ?: "futuro conductor",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (weeksLeft != null && weeksLeft > 0L) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "⚡ $weeksLeft semanas para el examen",
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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Perfil completado",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(completionRatio * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = progressColor
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { completionRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(50)),
                    color = progressColor,
                    trackColor = Color(0xFFE2E8F0),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Cuantos más datos, más preciso tu plan",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

private data class SummaryItem(val icon: String, val label: String, val value: String)

@Composable
private fun SummaryCard(item: SummaryItem, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)

    Surface(
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 0.dp,
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.Start
        ) {
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