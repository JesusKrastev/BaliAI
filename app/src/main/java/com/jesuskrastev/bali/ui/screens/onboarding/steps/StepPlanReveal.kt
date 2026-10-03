package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.ui.screens.onboarding.NotificationsAnswer
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingConfig
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingData
import com.jesuskrastev.bali.ui.screens.onboarding.PlanBlock
import com.jesuskrastev.bali.ui.screens.onboarding.StudyPlan
import com.jesuskrastev.bali.ui.screens.onboarding.StudyPlanBuilder
import com.jesuskrastev.bali.ui.screens.onboarding.Testimonial
import com.jesuskrastev.bali.ui.screens.onboarding.components.highlightPipes
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import com.jesuskrastev.bali.ui.theme.BaliTheme
import com.jesuskrastev.bali.ui.util.replayMask
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val SPANISH = Locale("es", "ES")

/** Width of a phone in the "así vas a estudiar" carousel. */
private val SHOT_WIDTH = 176.dp

/**
 * Reveals the plan built from the user's answers, right before the social proof and the pact.
 *
 * The top fits on one screen: the date, the daily rhythm and the user's own answers as chips, so
 * the page is personal before any scroll. Below, the plan week by week, each stretch a card that
 * opens on tap — the first one open, the rest inviting a look at what comes — with the topics the
 * mini-test caught flagged where they come up. Then real screens of the app, the user's favourite
 * way of practising first, and the comparison and proof that close it.
 *
 * @param data the answers collected during the onboarding flow
 * @param modifier modifier applied to the scrolling column
 */
@Composable
fun StepPlanReveal(data: OnboardingData, modifier: Modifier = Modifier) {
    val targetMillis = remember(data) { OnboardingConfig.planTargetMillis(data.examDate, data.weeklyStudy) }
    val plan = remember(data) {
        StudyPlanBuilder.build(
            now = System.currentTimeMillis(),
            targetMillis = targetMillis,
            failed = data.failedQuizQuestions()
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlanHero(name = data.name, targetDate = Date(targetMillis))

        Spacer(modifier = Modifier.height(16.dp))

        ProfileChips(data)

        SectionDivider()

        SectionTitle("Tu plan semana a semana")
        Text(
            text = "Toca cada etapa para ver qué toca",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )
        PlanTimeline(plan = plan, examDate = data.examDate)

        SectionDivider()

        SectionTitle("Así vas a estudiar")
        Text(
            text = "Pantallas reales de Bali",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )
        ShowcaseCarousel(favouriteKey = data.learningPreference)

        SectionDivider()

        OnboardingConfig.testimonials.getOrNull(OnboardingConfig.SOCIAL_PROOF_TESTIMONIALS)
            ?.let { QuoteBlock(it) }

        SectionDivider()

        WhyBaliSection()

        SectionDivider()

        ClosingProof()
    }
}

/**
 * Announces the plan and puts a date on it, which is the one thing the user is buying, with the
 * daily rhythm that gets there right under it.
 *
 * @param name the user's name, or null if it was skipped
 * @param targetDate the day the plan aims to have the theory exam passed by
 */
@Composable
private fun PlanHero(name: String?, targetDate: Date) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
        text = name?.let { "$it, tu plan está listo" } ?: "Tu plan está listo",
        modifier = Modifier.replayMask(enabled = !name.isNullOrBlank()),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Puedes aprobar el teórico antes del:",
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
            text = SimpleDateFormat("d 'de' MMMM 'de' yyyy", SPANISH).format(targetDate),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
        text = ("|${StudyPlanBuilder.DAILY_QUESTIONS} preguntas al día| y " +
            "|${StudyPlanBuilder.WEEKLY_MOCK_EXAMS} simulacros por semana|")
            .highlightPipes(MaterialTheme.colorScheme.primary),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
}

/**
 * The user's own answers, as chips under the date: proof at a glance that the plan is theirs.
 *
 * @param data the answers collected during the onboarding flow
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileChips(data: OnboardingData) {
    val chips = buildList {
        add(data.examDate?.let { "📅 Examen: ${shortDate(it)}" } ?: "📝 Sin fecha aún")
        OnboardingConfig.weeklyStudyShortLabel(data.weeklyStudy)?.let { add("⏱️ $it") }
        if (data.notifications == NotificationsAnswer.GRANTED) {
            data.studySlot()?.let { add("🔔 Aviso a las ${OnboardingConfig.reminderTimeLabel(it)}") }
        }
        OnboardingConfig.learningStyle(data.learningPreference)?.let { add("${it.emoji} ${it.label}") }
        data.province?.let { add("📍 $it") }
        data.failedQuizQuestions().map { it.topic }.distinct().takeIf { it.isNotEmpty() }
            ?.let { add("🎯 Reforzar: ${it.joinToString(", ")}") }
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        chips.forEach { chip ->
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Text(
                    text = chip,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

/**
 * The plan as a vertical timeline of stretches. The first one starts open; the others show a
 * one-line summary and open on tap.
 *
 * @param plan the plan to draw
 * @param examDate the exam day, named on the final stretch, or null when there is none
 */
