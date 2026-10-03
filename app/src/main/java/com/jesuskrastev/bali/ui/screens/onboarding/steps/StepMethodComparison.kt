package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingData
import com.jesuskrastev.bali.ui.theme.BaliTheme
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Samples taken from each curve. Both curves come from continuous functions, so at this
 * density the straight segment between two samples is well under a pixel and the stroke
 * reads as a genuinely smooth line without any spline fitting.
 */
private const val CURVE_SAMPLES = 160

/** Number of study-then-fail cycles drawn on the solo curve. */
private const val FAILED_ATTEMPTS = 3

/** Top of the value scale. Both curves stay clear of it so the chart never feels cramped. */
private const val CHART_MAX = 100f

/** Steady climb: a gentle sigmoid — with a method, every week compounds on the previous one. */
private fun baliProgressAt(t: Float): Float = 8f + 88f / (1f + exp(-(t - 0.5f) * 4.4f))

/** Value both curves share at t = 0, so the two stories visibly start from the same place. */
private val START_VALUE = baliProgressAt(0f)

/** Height of each study-then-fail bounce, in chart units. */
private const val BOUNCE_AMPLITUDE = 6.5f

/** How far the solo baseline sinks over the whole chart, in chart units. */
private const val BOUNCE_DECAY = 5f

/**
 * Bouncing ball: effort builds up and collapses [FAILED_ATTEMPTS] times over a slowly
 * sinking baseline, so the last attempt ends lower than the first one started.
 *
 * @param t position along the chart, 0f at the left edge and 1f at the right edge
 * @return the value of the solo curve at [t]
 */
private fun soloProgressAt(t: Float): Float =
    (START_VALUE + BOUNCE_AMPLITUDE) -
        BOUNCE_DECAY * t -
        BOUNCE_AMPLITUDE * cos(2f * PI.toFloat() * FAILED_ATTEMPTS * t)

/**
 * Where the solo curve peaks and collapses — one failed attempt each.
 * The cosine peaks exactly at these positions, so the crosses land dead on the line.
 */
private val FAILURE_POINTS = List(FAILED_ATTEMPTS) { attempt ->
    (2 * attempt + 1) / (2f * FAILED_ATTEMPTS)
}

private val CHART_HEIGHT = 260.dp

/** Both strokes are drawn left to right in this window. */
private const val CURVE_DRAW_MS = 1700

/** The crosses and the end marker land as the strokes are finishing. */
private const val ACCENT_DRAW_MS = 1100
private const val ACCENT_DELAY_MS = CURVE_DRAW_MS - 350

