package com.jesuskrastev.bali.ui.screens.onboarding.steps

import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingData

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.ui.theme.BaliTheme

@Composable
fun StepComparison(data: OnboardingData) {
    val scrollState = rememberScrollState()
    
    val cardBgColor = MaterialTheme.colorScheme.surfaceVariant
    val onCardColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val baliGreen = Color(0xFF4CAF50)

    // Valores para las barras
    val redValue = 0.47f // 47%
    val greenValue = 0.89f // 89%
    
    // Animación de altura: 0f a 1f en 800ms
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "BarHeightAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Card principal
        Surface(
            color = cardBgColor,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tu Diagnóstico Personalizado 🧬",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = onCardColor
                )

                // Subtítulo gris pequeño
                Text(
                    text = "Analizado en base a tus respuestas y perfil de conductor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onCardColor.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 16.dp),
                    textAlign = TextAlign.Start
                )

                // Texto en negrita centrado (Dinámico)
                val mainHeading = when {
                    data.experience?.contains("cero") == true -> 
                        "Tus respuestas indican una |clara ventaja| si usas Bali al empezar de cero."
                    data.experience?.contains("suspendido") == true -> 
                        "Esta vez |es la definitiva|: con Bali eliminaremos tus errores anteriores."
                    data.experience?.contains("carnet") == true -> 
                        "Aprovecharemos tu |experiencia| para que saques el teórico de forma exprés."
                    else -> "Tus respuestas indican una |clara ventaja| con el método Bali."
                }

                Text(
                    text = parsePersonalizedMessage(mainHeading, primaryColor),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = onCardColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // Gráfico de Barras con Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(vertical = 16.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        
                        val barWidth = 60.dp.toPx()
                        val spacing = 80.dp.toPx()
                        
                        // Centrar las barras
                        val redBarX = (canvasWidth / 2) - barWidth - (spacing / 2)
                        val greenBarX = (canvasWidth / 2) + (spacing / 2)

                        // Alturas visuales (Ironía visual: roja es más alta para representar "fallos/riesgo")
                        // Siguiendo la instrucción: roja alta ( ~85% del canvas), verde más baja
                        val redFullHeight = canvasHeight * 0.85f
                        val greenFullHeight = canvasHeight * 0.45f

                        val redCurrentHeight = redFullHeight * animationProgress
                        val greenCurrentHeight = greenFullHeight * animationProgress

                        // Dibujar barra roja (Izquierda)
                        drawRoundRect(
                            color = errorColor,
                            topLeft = Offset(redBarX, canvasHeight - redCurrentHeight),
                            size = Size(barWidth, redCurrentHeight),
                            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                        )

                        // Dibujar barra verde (Derecha)
                        drawRoundRect(
                            color = baliGreen,
                            topLeft = Offset(greenBarX, canvasHeight - greenCurrentHeight),
                            size = Size(barWidth, greenCurrentHeight),
                            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                        )

                        // Dibujar Eje X (línea base)
                        drawLine(
                            color = onCardColor.copy(alpha = 0.2f),
                            start = Offset(0f, canvasHeight),
                            end = Offset(canvasWidth, canvasHeight),
                            strokeWidth = 2.dp.toPx()
                        )

                        // Texto de porcentaje dentro de las barras (Usando nativeCanvas para precisión)
                        if (animationProgress > 0.5f) { // Solo mostrar cuando las barras hayan crecido un poco
                            val textPaint = android.graphics.Paint().apply {
                                color = Color.White.toArgb()
                                textSize = 16.sp.toPx()
                                isFakeBoldText = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }

                            drawContext.canvas.nativeCanvas.drawText(
                                "47%",
                                redBarX + (barWidth / 2),
                                canvasHeight - redCurrentHeight + 28.dp.toPx(),
                                textPaint
                            )

                            drawContext.canvas.nativeCanvas.drawText(
                                "89%",
                                greenBarX + (barWidth / 2),
                                canvasHeight - greenCurrentHeight + 28.dp.toPx(),
                                textPaint
                            )
                        }
                    }
                }

                // Etiquetas debajo de las barras
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Estudio\nManual",
                            style = MaterialTheme.typography.labelSmall,
                            color = onCardColor.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.width(40.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Tu Nivel\ncon Bali",
                            style = MaterialTheme.typography.labelSmall,
                            color = onCardColor.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Estadística final
                Text(
                    text = "¡Multiplicas por 2 tus opciones de aprobar! 🏅",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StepComparisonPreview() {
    BaliTheme(darkTheme = true) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            StepComparison(
                data = OnboardingData(
                    name = "User",
                    experience = "\uD83C\uDF93 Empiezo de cero absoluto"
                )
            )
        }
    }
}

private fun parsePersonalizedMessage(message: String, primaryColor: Color): AnnotatedString {
    return buildAnnotatedString {
        val parts = message.split("|")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }
}
