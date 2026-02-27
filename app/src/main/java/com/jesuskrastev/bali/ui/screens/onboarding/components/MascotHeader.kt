package com.jesuskrastev.bali.ui.screens.onboarding.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MascotHeader(
    message: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val fullAnnotatedMessage = remember(message) { parseMascotMessage(message, primaryColor) }
    var displayedText by remember { mutableStateOf(AnnotatedString("")) }

    LaunchedEffect(fullAnnotatedMessage) {
        displayedText = AnnotatedString("")
        fullAnnotatedMessage.text.forEachIndexed { index, _ ->
            displayedText = fullAnnotatedMessage.subSequence(0, index + 1)
            delay(30)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        with(sharedTransitionScope) {
            Image(
                painter = painterResource(id = R.drawable.bali),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .padding(top = 8.dp)
                    .sharedElement(
                        rememberSharedContentState(key = "bali_mascot"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = SpeechBubbleShape(),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp)
        ) {
            Text(
                text = displayedText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 24.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
            )
        }
    }
}

private fun parseMascotMessage(message: String, primaryColor: Color): AnnotatedString {
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

private class SpeechBubbleShape(
    private val cornerRadius: Dp = 16.dp,
    private val pointerSize: Dp = 12.dp,
    private val pointerOffset: Dp = 16.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cornerRadiusPx = with(density) { cornerRadius.toPx() }
        val pointerSizePx = with(density) { pointerSize.toPx() }
        val pointerOffsetPx = with(density) { pointerOffset.toPx() }

        val path = Path().apply {
            moveTo(pointerSizePx + cornerRadiusPx, 0f)

            lineTo(size.width - cornerRadiusPx, 0f)
            arcTo(
                rect = Rect(size.width - 2 * cornerRadiusPx, 0f, size.width, 2 * cornerRadiusPx),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            lineTo(size.width, size.height - cornerRadiusPx)
            arcTo(
                rect = Rect(size.width - 2 * cornerRadiusPx, size.height - 2 * cornerRadiusPx, size.width, size.height),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            lineTo(pointerSizePx + cornerRadiusPx, size.height)
            arcTo(
                rect = Rect(pointerSizePx, size.height - 2 * cornerRadiusPx, pointerSizePx + 2 * cornerRadiusPx, size.height),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            lineTo(pointerSizePx, pointerOffsetPx + pointerSizePx)

            lineTo(0f, pointerOffsetPx + pointerSizePx / 2)
            lineTo(pointerSizePx, pointerOffsetPx)

            arcTo(
                rect = Rect(pointerSizePx, 0f, pointerSizePx + 2 * cornerRadiusPx, 2 * cornerRadiusPx),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )

            close()
        }

        return Outline.Generic(path)
    }
}
