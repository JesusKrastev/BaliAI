package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingConfig
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingData
import com.jesuskrastev.bali.ui.screens.onboarding.Testimonial
import com.jesuskrastev.bali.ui.screens.onboarding.components.highlightPipes
import com.jesuskrastev.bali.ui.screens.onboarding.optionLabel
import com.jesuskrastev.bali.ui.theme.BaliTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val SUCCESS_GREEN = Color(0xFF10B981)
private val FAILURE_RED = Color(0xFFF44336)

/** Weeks the plan needs when there is no booked exam to aim at, by study rhythm. */
private const val WEEKS_DAILY = 3
private const val WEEKS_OFTEN = 5
private const val WEEKS_WHENEVER = 8

/**
 * Reveals the personalised plan, right before the product preview and the pact.
 *
 * It is deliberately a long scroll: it promises a date, shows the life behind that date,
 * explains how it will be reached, contrasts it with doing nothing and closes with proof
 * that other people already did it. Every block answers something the user typed earlier,
 * so the page reads as *their* plan rather than as a feature list.
 *
 * @param data the answers collected during the onboarding flow
 */
@Composable
fun StepPlanReveal(data: OnboardingData) {
    val targetDate = remember(data) { planTargetDate(data) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlanHero(name = data.name, targetDate = targetDate)

        SectionDivider()

        LifeChangeSection()

        SectionDivider()

        OnboardingConfig.testimonials.getOrNull(OnboardingConfig.SOCIAL_PROOF_TESTIMONIALS)
            ?.let { QuoteBlock(it) }

        SectionDivider()

        HowSection(data)

        SectionDivider()

        WhyBaliSection()

        SectionDivider()

        ClosingProof()
    }
}

/**
 * Announces the plan and puts a date on it, which is the one thing the user is buying.
 *
 * @param name the user's name, or null if it was skipped
 * @param targetDate the day the plan aims to have the licence in hand
 */
@Composable
private fun PlanHero(name: String?, targetDate: Date) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = name?.let { "$it, tu plan está listo" } ?: "Tu plan está listo",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "Puedes tener tu carnet antes del:",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(10.dp))

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = longDateLabel(targetDate),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.car))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.size(200.dp)
    )
}

/** What the licence actually buys, in the same terms the emotional arc used earlier. */
@Composable
private fun LifeChangeSection() {
    SectionTitle("Esto es lo que cambiará en tu vida")

    Spacer(modifier = Modifier.height(16.dp))

    val changes = listOf(
        "💪" to "Dejarás de |depender de los demás|",
        "🕊️" to "Ganarás |independencia total|",
        "🌍" to "|Viajarás| a donde quieras, cuando quieras",
        "⏱️" to "|No perderás más tiempo| esperando el autobús"
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        changes.forEach { (emoji, text) -> BulletRow(emoji = emoji, text = text) }
    }
}

/**
 * How the plan gets there, using the method the user said they preferred.
 *
 * @param data the answers collected during the onboarding flow
 */
@Composable
private fun HowSection(data: OnboardingData) {
    SectionTitle("Cómo lo vas a conseguir")

    Spacer(modifier = Modifier.height(16.dp))

    val steps = buildList {
        add(
            "🎯" to (data.learningPreference
                ?.let { optionLabel(it) }
                ?: "Tests adaptados a tus fallos")
        )
        add("🤖" to "Pregúntale a Bali cualquier duda del teórico, a cualquier hora")
        add("📚" to "Repasa la teoría de cada tema, explicada en corto")
        add(
            "🎓" to (data.province
                ?.let { "Haz simulacros con las preguntas que se usan en $it" }
                ?: "Haz simulacros con las preguntas oficiales de la DGT")
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        steps.forEach { (emoji, text) -> StepCard(emoji = emoji, text = text) }
    }
}

/** The contrast block: what the next weeks look like with and without the app. */
@Composable
private fun WhyBaliSection() {
    SectionTitle("¿Por qué Bali?")

    Spacer(modifier = Modifier.height(16.dp))

    ContrastCard(
        title = "Sin Bali",
        accent = FAILURE_RED,
        isPositive = false,
        items = listOf(
            "Leer el manual entero sin saber qué entra",
            "Estudiar sin saber si vas por buen camino",
            "Suspender y volver a pagar la tasa de la DGT"
        )
    )

    Spacer(modifier = Modifier.height(12.dp))

    ContrastCard(
        title = "Con Bali",
        accent = SUCCESS_GREEN,
        isPositive = true,
        items = listOf(
            "Un método probado por +${OnboardingConfig.USERS_HELPED} alumnos",
            "Un profesor de teórico con IA disponible 24/7",
            "Practicarás como si fuera el examen real"
        )
    )
}

/** Closing proof: the headline number plus the reviews the social proof screen did not use. */
@Composable
private fun ClosingProof() {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "🌿", fontSize = 26.sp)
        Spacer(modifier = Modifier.width(8.dp))
        repeat(5) { Text(text = "⭐", fontSize = 20.sp) }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "🌿", fontSize = 26.sp)
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "+${OnboardingConfig.USERS_HELPED} personas ya tienen su carnet gracias a Bali",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = OnboardingConfig.STORE_RATING,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    val remaining = OnboardingConfig.testimonials
        .drop(OnboardingConfig.SOCIAL_PROOF_TESTIMONIALS + 1)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        remaining.forEach { ReviewCard(it) }
    }
}

