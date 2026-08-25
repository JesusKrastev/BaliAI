package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingConfig
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingData
import com.jesuskrastev.bali.ui.screens.onboarding.components.highlightPipes
import com.jesuskrastev.bali.ui.theme.BaliTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val BALI_PASS_RATE = 0.89f

/**
 * The complement of the 58% failure rate the title leads with. The two numbers are on
 * screen at the same time, so they have to add up to 100 or the chart contradicts the
 * headline sitting right above it.
 */
private const val AVERAGE_PASS_RATE = 0.42f

/** Height of a bar at 100%. Both bars are measured against this, so their ratio stays honest. */
private val BAR_FULL_HEIGHT = 200.dp

private val BAR_WIDTH = 82.dp

/** The losing bar rises first so the winning one lands last and holds the attention. */
private const val LOSING_BAR_DELAY_MS = 200L
private const val WINNING_BAR_DELAY_MS = 550L

/** Bouncy enough to feel alive, damped enough that the overshoot never looks like a glitch. */
private val BAR_SPRING = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessLow
)

/**
 * Post-processing screen that turns the collected profile into the single number the user
 * cares about: their odds of passing on the first attempt.
 *
 * It talks about the *result*, not about the method — the learning curve earlier in the flow
 * already made the case for the method. The step hides the mascot bubble and carries its own
 * title so the chart gets the full height.
 *
 * @param data the answers collected during the onboarding flow, used to personalise the headline
 */
@Composable
fun StepComparison(data: OnboardingData) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val losingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

    val winningProgress = remember { Animatable(0f) }
    val losingProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            delay(LOSING_BAR_DELAY_MS)
            losingProgress.animateTo(1f, BAR_SPRING)
        }
        launch {
            delay(WINNING_BAR_DELAY_MS)
            winningProgress.animateTo(1f, BAR_SPRING)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = buildTitle(data).highlightPipes(primaryColor),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Calculada con tus respuestas y con los datos de alumnos con tu mismo perfil",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom
        ) {
            ProbabilityBar(
                rate = BALI_PASS_RATE,
                progress = { winningProgress.value },
                label = "Con Bali AI",
                barBrush = Brush.verticalGradient(
                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.72f))
                ),
                valueColor = primaryColor,
                glowColor = primaryColor
            )
            ProbabilityBar(
                rate = AVERAGE_PASS_RATE,
                progress = { losingProgress.value },
                label = "Sin Bali AI",
                barBrush = Brush.verticalGradient(
                    colors = listOf(losingColor, losingColor.copy(alpha = 0.55f))
                ),
                valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                glowColor = Color.Transparent
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = buildHeadline(data).highlightPipes(primaryColor),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

/**
 * One bar of the comparison, with its percentage counting up as it grows and its caption
 * underneath.
 *
 * The bar is a laid-out box rather than a canvas drawing, which is what lets the percentage
 * and the caption be real text — correctly sized, themed and accessible — instead of glyphs
 * painted at hand-computed coordinates.
 *
 * @param rate final value of the bar, between 0f and 1f
 * @param progress lambda returning the entrance progress, which may overshoot above 1f
 * @param label caption shown under the bar
 * @param barBrush fill of the bar
 * @param valueColor colour of the percentage above the bar
 * @param glowColor colour of the halo behind the bar; pass [Color.Transparent] for no halo
 */
@Composable
private fun ProbabilityBar(
    rate: Float,
    progress: () -> Float,
    label: String,
    barBrush: Brush,
    valueColor: Color,
    glowColor: Color
) {
    // The spring overshoots past 1f, which gives the bar its bounce but would make the
    // percentage tick above its real value, so the number is clamped and the bar is not.
    val raw = progress()
    val barFraction = raw.coerceAtLeast(0f)
    val displayedPercentage = (rate * 100f * raw.coerceIn(0f, 1f)).roundToInt()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$displayedPercentage%",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(contentAlignment = Alignment.BottomCenter) {
            if (glowColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .width(BAR_WIDTH + 28.dp)
                        .height(BAR_FULL_HEIGHT * rate * barFraction)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(glowColor.copy(alpha = 0.22f), Color.Transparent)
                            ),
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        )
                )
            }

            Box(
                modifier = Modifier
                    .width(BAR_WIDTH)
                    .height(BAR_FULL_HEIGHT * rate * barFraction)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(barBrush)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Picks the title, which answers back the attempt the user just told us about.
 *
 * Both variants lead with the same failure rate the chart is drawn from, so the screen
 * reads as a reaction to their answer rather than as a generic statistic: a repeat
 * candidate is told they already know what it feels like, a first-timer is told it does
 * not have to happen to them.
 *
 * @param data the answers collected during the onboarding flow
 * @return the title, with the words to highlight wrapped in pipes
 */
private fun buildTitle(data: OnboardingData): String = when (data.experience) {
    OnboardingConfig.EXPERIENCE_FIRST_TIME ->
        "El |58%| suspende a la primera. No tiene que ser tu caso."
    OnboardingConfig.EXPERIENCE_RETRY ->
        "Ya sabes lo que se siente. El |58%| suspende a la primera, y tú no vas a repetir."
    else -> "Tu probabilidad de aprobar |a la primera|"
}

/**
 * Picks the closing line under the chart, which turns the title's statistic into what the
 * user's own plan does about it.
 *
 * @param data the answers collected during the onboarding flow
 * @return the closing line, with the words to highlight wrapped in pipes
 */
private fun buildHeadline(data: OnboardingData): String = when (data.experience) {
    OnboardingConfig.EXPERIENCE_FIRST_TIME ->
        "Llegar al examen con un método desde el día uno es tu |mayor ventaja|."
    OnboardingConfig.EXPERIENCE_RETRY ->
        "Esta vez |es la definitiva|: atacamos justo donde fallaste."
    else -> "Tu perfil encaja con el de los alumnos que |aprueban a la primera|."
}

@Preview(showBackground = true)
@Composable
private fun StepComparisonPreview() {
    BaliTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            StepComparison(
                data = OnboardingData(
                    name = "Jesús",
                    experience = OnboardingConfig.EXPERIENCE_FIRST_TIME
                )
            )
        }
    }
}
