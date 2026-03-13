package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.ui.screens.onboarding.components.ProcessingTaskRow

@Composable
fun StepProcessing(progress: Float) {
    val processingTasks = listOf(
        "Analizando tu perfil y experiencia previa...",
        "Identificando patrones en tus temas difíciles...",
        "Optimizando tu horario de estudio personalizado...",
        "Generando estrategia inteligente para tu carnet...",
        "Finalizando tu plan de estudio a medida..."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Construyendo tu plan...",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        processingTasks.forEachIndexed { index, task ->
            val stepProgress = (index + 1).toFloat() / processingTasks.size
            val isCompleted = progress >= stepProgress
            val isActive = progress >= (index.toFloat() / processingTasks.size) && !isCompleted

            ProcessingTaskRow(
                text = task,
                isCompleted = isCompleted,
                isActive = isActive
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
