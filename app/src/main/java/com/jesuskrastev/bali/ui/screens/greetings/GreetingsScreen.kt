package com.jesuskrastev.bali.ui.screens.greetings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GreetingsScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onStartClick: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GreetingsHeader(
                isVisible = isVisible,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )

            GreetingsContent(
                isVisible = isVisible, modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 32.dp)
            )

            GreetingsFooter(
                isVisible = isVisible, onStartClick = onStartClick
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun GreetingsHeader(
    isVisible: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .zIndex(1f)
            .graphicsLayer(clip = false)
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
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
                        .size(170.dp)
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

@Composable
private fun GreetingsContent(
    isVisible: Boolean, modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = isVisible, enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = tween(durationMillis = 600, delayMillis = 300)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 300))
        ) {
            Text(
                text = "¡Hola! Soy Bali",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = isVisible, enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = tween(durationMillis = 600, delayMillis = 500)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 500))
        ) {
            Text(
                text = "Te ayudo a conseguir tu L a la primera",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(
            visible = isVisible, enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = tween(durationMillis = 600, delayMillis = 700)
            ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 700))
        ) {
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

@Composable
private fun GreetingsFooter(
    isVisible: Boolean, onStartClick: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        enter = slideInVertically(
            initialOffsetY = { 100 }, animationSpec = tween(durationMillis = 600, delayMillis = 900)
        ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 900))
    ) {
        Button(
            onClick = onStartClick,
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            Text(
                text = "¡A por mi L!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}