/**
 * The way out, right after the mini-test result: from the user's score today to the 27 of 30
 * the exam asks for.
 *
 * Two curves tell the story without a word — Bali climbing steadily, and going it alone
 * bouncing up and crashing three times, each crash marked with an X. The step hides the
 * mascot bubble and carries its own title so the chart gets the full height.
 *
 * @param data the answers collected so far, whose mini-test score opens the road
 * @param modifier modifier applied to the root container
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepMethodComparison(data: OnboardingData, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val failureColor = MaterialTheme.colorScheme.error
    val backgroundColor = MaterialTheme.colorScheme.background

    val curveProgress = remember { Animatable(0f) }
    val accentProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            curveProgress.animateTo(1f, tween(CURVE_DRAW_MS, easing = EaseInOutCubic))
        }
        // Linear on purpose: the per-cross stagger below does the easing, and an even
        // timeline keeps the gap between the three pops identical.
        accentProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(ACCENT_DRAW_MS, delayMillis = ACCENT_DELAY_MS, easing = LinearEasing)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tu camino hasta el aprobado",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Con Bali AI, aprobar a la primera es posible",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        FreedomChart(
            curveProgress = { curveProgress.value },
            accentProgress = { accentProgress.value },
            winningColor = primaryColor,
            failureColor = failureColor,
            ringColor = backgroundColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT)
        )

        Spacer(modifier = Modifier.height(20.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LegendItem(color = primaryColor, label = "Con Bali AI")
            LegendItem(color = failureColor, label = "Sin Bali AI")
            LegendItem(color = failureColor, label = "Suspensos", isCross = true)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Where the user starts, from their own mini-test, and where the exam needs them: the
        // curve above is the road between the two. It replaced an unsourced "+1000 personas".
        Text(
            text = if (data.quizAnswers.isEmpty()) "${ExamRules.PASS_SCORE} de ${ExamRules.QUESTION_COUNT}"
            else "${data.quizScore()} de ${data.quizAnswers.size} → ${ExamRules.PASS_SCORE} de ${ExamRules.QUESTION_COUNT}",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = primaryColor,
            textAlign = TextAlign.Center
        )

        Text(
            text = if (data.quizAnswers.isEmpty()) {
                "Es lo que necesitas en el examen. Tu plan te lleva hasta ahí, tema a tema."
            } else {
                "De tu prueba de hoy a lo que pide el examen. Tu plan te lleva hasta ahí, tema a tema."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )
    }
}

/**
 * The chart itself: the winning curve with its gradient area and end marker, the solo curve
 * bouncing underneath, and one cross per failed attempt.
 *
 * Everything is drawn by hand in a single coordinate system, which is what guarantees the
 * crosses sit exactly on the solo curve instead of being positioned by a second formula.
 * Path building happens in [drawWithCache] so it only re-runs when the size changes; the
 * animation values are read inside the draw block and therefore only trigger a repaint.
 *
 * @param curveProgress lambda returning how much of both strokes is revealed, 0f to 1f
 * @param accentProgress lambda returning the entrance progress of the crosses and end marker
 * @param winningColor colour of the Bali curve, its area and its end marker
 * @param failureColor colour of the solo curve and of the crosses
 * @param ringColor colour of the ring punched out of the end marker, normally the background
 * @param modifier modifier applied to the canvas
 */
