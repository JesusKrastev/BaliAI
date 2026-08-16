package com.jesuskrastev.bali.ui.screens.coins

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun CoinsGainedScreen(
    coinsGained: Int,
    onContinueClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val mainScale = remember { Animatable(0f) }
    val mascotScale = remember { Animatable(0.8f) }
    
    // Animación de entrada
    LaunchedEffect(Unit) {
        launch {
            mascotScale.animateTo(
                1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            )
        }
        delay(300)
        launch {
            mainScale.animateTo(
                1.2f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
            mainScale.animateTo(1f)
        }
    }

    Scaffold(
        bottomBar = {
            Button(
                onClick = onContinueClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Continuar", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Partículas de monedas saltando al principio
            repeat(15) { index ->
                CoinParticle(delayMillis = index * 50)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Mascot holding a bowl of coins
                Image(
                    painter = painterResource(id = R.drawable.bali_coins),
                    contentDescription = null,
                    modifier = Modifier
                        .size(240.dp)
                        .scale(mascotScale.value),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(48.dp))

                Row(
                    modifier = Modifier.scale(mainScale.value),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.coin),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "+$coinsGained",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFA500)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Recolecta tokens y cámbialo por power ups, energias y más.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

@Composable
fun CoinParticle(delayMillis: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "particle")
    var visible by remember { mutableStateOf(false) }
    
    val startX = remember { Random.nextFloat() * 0.4f + 0.3f } // Cerca del centro
    val targetX = remember { Random.nextFloat() * 2f - 1f } // Dispersión total
    val targetY = remember { Random.nextFloat() * -1.5f - 0.5f } // Hacia arriba
    
    val progress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
        )
        visible = false
    }

    if (visible) {
        val xOffset = (targetX * progress.value * 200).dp
        val yOffset = (targetY * progress.value * 300 + (progress.value * progress.value * 500)).dp // Parábola
        val rotation = progress.value * 720f
        val opacity = 1f - progress.value

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = xOffset.toPx()
                    translationY = yOffset.toPx()
                    rotationZ = rotation
                    alpha = opacity
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.coin),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