@Composable
private fun PlanTimeline(plan: StudyPlan, examDate: Long?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        plan.blocks.forEachIndexed { index, block ->
            PlanBlockCard(
                block = block,
                isFirst = index == 0,
                isDayBased = block.title.startsWith("Día"),
                examDate = examDate
            )
        }
    }
}

/**
 * One stretch of the plan: its name and dates, and what it holds once opened.
 *
 * @param block the stretch
 * @param isFirst whether it is the first one, which starts open and is marked "Empiezas hoy"
 * @param isDayBased whether the plan is told in days, which changes the mock exam line
 * @param examDate the exam day, named on the final stretch, or null when there is none
 */
@Composable
private fun PlanBlockCard(block: PlanBlock, isFirst: Boolean, isDayBased: Boolean, examDate: Long?) {
    var expanded by rememberSaveable(block.title) { mutableStateOf(isFirst) }
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "plan_block_arrow")
    val accent = if (block.isFinalStretch) BaliAccentGreen else MaterialTheme.colorScheme.primary

    Surface(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (expanded) 2.dp else 1.dp, if (expanded) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(accent, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (block.isFinalStretch) "${block.title} · Recta final" else block.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateRange(block.startMillis, block.endMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isFirst) {
                    Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.14f)) {
                        Text(
                            text = "Empiezas hoy",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Cerrar" else "Abrir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(arrowRotation)
                )
            }

            if (!expanded) {
                Text(
                    text = blockTeaser(block),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, top = 8.dp)
                )
            } else {
                Column(
                    modifier = Modifier.padding(start = 24.dp, top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    blockLines(block, isDayBased, examDate).forEach { (emoji, text) ->
                        PlanLine(emoji = emoji, text = text)
                    }
                }
            }
        }
    }
}

/**
 * The one line a closed stretch shows, enough to want to open it.
 *
 * @param block the stretch
 * @return the summary
 */
private fun blockTeaser(block: PlanBlock): String {
    if (block.isFinalStretch) return "Simulacros completos y repaso final"
    val first = block.sections.first().substringBefore(':')
    val others = block.sections.size - 1
    val topics = if (others == 0) first else "$first y $others ${if (others == 1) "tema" else "temas"} más"
    return if (block.reinforce.isEmpty()) topics else "$topics · refuerzas ${block.reinforce.joinToString(" y ")}"
}

/**
 * The lines of an open stretch: its sections, the topics to reinforce and the daily work; the
 * final stretch is mock exams and review, ending on the exam day or on the readiness check.
 *
 * @param block the stretch
 * @param isDayBased whether the plan is told in days
 * @param examDate the exam day, or null when there is none
 * @return each line as emoji and text, `|` pairs marking the words to highlight
 */
private fun blockLines(block: PlanBlock, isDayBased: Boolean, examDate: Long?): List<Pair<String, String>> =
    buildList {
        if (block.isFinalStretch) {
            add("🎓" to "Simulacros completos: apunta a |${ExamRules.PASS_SCORE} de ${ExamRules.QUESTION_COUNT}| o más")
            add(
                "🔁" to (block.reinforce.takeIf { it.isNotEmpty() }
                    ?.let { "Repasa |${it.joinToString(" y ")}|, lo que fallaste en la prueba" }
                    ?: "Repasa |tus fallos| de los tests y simulacros")
            )
            add(
                "🏁" to (examDate?.let { "|${shortDate(it)}|: tu examen" }
                    ?: "Bali te dirá cuándo estás |a punto para pedir fecha|")
            )
        } else {
            block.sections.forEach { add("📚" to it) }
            if (block.reinforce.isNotEmpty()) {
                add("⚠️" to "Refuerza |${block.reinforce.joinToString(" y ")}|: lo fallaste en la prueba")
            }
            add("🎯" to "|${StudyPlanBuilder.DAILY_QUESTIONS} preguntas| al día")
            add(
                "📝" to if (isDayBased) "|1 simulacro| en estos días"
                else "|${StudyPlanBuilder.WEEKLY_MOCK_EXAMS} simulacros| por semana"
            )
        }
    }

/**
 * One line inside an open stretch.
 *
 * @param emoji the symbol on the left
 * @param text the line; `|` pairs mark the words to highlight
 */