/**
 * Title of one block of the page.
 *
 * @param text the title copy
 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Short rule that separates the blocks, so the long scroll reads as chapters. */
@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(28.dp))
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(4.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(2.dp)
            )
    )
    Spacer(modifier = Modifier.height(28.dp))
}

/**
 * One line of the "what will change" list.
 *
 * @param emoji symbol shown on the left
 * @param text the line, with the words to emphasise wrapped in pipes
 */
@Composable
private fun BulletRow(emoji: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text.highlightPipes(MaterialTheme.colorScheme.primary),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * One card of the "how you will get there" list.
 *
 * @param emoji symbol shown on the left
 * @param text what the user will be doing
 */
@Composable
private fun StepCard(emoji: String, text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * One half of the with/without comparison.
 *
 * @param title heading of the card
 * @param accent colour of the icons and the heading
 * @param isPositive true for the "with Bali" card, which uses ticks instead of crosses
 * @param items the lines of the card
 */
@Composable
private fun ContrastCard(
    title: String,
    accent: Color,
    isPositive: Boolean,
    items: List<String>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )

            Spacer(modifier = Modifier.height(12.dp))

            items.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * The single pulled-out quote that sits between the promise and the method.
 *
 * @param testimonial the review to show
 */
@Composable
private fun QuoteBlock(testimonial: Testimonial) {
    Row(horizontalArrangement = Arrangement.Center) {
        repeat(5) { Text(text = "⭐", fontSize = 18.sp) }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "\"${testimonial.body}\"",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
        text = "— ${testimonial.name}",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

/**
 * A review rendered as a card, for the closing block.
 *
 * @param testimonial the review to show
 */
@Composable
private fun ReviewCard(testimonial: Testimonial) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = testimonial.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                repeat(5) { Text(text = "⭐", fontSize = 12.sp) }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = testimonial.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Works out the day the plan aims at.
 *
 * A booked exam is the honest answer. Without one the date is derived from the rhythm the
 * user committed to, which keeps the promise personal instead of inventing a deadline.
 *
 * @param data the answers collected during the onboarding flow
 * @return the target date shown in the hero
 */
private fun planTargetDate(data: OnboardingData): Date {
    data.examDate
        ?.takeIf { it > System.currentTimeMillis() }
        ?.let { return Date(it) }

    val weeks = when (data.weeklyStudy) {
        OnboardingConfig.WEEKLY_STUDY_DAILY -> WEEKS_DAILY
        OnboardingConfig.WEEKLY_STUDY_OFTEN -> WEEKS_OFTEN
        else -> WEEKS_WHENEVER
    }

    return Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, weeks) }.time
}

/**
 * Formats the target date the way the promise reads out loud.
 *
 * @param date the target date
 * @return a label such as "2 de octubre de 2026"
 */
private fun longDateLabel(date: Date): String =
    SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "ES")).format(date)

@Preview(showBackground = true)
@Composable
private fun StepPlanRevealPreview() {
    BaliTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
        ) {
            StepPlanReveal(
                data = OnboardingData(
                    name = "Jesús",
                    motivation = OnboardingConfig.MOTIVATION_WORK,
                    theoryBlocker = OnboardingConfig.BLOCKER_NO_METHOD,
                    weeklyStudy = OnboardingConfig.WEEKLY_STUDY_DAILY,
                    learningPreference = OnboardingConfig.learningPreferences.first(),
                    province = "Almería",
                    examDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(21)
                )
            )
        }
    }
}
