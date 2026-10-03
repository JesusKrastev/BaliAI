package com.jesuskrastev.bali.ui.screens.greetings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.screens.onboarding.components.highlightPipes
import com.jesuskrastev.bali.ui.theme.BaliPrimary

/** What Bali is, in three tags a first-time visitor reads in a second. All three are real features. */
private val VALUE_TAGS = listOf("🎓 Simulacros tipo examen", "💡 Cada fallo explicado", "📅 Un plan hasta tu examen")

/**
 * Presents Bali's welcome screen: the mascot lit up like the beacon it is, saying hello, then what
 * the app does in one line and the two ways in.
 *
 * The first screen has one job — tell a stranger what this is and why it is worth a minute — so
 * the copy names the outcome (passing the theory exam first time) and the means (exam-style tests,
 * every mistake explained) instead of a claim about how many people already used it.
 *
 * @param sharedTransitionScope scope shared with destinations that animate the mascot.
 * @param animatedVisibilityScope scope that owns the destination visibility animation.
 * @param onStartClick action invoked when the user begins onboarding.
 * @param onAuthClick action invoked when the user signs in with an existing account.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GreetingsScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onStartClick: () -> Unit = {},
    onAuthClick: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    // AppNavigation's own Scaffold already reserves the status bar (top) and the
    // navigation bar (bottom) for this whole screen via NavHost's content padding, so this
    // inner Scaffold must not reserve system bar insets a second time here — that previously
    // pushed the footer's buttons up off the true bottom edge with dead space underneath.
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The hero absorbs whatever height is left, so the copy and the buttons stay grouped
            // instead of a blank strip opening between them on taller screens.
            GreetingsHeader(
                isVisible = isVisible,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = Modifier.weight(1f)
            )

            GreetingsContent(
                isVisible = isVisible,
                modifier = Modifier.padding(start = 28.dp, top = 4.dp, end = 28.dp)
            )

            GreetingsFooter(
                isVisible = isVisible,
                onStartClick = onStartClick,
                onAuthClick = onAuthClick
            )
        }
    }
}

/**
 * Draws the curved brand header with the mascot in the middle: a beacon light, so it glows and
 * bobs gently, and says hello from a speech bubble.
 *
 * @param isVisible whether the mascot entrance animation should play.
 * @param sharedTransitionScope scope shared with destinations that animate the mascot.
 * @param animatedVisibilityScope scope that owns the destination visibility animation.
 * @param modifier layout modifier that sizes the header within the screen.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun GreetingsHeader(
    isVisible: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val beacon = rememberInfiniteTransition(label = "beacon")
    val glow by beacon.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "beacon_glow"
    )
    val bob by beacon.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "beacon_bob"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(1f)
            .graphicsLayer(clip = false)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                quadraticBezierTo(
                    size.width / 2f, size.height - 150f, 0f, size.height
                )
                close()
            }
            drawPath(path = path, color = BaliPrimary)
        }

        // The beacon's light: a warm halo that breathes behind the mascot.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp)
                .graphicsLayer {
                    scaleX = glow
                    scaleY = glow
                    alpha = if (isVisible) 1f else 0f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFE08A).copy(alpha = 0.75f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        AnimatedVisibility(
            visible = isVisible,
            modifier = Modifier.align(Alignment.Center),
            enter = slideInVertically(
                initialOffsetY = { 600 }, animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(animationSpec = tween(1000))
        ) {
            with(sharedTransitionScope) {
                Image(
                    painter = painterResource(id = R.drawable.bali),
                    contentDescription = "Bali",
                    modifier = Modifier
                        .size(200.dp)
                        .graphicsLayer { translationY = -8.dp.toPx() * bob }
                        .sharedElement(
                            rememberSharedContentState(key = "bali_mascot"),
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                    contentScale = ContentScale.Fit
                )
            }
        }

        AnimatedVisibility(
            visible = isVisible,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 84.dp, y = (-104).dp),
            enter = scaleIn(tween(400, delayMillis = 700)) + fadeIn(tween(400, delayMillis = 700))
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Text(
                    text = "¡Hola! Soy Bali 👋",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
    }
}

/**
 * The promise and what backs it, right under the header.
 *
 * @param isVisible whether the content entrance animations should play.
 * @param modifier layout modifier that positions the content between header and actions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GreetingsContent(
    isVisible: Boolean, modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(visible = isVisible, enter = staggeredEnter(delayMillis = 300)) {
            Text(
                text = "Aprueba el teórico\n|a la primera|".highlightPipes(MaterialTheme.colorScheme.primary),
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(visible = isVisible, enter = staggeredEnter(delayMillis = 500)) {
            Text(
                text = "Tests como los del examen de la DGT y un profe que te explica cada fallo.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        AnimatedVisibility(visible = isVisible, enter = staggeredEnter(delayMillis = 700)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                VALUE_TAGS.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shows the primary onboarding call to action and the sign-in link for returning users.
 *
 * @param isVisible whether the footer entrance animation should play.
 * @param onStartClick action invoked when the user begins onboarding.
 * @param onAuthClick action invoked when the user signs in with an existing account.
 */
@Composable
private fun GreetingsFooter(
    isVisible: Boolean,
    onStartClick: () -> Unit,
    onAuthClick: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, top = 16.dp, end = 32.dp, bottom = 12.dp),
        enter = staggeredEnter(delayMillis = 900, offsetY = 100)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onStartClick,
                modifier = Modifier.height(56.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
            ) {
                Text(
                    text = "¡A por mi L!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            TextButton(
                onClick = onAuthClick,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    // Sits over the plain background below the header curve, not over the
                    // orange header, so it needs a theme-aware color instead of a fixed white
                    // (that was unreadable in light theme).
                    text = "Ya tengo cuenta",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Builds the slide-up-and-fade entrance shared by the welcome screen's staggered elements.
 *
 * @param delayMillis how long the element waits before animating in.
 * @param offsetY distance in pixels the element slides up from.
 * @return the combined enter transition.
 */
private fun staggeredEnter(delayMillis: Int, offsetY: Int = 50): EnterTransition =
    slideInVertically(
        initialOffsetY = { offsetY },
        animationSpec = tween(durationMillis = 600, delayMillis = delayMillis)
    ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = delayMillis))
