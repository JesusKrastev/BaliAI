package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.ui.theme.BaliTheme
import kotlin.math.exp
import kotlin.math.pow

@Composable
fun StepMethodComparison(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.surface

    val n = 60
    val baliPoints = remember {
        (0 until n).map { i ->
            val t = i / (n - 1.0)
            1.0 / (1.0 + exp(-(t - 0.4) * 9.0)) * (0.92 - 0.30) + 0.30
        }
    }

    val traditionalPoints = remember {
        (0 until n).map { i ->
            val t = i / (n - 1.0)
            1.0 / (1.0 + exp(-(t - 0.76) * 12.0)) * (0.70 - 0.30) + 0.30
        }
    }

    Column(modifier = modifier) {
        // Título
        Text(
            text = "Tu progreso de aprendizaje",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Área del gráfico con fondo oscuro redondeado
        Surface(
            color = surfaceColor,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    val chartLeft = 0f
                    val chartRight = size.width
                    val chartTop = 16.dp.toPx()
                    val chartBottom = size.height - 16.dp.toPx()

                    val valueToX: (Int) -> Float = { index ->
                        chartLeft + (index.toFloat() / (n - 1)) * (chartRight - chartLeft)
                    }
                    val valueToY: (Double) -> Float = { value ->
                        chartBottom - (value.toFloat() * (chartBottom - chartTop))
                    }

                    val baliLinePath = baliPoints.toSmoothPath(valueToX, valueToY)
                    val traditionalLinePath = traditionalPoints.toSmoothPath(valueToX, valueToY)

                    // Área rellena bajo Bali
                    val baliFillPath = Path().apply {
                        addPath(baliLinePath)
                        lineTo(chartRight, chartBottom)
                        lineTo(chartLeft, chartBottom)
                        close()
                    }
                    drawPath(
                        path = baliFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor.copy(alpha=0.3f), Color.Transparent),
                            startY = chartTop,
                            endY = chartBottom
                        )
                    )

                    // Área rellena bajo Tradicional
                    val tradFillPath = Path().apply {
                        addPath(traditionalLinePath)
                        lineTo(chartRight, chartBottom)
                        lineTo(chartLeft, chartBottom)
                        close()
                    }
                    drawPath(
                        path = tradFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(errorColor.copy(alpha = 0.2f), Color.Transparent),
                            startY = chartTop,
                            endY = chartBottom
                        )
                    )

                    // Líneas
                    drawPath(
                        path = baliLinePath,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = traditionalLinePath,
                        color = errorColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Círculos en los puntos finales
                    val endX = valueToX(n - 1)
                    val endYBali = valueToY(baliPoints.last())
                    drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = Offset(endX, endYBali))
                    drawCircle(color = surfaceColor, radius = 4.dp.toPx(), center = Offset(endX, endYBali))

                    val endYTrad = valueToY(traditionalPoints.last())
                    drawCircle(color = errorColor, radius = 6.dp.toPx(), center = Offset(endX, endYTrad))
                    drawCircle(color = surfaceColor, radius = 4.dp.toPx(), center = Offset(endX, endYTrad))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Semana 1", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Semana 6", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Leyenda
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = primaryColor, label = "Bali")
                    LegendItem(color = errorColor, label = "Método tradicional")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Estadística motivadora
                Text(
                    text = "El 86% de los alumnos que usan Bali aprueban a la primera incluso partiendo de cero",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

private fun List<Double>.toSmoothPath(valueToX: (Int) -> Float, valueToY: (Double) -> Float): Path {
    val path = Path()
    if (isEmpty()) return path
    path.moveTo(valueToX(0), valueToY(this[0]))
    for (i in 1 until size) {
        val x0 = valueToX(i - 1)
        val y0 = valueToY(this[i - 1])
        val x1 = valueToX(i)
        val y1 = valueToY(this[i])
        val cx = (x0 + x1) / 2f
        path.cubicTo(cx, y0, cx, y1, x1, y1)
    }
    return path
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}