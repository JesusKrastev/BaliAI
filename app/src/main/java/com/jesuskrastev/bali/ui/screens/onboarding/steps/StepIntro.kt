package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.ui.screens.onboarding.components.highlightPipes
import com.jesuskrastev.bali.ui.theme.BaliTheme
import kotlinx.coroutines.launch

/** Share of the chest animation after which the lid is open and the cards can come out. */
private const val CHEST_OPEN_AT = 0.55f

/** How long the chest takes to open when the animation file gives no duration. */
private const val CHEST_FALLBACK_MS = 2_000

/**
 * One card the chest gives out: a promise that answers a fear most theory students share, and the
 * real screen of the app that keeps it.
 *
 * @property title the promise; `|` pairs mark the words to highlight
 * @property body one line on how the app keeps it
 * @property image the screenshot, rendered from the app by `OnboardingShowcaseScreenshotTest`
 */
internal data class IntroCard(val title: String, val body: String, @DrawableRes val image: Int)

/**
 * The four cards, in the order the fears usually come: failing, the trick questions, the boredom of
 * the manual, and not knowing whether one is ready.
 */
internal val INTRO_CARDS = listOf(
    IntroCard(
        title = "Aprueba |a la primera|",
        body = "Y olvídate de pagar otra tasa. Simulacros como el examen real: " +
            "${ExamRules.QUESTION_COUNT} preguntas, 30 minutos.",
        image = R.drawable.onboarding_shot_exam
    ),
    IntroCard(
        title = "Adiós a las |preguntas trampa|",
        body = "Cada fallo, explicado al momento. Y tus dudas, resueltas cuando quieras.",
        image = R.drawable.onboarding_shot_practice
    ),
    IntroCard(
        title = "|Diviértete| aprendiendo",
        body = "Minijuegos, rachas y monedas: estudiar deja de ser un rollo.",
        image = R.drawable.onboarding_shot_games
    ),
    IntroCard(
        title = "Sabrás cuándo estás |a punto|",
        body = "Tu probabilidad de aprobar, para pedir fecha sin miedo.",
        image = R.drawable.onboarding_shot_stats
    )
)

/**
 * The first screen of the flow, before any question: what Bali is, shown rather than told.
 *
 * A chest opens and gives out four cards, each a promise against a common fear of the theory exam
 * with the real screen that keeps it. The cards swipe, and the button walks through them; the last
 * one starts the questions. Tapping the chest opens it straight away.
 *
 * @param onPageShown called with each card's position the first time it is on screen
 * @param onFinish called from the last card, to start the questions
 */
@Composable
fun StepIntro(onPageShown: (Int) -> Unit, onFinish: () -> Unit) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.bali_chest_opening))
    val chest = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(composition) {
        val duration = composition?.duration?.toInt()?.takeIf { it > 0 } ?: CHEST_FALLBACK_MS
        chest.animateTo(1f, tween(durationMillis = duration, easing = LinearEasing))
    }
    val opened = chest.value >= CHEST_OPEN_AT
    val chestSize by animateDpAsState(if (opened) 96.dp else 220.dp, tween(500), label = "intro_chest_size")

    val pager = rememberPagerState { INTRO_CARDS.size }
    LaunchedEffect(pager.currentPage, opened) {
        if (opened) onPageShown(pager.currentPage)
    }
    val isLast = pager.currentPage == INTRO_CARDS.lastIndex

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LottieAnimation(
            composition = composition,
            progress = { chest.value },
            modifier = Modifier
                .size(chestSize)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { scope.launch { chest.snapTo(1f) } }
        )

        if (!opened) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Esto es lo que |te llevas| 🎁".highlightPipes(MaterialTheme.colorScheme.primary),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }

        AnimatedVisibility(
            visible = opened,
            modifier = Modifier.weight(1f),
            enter = scaleIn(tween(450), initialScale = 0.4f) + fadeIn(tween(300))
        ) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                HorizontalPager(
                    state = pager,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page -> IntroCardPage(INTRO_CARDS[page]) }

                Spacer(modifier = Modifier.height(12.dp))
                PageDots(count = INTRO_CARDS.size, current = pager.currentPage)
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (isLast) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (isLast) "¡Empezamos! 🚀" else "Siguiente →",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * One card: the promise, how the app keeps it and the screen that shows it.
 *
 * @param card the card to draw
 */
@Composable
private fun IntroCardPage(card: IntroCard) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = card.title.highlightPipes(MaterialTheme.colorScheme.primary),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = card.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.onBackground,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(360f / 780f)
            ) {
                Image(
                    painter = painterResource(card.image),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .padding(5.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .fillMaxSize()
                )
            }
        }
    }
}

/**
 * Dots under the cards, the current one stretched into a pill.
 *
 * @param count how many cards there are
 * @param current the card on screen
 */
@Composable
private fun PageDots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            val width by animateFloatAsState(if (index == current) 22f else 8f, label = "intro_dot")
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width.dp)
                    .background(
                        color = if (index == current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun StepIntroPreview() {
    BaliTheme {
        StepIntro(onPageShown = {}, onFinish = {})
    }
}
