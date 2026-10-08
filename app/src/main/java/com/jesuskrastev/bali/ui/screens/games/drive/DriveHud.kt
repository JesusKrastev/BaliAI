package com.jesuskrastev.bali.ui.screens.games.drive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import com.jesuskrastev.bali.ui.theme.BaliPrimary
import com.jesuskrastev.bali.ui.theme.BaliSecondary
import kotlin.math.roundToInt

/** Shows [progress] as a tiny road from the player's car to the finish flag; returns UI content. */
@Composable
internal fun DriveRouteProgress(progress: Float) {
    val fraction = progress.coerceIn(0f, 1f)
    val animatedFraction = animateFloatAsState(fraction, tween(220), label = "drive_route_progress").value
    val label = stringResource(R.string.drive_route_progress)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = BaliSecondary.copy(alpha = 0.93f),
        border = BorderStroke(1.dp, BaliAccentYellow.copy(alpha = 0.65f)),
        shadowElevation = 3.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp).semantics {
            contentDescription = label
            progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
        }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.drive_route_label), color = Color.White, fontSize = 10.sp)
                Text("${(fraction * 100).roundToInt()} %", color = BaliAccentYellow, fontSize = 10.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DriveIcon(DriveIconKind.CAR, Modifier.size(18.dp), BaliPrimary)
                Canvas(Modifier.weight(1f).height(16.dp)) {
                    val centerY = size.height / 2f
                    val roadHeight = 11.dp.toPx()
                    drawRoundRect(
                        DrivePalette.Road,
                        topLeft = Offset(0f, centerY - roadHeight / 2f),
                        size = Size(size.width, roadHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(roadHeight / 2f),
                    )
                    drawLine(
                        Color.White.copy(alpha = 0.48f),
                        Offset(0f, centerY),
                        Offset(size.width, centerY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 8.dp.toPx())),
                    )
                    val marker = size.width * animatedFraction
                    drawLine(BaliAccentYellow, Offset(0f, centerY), Offset(marker, centerY), 4.dp.toPx(), StrokeCap.Round)
                    drawCircle(BaliPrimary, 6.dp.toPx(), Offset(marker, centerY))
                    drawCircle(Color.White, 2.dp.toPx(), Offset(marker, centerY))
                }
                DriveIcon(DriveIconKind.FLAG, Modifier.size(18.dp), BaliAccentYellow)
            }
        }
    }
}
