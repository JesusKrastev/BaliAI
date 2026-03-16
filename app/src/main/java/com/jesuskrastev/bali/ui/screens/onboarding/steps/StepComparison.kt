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
    val baliValue = 0.89f // 89%
    val avgValue = 0.47f // 47%
    
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
                )

                // Gráfico de Barras con Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        
                        val barWidth = 60.dp.toPx()
                        val spacing = 80.dp.toPx()
                        
                        // Centrar las barras
                        val baliBarX = (canvasWidth / 2) - barWidth - (spacing / 2)
                        val avgBarX = (canvasWidth / 2) + (spacing / 2)

                        // Alturas visuales: Bali es más alta (89%), Promedio más baja (47%)
                        val baliFullHeight = canvasHeight * 0.85f
                        val avgFullHeight = canvasHeight * 0.45f

                        val baliCurrentHeight = baliFullHeight * animationProgress
                        val avgCurrentHeight = avgFullHeight * animationProgress

                        // Barra "Con Bali" (Izquierda) - Color de marca
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(baliBarX, canvasHeight - baliCurrentHeight),
                            size = Size(barWidth, baliCurrentHeight),
                            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                        )

                        // Barra "Promedio" (Derecha) - Color gris neutro
                        drawRoundRect(
                            color = Color(0xFF64748B),
                            topLeft = Offset(avgBarX, canvasHeight - avgCurrentHeight),
                            size = Size(barWidth, avgCurrentHeight),
                            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
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
                                "89%",
                                baliBarX + (barWidth / 2),
                                canvasHeight - baliCurrentHeight + 28.dp.toPx(),
                                textPaint
                            )

                            drawContext.canvas.nativeCanvas.drawText(
                                "47%",
                                avgBarX + (barWidth / 2),
                                canvasHeight - avgCurrentHeight + 28.dp.toPx(),
                                textPaint
                            )
                        }
                    }
                }

                // Etiquetas debajo de las barras
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.width(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Con Bali",
                            style = MaterialTheme.typography.labelSmall,
                            color = onCardColor.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.width(80.dp))
                    Box(
                        modifier = Modifier.width(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Promedio",
                            style = MaterialTheme.typography.labelSmall,
                            color = onCardColor.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
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
