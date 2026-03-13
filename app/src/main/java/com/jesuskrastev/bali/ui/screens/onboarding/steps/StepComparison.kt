package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StepComparison() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Tu ventaja competitiva",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sin Bali
            Column(modifier = Modifier.weight(1f)) {
                ComparisonHeader(title = "Sin Bali", color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                ComparisonHeaderItem(text = "Libros aburridos", icon = "📚", isNegative = true)
                ComparisonHeaderItem(text = "Dudas sin resolver", icon = "❓", isNegative = true)
                ComparisonHeaderItem(text = "Estudiar lo que no sale", icon = "📉", isNegative = true)
                ComparisonHeaderItem(text = "Soledad total", icon = "👤", isNegative = true)
            }

            // Con Bali
            Column(modifier = Modifier.weight(1f)) {
                ComparisonHeader(title = "Con Bali", color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                ComparisonHeaderItem(text = "Plan Inteligente", icon = "🎯", isNegative = false)
                ComparisonHeaderItem(text = "Respuestas al instante", icon = "⚡", isNegative = false)
                ComparisonHeaderItem(text = "Foco en el examen", icon = "🔥", isNegative = false)
                ComparisonHeaderItem(text = "Copiloto 24/7", icon = "🤖", isNegative = false)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Bali analiza tu perfil para que apruebes en tiempo récord.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ComparisonHeader(title: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(vertical = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ComparisonHeaderItem(text: String, icon: String, isNegative: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (isNegative) Color.Gray else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