@Composable
private fun FreedomChart(
    curveProgress: () -> Float,
    accentProgress: () -> Float,
    winningColor: Color,
    failureColor: Color,
    ringColor: Color,
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val plot = Plot(size, this)

            val winningPath = plot.curvePath(::baliProgressAt)
            val soloPath = plot.curvePath(::soloProgressAt)
            val endPoint = plot.offsetAt(1f, baliProgressAt(1f))
            val failurePoints = FAILURE_POINTS.map { plot.offsetAt(it, soloProgressAt(it)) }

            // The area closes straight down from the last point, so the fill ends on a
            // clean vertical edge under the marker instead of tapering away.
            val areaPath = Path().apply {
                addPath(winningPath)
                lineTo(endPoint.x, plot.bottom)
                lineTo(plot.left, plot.bottom)
                close()
            }

            val areaBrush = Brush.verticalGradient(
                colors = listOf(
                    winningColor.copy(alpha = 0.28f),
                    winningColor.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                startY = endPoint.y,
                endY = plot.bottom
            )

            onDrawBehind {
                val revealed = curveProgress()
                val accents = accentProgress()

                // Both curves advance strictly left to right, so clipping the canvas is
                // equivalent to walking the paths — and costs nothing per frame.
                clipRect(right = plot.left + (plot.right - plot.left) * revealed) {
                    drawPath(path = areaPath, brush = areaBrush)

                    drawPath(
                        path = soloPath,
                        color = failureColor,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Drawn last so the winning curve reads as continuous where the two cross.
                    drawPath(
                        path = winningPath,
                        color = winningColor,
                        style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                drawFailureMarks(failurePoints, accents, failureColor)
                drawEndMarker(endPoint, accents, winningColor, ringColor)
            }
        }
    )
}

/**
 * Maps curve space — position `t` in 0f..1f and value in 0f..[CHART_MAX] — onto the canvas.
 *
 * The insets are asymmetric on purpose: the top and the right edge have to hold the halo of
 * the end marker, while the left edge only needs to clear half a stroke.
 *
 * @param size size of the canvas being drawn into
 * @param density density used to convert the insets from dp to pixels
 */
private class Plot(size: Size, density: Density) {
    val left = with(density) { 4.dp.toPx() }
    val top = with(density) { 20.dp.toPx() }
    val right = size.width - with(density) { 20.dp.toPx() }
    val bottom = size.height - with(density) { 6.dp.toPx() }

    /**
     * Converts a point of a curve into canvas coordinates.
     *
     * @param t position along the chart, 0f at the left edge and 1f at the right edge
     * @param value value of the curve, on the 0f..[CHART_MAX] scale
     * @return the matching canvas offset
     */
    fun offsetAt(t: Float, value: Float): Offset = Offset(
        x = left + (right - left) * t,
        y = bottom - (bottom - top) * (value / CHART_MAX)
    )

    /**
     * Samples a curve function into a path spanning the full width of the plot.
     *
     * @param curve function giving the value of the curve at a position `t`
     * @return the sampled path
     */
    fun curvePath(curve: (Float) -> Float): Path = Path().apply {
        repeat(CURVE_SAMPLES) { index ->
            val t = index / (CURVE_SAMPLES - 1f)
            val point = offsetAt(t, curve(t))
            if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
    }
}

/**
 * Draws one cross per failed attempt, each popping in slightly after the previous one.
 *
 * The crosses sit straight on the solo curve with nothing behind them: a disc in the
 * background colour would read as a black blob on a dark theme, and the arms are thick
 * enough to stand out from the line on their own.
 *
 * @param points canvas positions of the failed attempts, in order
 * @param progress overall entrance progress of the crosses, 0f to 1f
 * @param color colour of the crosses
 */
private fun DrawScope.drawFailureMarks(
    points: List<Offset>,
    progress: Float,
    color: Color
) {
    points.forEachIndexed { order, point ->
        val local = ((progress - order * 0.2f) / 0.6f).coerceIn(0f, 1f)
        if (local <= 0f) return@forEachIndexed

        // Ease-out with a slight overshoot so each cross lands with a pop.
        val scale = (1f - (1f - local) * (1f - local)) * (1f + 0.25f * sin(local * PI.toFloat()))
        val arm = 8.dp.toPx() * scale

        val strokeWidth = 4.dp.toPx()
        drawLine(
            color = color,
            start = Offset(point.x - arm, point.y - arm),
            end = Offset(point.x + arm, point.y + arm),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(point.x + arm, point.y - arm),
            end = Offset(point.x - arm, point.y + arm),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Draws the marker that closes the winning curve: a soft halo, a ring punched out of the
 * background and a solid core. It is the visual full stop of the story the chart tells.
 *
 * @param point canvas position of the last point of the winning curve
 * @param progress entrance progress of the marker, 0f to 1f
 * @param color colour of the halo and of the core
 * @param ringColor colour of the ring separating the core from the halo
 */
private fun DrawScope.drawEndMarker(
    point: Offset,
    progress: Float,
    color: Color,
    ringColor: Color
) {
    if (progress <= 0f) return

    val scale = 1f - (1f - progress) * (1f - progress)

    drawCircle(color = color.copy(alpha = 0.18f * progress), radius = 15.dp.toPx() * scale, center = point)
    drawCircle(color = ringColor, radius = 9.dp.toPx() * scale, center = point)
    drawCircle(color = color, radius = 5.5.dp.toPx() * scale, center = point)
}

/**
 * One entry of the chart legend.
 *
 * @param color colour of the swatch
 * @param label text describing the series
 * @param isCross true to draw an X swatch (the failed attempts) instead of a line swatch
 */
@Composable
private fun LegendItem(color: Color, label: String, isCross: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isCross) {
            Canvas(modifier = Modifier.size(12.dp)) {
                val strokeWidth = 2.5.dp.toPx()
                drawLine(color, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth, StrokeCap.Round)
            }
        } else {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(4.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StepMethodComparisonPreview() {
    BaliTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            StepMethodComparison(data = OnboardingData(), modifier = Modifier.padding(24.dp))
        }
    }
}
