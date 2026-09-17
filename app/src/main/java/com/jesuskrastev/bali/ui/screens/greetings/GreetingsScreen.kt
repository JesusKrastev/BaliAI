package com.jesuskrastev.bali.ui.screens.greetings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.jesuskrastev.bali.ui.theme.BaliPrimary

/**
 * Presents Bali's welcome screen with a curved hero, explanatory content and account actions.
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
                modifier = Modifier.padding(start = 32.dp, top = 8.dp, end = 32.dp)
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
 * Draws the curved brand header and centers the Bali mascot inside it.
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
                    contentDescription = "Beacon Bali",
                    modifier = Modifier
                        .size(210.dp)
                        .sharedElement(
                            rememberSharedContentState(key = "bali_mascot"),
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

/**
 * Shows the welcome copy and social proof immediately below the curved header.
 *
 * @param isVisible whether the content entrance animations should play.
 * @param modifier layout modifier that positions the content between header and actions.
 */
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
                text = "¡Hola! Soy Bali",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(visible = isVisible, enter = staggeredEnter(delayMillis = 500)) {
            Text(
                text = "Te ayudo a conseguir tu L a la primera",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible = isVisible, enter = staggeredEnter(delayMillis = 700)) {
            Image(
                painter = painterResource(
                    id = if (isSystemInDarkTheme()) R.drawable.editors_choice_dark else R.drawable.editors_choice_light
                ),
                contentDescription = "Editors Choice",
                modifier = Modifier
                    .height(90.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
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