@Composable
private fun PlanLine(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(text = emoji, fontSize = 16.sp, modifier = Modifier.width(26.dp))
        Text(
            text = text.highlightPipes(MaterialTheme.colorScheme.primary),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * A real screen of the app with what it is for.
 *
 * @property image the screenshot, rendered from the app by `OnboardingShowcaseScreenshotTest`
 * @property caption one line on what the user does there
 * @property styleKey the [com.jesuskrastev.bali.ui.screens.onboarding.LearningStyle.key] it shows best
 */
private data class Showcase(@DrawableRes val image: Int, val caption: String, val styleKey: String?)

private val SHOWCASES = listOf(
    Showcase(
        R.drawable.onboarding_shot_exam,
        "Simulacros como el examen real: ${ExamRules.QUESTION_COUNT} preguntas, 30 minutos, máximo ${ExamRules.MAX_MISTAKES} fallos",
        OnboardingConfig.STYLE_MOCK_EXAMS.key
    ),
    Showcase(
        R.drawable.onboarding_shot_practice,
        "Tests cortos, con cada fallo explicado al momento",
        OnboardingConfig.STYLE_QUICK_TESTS.key
    ),
    Showcase(
        R.drawable.onboarding_shot_chat,
        "¿Una duda? Pregúntale a Bali y te la explica, a cualquier hora",
        OnboardingConfig.STYLE_EXPLANATIONS.key
    ),
    Showcase(
        R.drawable.onboarding_shot_games,
        "Minijuegos de señales y normas para cuando no te apetece un test",
        OnboardingConfig.STYLE_GAMES.key
    ),
    Showcase(
        R.drawable.onboarding_shot_stats,
        "Tu probabilidad de aprobar, para saber cuándo estás a punto",
        null
    )
)

/**
 * Real screens of the app in a horizontal carousel, the one for the user's favourite way of
 * practising first and marked as such.
 *
 * @param favouriteKey the learning style the user picked, or null
 */
@Composable
private fun ShowcaseCarousel(favouriteKey: String?) {
    val ordered = remember(favouriteKey) { SHOWCASES.sortedByDescending { it.styleKey == favouriteKey } }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(ordered, key = { it.image }) { showcase ->
            ShowcaseCard(showcase = showcase, isFavourite = favouriteKey != null && showcase.styleKey == favouriteKey)
        }
    }
}

/**
 * One screen of the carousel: the screenshot in a phone-like frame and its caption.
 *
 * @param showcase the screen
 * @param isFavourite whether it shows the user's favourite way of practising
 */
@Composable
private fun ShowcaseCard(showcase: Showcase, isFavourite: Boolean) {
    Column(modifier = Modifier.width(SHOT_WIDTH), horizontalAlignment = Alignment.CenterHorizontally) {
        // Every card keeps the badge's slot, so the phones stay aligned whichever one wears it.
        Box(modifier = Modifier.height(30.dp), contentAlignment = Alignment.Center) {
            if (isFavourite) {
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                    Text(
                        text = "⭐ Tu favorita",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.onBackground,
                shadowElevation = 4.dp
            ) {
                Image(
                    painter = painterResource(showcase.image),
                    contentDescription = showcase.caption,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .padding(5.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .fillMaxWidth()
                        .aspectRatio(360f / 780f)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = showcase.caption,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** The contrast block: what the next weeks look like with and without the app. */
@Composable
private fun WhyBaliSection() {
    SectionTitle("¿Por qué Bali?")

    Spacer(modifier = Modifier.height(16.dp))

    ContrastCard(
        title = "Sin Bali",
        accent = BaliAccentRed,
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
        accent = BaliAccentGreen,
        isPositive = true,
        items = listOf(
            "Cada fallo, explicado al momento. Y tus dudas, resueltas a cualquier hora",
            "Un plan con tu fecha y tu ritmo",
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
 * The single pulled-out quote that sits between the plan and the comparison.
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
 * Formats a day short, as the chips and the plan name it: "24 oct".
 *
 * @param millis any instant of the day
 * @return the day and abbreviated month
 */
private fun shortDate(millis: Long): String =
    SimpleDateFormat("d MMM", SPANISH).format(Date(millis)).trimEnd('.')

/**
 * Names the days a stretch covers: "3–9 oct", "28 oct – 3 nov" or a single "5 oct".
 *
 * @param start local midnight of the first day
 * @param end local midnight of the last day
 * @return the range
 */
private fun dateRange(start: Long, end: Long): String {
    if (start == end) return shortDate(start)
    val sameMonth = SimpleDateFormat("MM", SPANISH).let { it.format(Date(start)) == it.format(Date(end)) }
    return if (sameMonth) {
        "${SimpleDateFormat("d", SPANISH).format(Date(start))}–${shortDate(end)}"
    } else {
        "${shortDate(start)} – ${shortDate(end)}"
    }
}

@Preview(showBackground = true, heightDp = 2200)
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
                    weeklyStudy = OnboardingConfig.WEEKLY_STUDY_DAILY,
                    learningPreference = OnboardingConfig.STYLE_MOCK_EXAMS.key,
                    province = "Almería",
                    examDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(21)
                )
            )
        }
    }
}
