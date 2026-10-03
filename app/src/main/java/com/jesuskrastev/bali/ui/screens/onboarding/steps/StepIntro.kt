package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.ui.screens.onboarding.components.highlightPipes
import com.jesuskrastev.bali.ui.theme.BaliTheme
import kotlinx.coroutines.launch

/** Marker of `bali_chest_opening.json` that holds the lid shut: frame 0, a single still frame. */
private const val CHEST_CLOSED_MARKER = "closed_idle"

/** Marker that plays the lid opening and the glow rising out of the chest (frames 1–37). */
private const val CHEST_OPENING_MARKER = "opening"

/** Marker that loops the open chest's gentle shimmer (frames 37–60). */
private const val CHEST_OPENED_MARKER = "opened_idle"

/** Where the chest is: shut and waiting for a tap, lid opening, or open with the cards out. */
private enum class ChestPhase { Closed, Opening, Opened }

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
 * A shut chest waits, rocking gently, for the user to open it — a tap on the chest or on the
 * button. The lid then opens with the `opening` part of `bali_chest_opening`, the phone buzzes,
 * the open chest keeps shimmering at the top and four cards come out of it, each a promise against
 * a common fear of the theory exam with the real screen that keeps it. The cards swipe, and the
 * button walks through them; the last one starts the questions.
 *
 * Opening it is the user's first action in the app, on purpose. If the animation cannot be loaded
 * the cards are shown straight away, so nobody is left in front of an empty screen.
 *
 * @param onPageShown called with each card's position the first time it is on screen
 * @param onFinish called from the last card, to start the questions
 */
@Composable
fun StepIntro(onPageShown: (Int) -> Unit, onFinish: () -> Unit) {
    val compositionResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.bali_chest_opening))
    val composition = compositionResult.value
    val chest = rememberLottieAnimatable()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var phase by rememberSaveable { mutableStateOf(ChestPhase.Closed) }

    // Shows the shut chest, or the open one when coming back to this screen.
    LaunchedEffect(composition, compositionResult.isFailure) {
        if (compositionResult.isFailure) {
            phase = ChestPhase.Opened
            return@LaunchedEffect
        }
        val loaded = composition ?: return@LaunchedEffect
        when (phase) {
            ChestPhase.Closed -> chest.snapTo(
                composition = loaded,
                progress = loaded.getMarker(CHEST_CLOSED_MARKER)?.startFrame?.let(loaded::getProgressForFrame) ?: 0f
            )
            else -> {
                phase = ChestPhase.Opened
                chest.animate(
                    composition = loaded,
                    iterations = LottieConstants.IterateForever,
                    clipSpec = LottieClipSpec.Marker(CHEST_OPENED_MARKER)
                )
            }
        }
    }

    val openChest: () -> Unit = {
        val loaded = composition
        when {
            phase != ChestPhase.Closed -> Unit
            loaded == null -> phase = ChestPhase.Opened
            else -> scope.launch {
                phase = ChestPhase.Opening
                chest.animate(
                    composition = loaded,
                    iterations = 1,
                    clipSpec = LottieClipSpec.Marker(CHEST_OPENING_MARKER)
                )
                phase = ChestPhase.Opened
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                chest.animate(
                    composition = loaded,
                    iterations = LottieConstants.IterateForever,
                    clipSpec = LottieClipSpec.Marker(CHEST_OPENED_MARKER)
                )
            }
        }
    }

    val opened = phase == ChestPhase.Opened
    val chestSize by animateDpAsState(if (opened) 96.dp else 240.dp, tween(500), label = "intro_chest_size")
    val rocking = rememberInfiniteTransition(label = "intro_chest_rock")
    val rock by rocking.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "intro_chest_rock_angle"
    )

    val pager = rememberPagerState { INTRO_CARDS.size }
    LaunchedEffect(pager.currentPage, opened) {
        if (opened) onPageShown(pager.currentPage)
    }
    val isLast = pager.currentPage == INTRO_CARDS.lastIndex

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!opened) Spacer(modifier = Modifier.weight(1f))

        LottieAnimation(
            composition = composition,
            progress = { chest.progress },
            modifier = Modifier
                .size(chestSize)
                .graphicsLayer { rotationZ = if (phase == ChestPhase.Closed) rock else 0f }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = openChest
                )
                .semantics { contentDescription = if (opened) "Cofre abierto" else "Cofre: tócalo para abrirlo" }
        )

        if (!opened) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Tu kit para aprobar |el teórico| 🎁".highlightPipes(MaterialTheme.colorScheme.primary),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (phase == ChestPhase.Opening) "Abriendo…" else "Toca el cofre para ver qué hay dentro",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = openChest,
                enabled = phase == ChestPhase.Closed,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Abrir el cofre 🎁", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        AnimatedVisibility(
            visible = opened,
            modifier = Modifier.weight(1f),
            enter = scaleIn(tween(450), initialScale = 0.4f, transformOrigin = TransformOrigin(0.5f, 0f)) +
                fadeIn(tween(300))
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
