package com.jesuskrastev.bali.ui.screens.topics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Topic(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String
)

val dgtTopics = listOf(
    Topic("signals", "Señales y Marcas", "🛑", "Señales de advertencia, reglamentación e indicación."),
    Topic("priority", "Prioridad de Paso", "🚦", "Cruces, intersecciones y normas de preferencia."),
    Topic("speed", "Velocidad", "🏎️", "Límites genéricos, específicos y distancias."),
    Topic("maneuvers", "Maniobras", "🔄", "Incorporaciones, adelantamientos y giros."),
    Topic("lighting", "Alumbrado", "💡", "Uso de luces en diferentes condiciones."),
    Topic("mechanics", "Mecánica", "🔧", "Mantenimiento básico y componentes del vehículo."),
    Topic("safety", "Seguridad Vial", "🍻", "Alcohol, drogas, fatiga y distracciones."),
    Topic("docs", "Documentación", "📄", "Seguros, ITV y permisos necesarios."),
    Topic("users", "Usuarios de la vía", "🚲", "Peatones, ciclistas y vehículos especiales."),
    Topic("theory", "Conceptos Básicos", "📖", "Definiciones y normas generales de circulación.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsScreen(
    onBackClick: () -> Unit,
    onTopicClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tests por Tema", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Elige un tema para practicar específicamente lo que necesitas reforzar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                )
            }

            items(dgtTopics) { topic ->
                TopicCard(topic = topic, onClick = { onTopicClick(topic.title) })
            }
        }
    }
}

@Composable
fun TopicCard(topic: Topic, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(text = topic.emoji, fontSize = 28.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = topic.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Rounded.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
