package com.jesuskrastev.bali.ui.screens.games.drive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.ui.theme.BaliPrimary
import kotlin.math.cos
import kotlin.math.sin

/** Vector symbols drawn locally with the same rounded stroke and Bali palette. */
internal enum class DriveIconKind { STAR, POINTS, RECORD, CHECK, XP, COIN, TIME, ACCURACY, FAULT, CAR, FLAG, FINGER }

/** Draws a decorative [kind] in [color]; its adjoining text owns the accessible label. */
@Composable
internal fun DriveIcon(kind: DriveIconKind, modifier: Modifier = Modifier, color: Color = BaliPrimary) {
    Canvas(modifier.size(28.dp)) {
        scale(size.width / 32f, size.height / 32f, pivot = Offset.Zero) {
            val stroke = Stroke(2.5f, cap = StrokeCap.Round)
            when (kind) {
                DriveIconKind.STAR -> {
                    val path = Path()
                    for (i in 0 until 10) {
                        val a = -Math.PI / 2 + i * Math.PI / 5
                        val r = if (i % 2 == 0) 14f else 7f
                        val x = 16 + cos(a).toFloat() * r
                        val y = 16 + sin(a).toFloat() * r
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close(); drawPath(path, color)
                }
                DriveIconKind.XP -> {
                    val p = Path().apply { moveTo(18f, 2f); lineTo(7f, 18f); lineTo(15f, 18f); lineTo(13f, 30f); lineTo(25f, 13f); lineTo(17f, 13f); close() }
                    drawPath(p, color)
                }
                DriveIconKind.CAR -> {
                    drawRoundRect(color, Offset(8f, 2f), Size(16f, 28f), androidx.compose.ui.geometry.CornerRadius(5f))
                    drawRoundRect(Color.White, Offset(10f, 8f), Size(12f, 7f), androidx.compose.ui.geometry.CornerRadius(2f))
                }
                DriveIconKind.FLAG -> {
                    drawLine(color, Offset(5f, 30f), Offset(5f, 3f), 2.5f, StrokeCap.Round)
                    for (x in 0..3) for (y in 0..2) if ((x + y) % 2 == 0)
                        drawRect(color, Offset(6f + x * 5f, 3f + y * 5f), Size(5f, 5f))
                }
                DriveIconKind.FINGER -> {
                    val p = Path().apply { moveTo(12f, 27f); lineTo(5f, 18f); cubicTo(3f, 14f, 7f, 13f, 11f, 18f); lineTo(11f, 5f); cubicTo(11f, 0f, 17f, 0f, 17f, 5f); lineTo(17f, 13f); cubicTo(27f, 10f, 29f, 16f, 26f, 27f); close() }
                    drawPath(p, color); drawPath(p, Color.White, style = stroke)
                }
                DriveIconKind.RECORD -> {
                    drawArc(color, 0f, 180f, false, Offset(6f, 3f), Size(20f, 20f), style = stroke)
                    drawLine(color, Offset(16f, 14f), Offset(16f, 27f), 3f, StrokeCap.Round)
                    drawLine(color, Offset(8f, 28f), Offset(24f, 28f), 3f, StrokeCap.Round)
                    drawArc(color, 0f, 180f, false, Offset(1f, 4f), Size(30f, 17f), style = stroke)
                }
                else -> {
                    drawCircle(color.copy(alpha = 0.14f), 14f, Offset(16f, 16f))
                    drawCircle(color, 13f, Offset(16f, 16f), style = stroke)
                    when (kind) {
                        DriveIconKind.CHECK -> { drawLine(color, Offset(8f, 16f), Offset(14f, 22f), 3f, StrokeCap.Round); drawLine(color, Offset(14f, 22f), Offset(24f, 10f), 3f, StrokeCap.Round) }
                        DriveIconKind.TIME -> { drawLine(color, Offset(16f, 8f), Offset(16f, 16f), 3f, StrokeCap.Round); drawLine(color, Offset(16f, 16f), Offset(22f, 19f), 3f, StrokeCap.Round) }
                        DriveIconKind.ACCURACY -> { drawCircle(color, 7f, Offset(16f, 16f), style = stroke); drawCircle(color, 2f, Offset(16f, 16f)) }
                        DriveIconKind.FAULT -> { drawLine(color, Offset(16f, 8f), Offset(16f, 18f), 3f, StrokeCap.Round); drawCircle(color, 1.5f, Offset(16f, 23f)) }
                        DriveIconKind.COIN -> drawRoundRect(color, Offset(10f, 9f), Size(12f, 14f), androidx.compose.ui.geometry.CornerRadius(3f), style = stroke)
                        else -> { drawLine(color, Offset(9f, 16f), Offset(23f, 16f), 3f, StrokeCap.Round); drawLine(color, Offset(16f, 9f), Offset(16f, 23f), 3f, StrokeCap.Round) }
                    }
                }
            }
        }
    }
}
