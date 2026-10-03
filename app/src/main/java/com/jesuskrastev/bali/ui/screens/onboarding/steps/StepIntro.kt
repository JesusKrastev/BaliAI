package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.ui.screens.onboarding.components.highlightPipes
import com.jesuskrastev.bali.ui.theme.BaliTheme
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * One card of the intro: a promise that answers a fear most theory students share, and the
 * real screen of the app that keeps it.
 *
 * @property title the promise; `|` pairs mark the words to highlight
 * @property body one line on how the app keeps it
 * @property image the screenshot, rendered in the dark theme by `OnboardingShowcaseScreenshotTest`
 * @property badge the short claim floating over the phone, emoji first
 * @property badgeAt how far down the phone the claim sits (0 top, 1 bottom), chosen per screenshot
 *   so it lands on empty space instead of covering what the screen is showing off
 */
internal data class IntroCard(
    val title: String,
    val body: String,
    @DrawableRes val image: Int,
    val badge: String,
    val badgeAt: Float
)

/**
 * The four cards, in the order the fears usually come: failing, the trick questions, the boredom of
 * the manual, and not knowing whether one is ready.
 */
internal val INTRO_CARDS = listOf(
    IntroCard(
        title = "Aprueba |a la primera|",
        body = "Y olvídate de pagar otra tasa. Simulacros como el examen real: " +
            "${ExamRules.QUESTION_COUNT} preguntas, 30 minutos.",
        image = R.drawable.onboarding_shot_exam,
        badge = "⏱️ Como el examen real",
        badgeAt = 0.6f
    ),
    IntroCard(
        title = "Adiós a las |preguntas trampa|",
        body = "Cada fallo, explicado al momento. Y tus dudas, resueltas cuando quieras.",
        image = R.drawable.onboarding_shot_practice,
        badge = "💡 Explicado al momento",
        badgeAt = 0.5f
    ),
    IntroCard(
        title = "|Diviértete| aprendiendo",
        body = "Minijuegos, rachas y monedas: estudiar deja de ser un rollo.",
        image = R.drawable.onboarding_shot_games,
        badge = "🎮 +XP y monedas",
        badgeAt = 0.21f
    ),
    IntroCard(
        title = "Sabrás cuándo estás |a punto|",
        body = "Tu probabilidad de aprobar, para pedir fecha sin miedo.",
        image = R.drawable.onboarding_shot_stats,
        badge = "🎯 95 % de aprobar",
        badgeAt = 0.47f
    )
)

/**
 * The first screen of the flow, before any question: what Bali is, shown rather than told.
 *
 * Four cards, each a promise against a common fear of the theory exam with the real screen that
 * keeps it. The cards swipe, and the button walks through them; the last one starts the questions.
 *
 * @param onPageShown called with each card's position the first time it is on screen
 * @param onFinish called from the last card, to start the questions
 */
@Composable
fun StepIntro(onPageShown: (Int) -> Unit, onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { INTRO_CARDS.size }
    LaunchedEffect(pager.currentPage) { onPageShown(pager.currentPage) }
    val isLast = pager.currentPage == INTRO_CARDS.lastIndex

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pager,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            IntroCardPage(
                card = INTRO_CARDS[page],
                pageOffset = { pager.getOffsetDistanceInPages(page) }
            )
        }

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

/**
 * One card: the promise, how the app keeps it and the screen that shows it, framed as a phone
 * glowing in the brand colour, with a claim floating over it. While swiping, the phone leans and
 * shrinks a little toward the side it leaves by.
 *
 * @param card the card to draw
 * @param pageOffset how far this card is from the centre, in pages (0 when settled)
 */
@Composable
private fun IntroCardPage(card: IntroCard, pageOffset: () -> Float) {
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
        Spacer(modifier = Modifier.height(18.dp))
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer {
                    val offset = pageOffset()
                    rotationZ = offset * -6f
                    val scale = 1f - 0.08f * abs(offset).coerceAtMost(1f)
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.TopCenter
        ) {
            // The phone is as tall as the box; its right edge sets where the sticker hangs off it.
            val phoneWidth = minOf(maxWidth, maxHeight * (360f / 780f))
            val phoneRightGap = (maxWidth - phoneWidth) / 2
            PhoneGlow()
            PhoneFrame(image = card.image)
            FloatingBadge(
                text = card.badge,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = -(phoneRightGap - BADGE_OVERHANG).coerceAtLeast(0.dp), y = maxHeight * card.badgeAt)
            )
        }
    }
}

/** Soft brand-coloured halo behind the phone. */
@Composable
private fun PhoneGlow() {
    val glow = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxSize()) {
        // Kept within the width so the halo fades out instead of being cut at the sides.
        val radius = size.width / 2
        drawCircle(
            brush = Brush.radialGradient(
                listOf(glow.copy(alpha = 0.45f), glow.copy(alpha = 0.12f), Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius
        )
    }
}

/**
 * A dark phone around a screenshot: rounded bezel, a thin rim of light and a punch-hole camera.
 *
 * @param image the screenshot to show
 */
@Composable
private fun PhoneFrame(@DrawableRes image: Int) {
    Surface(
        shape = RoundedCornerShape(34.dp),
        color = PHONE_BEZEL,
        shadowElevation = 18.dp,
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.16f)),
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(360f / 780f)
    ) {
        Box {
            Image(
                painter = painterResource(image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .padding(6.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .size(9.dp)
                    .background(PHONE_BEZEL, CircleShape)
            )
        }
    }
}

/**
 * The card's claim as a sticker stuck on the phone's right edge: tilted, bobbing gently.
 *
 * @param text the claim, emoji first
 * @param modifier positions the pill
 */
@Composable
private fun FloatingBadge(text: String, modifier: Modifier = Modifier) {
    val bob = rememberInfiniteTransition(label = "intro_badge")
    val dy by bob.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(1_100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "intro_badge_bob"
    )
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 10.dp,
        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.9f)),
        modifier = modifier.graphicsLayer {
            translationY = dy.dp.toPx()
            rotationZ = -5f
        }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

/** How far the claim sticker sticks out past the phone's right edge. */
private val BADGE_OVERHANG = 22.dp

/** Near-black bezel of the phone frame, the same in both themes. */
private val PHONE_BEZEL = Color(0xFF05070D)

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
