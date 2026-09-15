package com.jesuskrastev.bali.ui.screens.test

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.review.InAppReviewEffect

fun getMotivationalMessage(accuracy: Int, durationSeconds: Int): Pair<String, String> {
    val minutes = durationSeconds / 60
    return when {
        accuracy == 100 -> Pair(
            "¡Perfección absoluta! \uD83C\uDFAF",
            "10 de 10. Has dominado esta lección por completo."
        )
        accuracy >= 90 -> Pair(
            "¡Excelente resultado! \uD83D\uDD25",
            "Casi perfecto. Estás muy cerca de dominar esto."
        )
        accuracy >= 70 -> Pair(
            "¡Buen trabajo! \uD83D\uDCAA",
            "Sólido. Repasa los fallos y la próxima será perfecta."
        )
        accuracy >= 50 -> Pair(
            "Vas por buen camino \uD83D\uDCC8",
            "Más de la mitad bien. Sigue practicando y mejorarás."
        )
        accuracy >= 30 -> Pair(
            "No te rindas \uD83E\uDDE0",
            "Cada fallo es una lección. Repasa y vuelve a intentarlo."
        )
        else -> Pair(
            "Aquí empieza el aprendizaje \uD83C\uDF31",
            "No importa el comienzo, importa no parar. ¡Tú puedes!"
        )
    }
}

@Composable
/**
 * Celebrates a completed test and requests a Play Store review after an excellent result.
 *
 * @param xpGained total experience earned by the user
 * @param baseXp experience earned before bonuses
 * @param bonusPerfection optional bonus for a perfect result
 * @param bonusFast optional bonus for completing the test quickly
 * @param bonusStreak optional bonus for maintaining a streak
 * @param leveledUp whether the result increased the user's level
 * @param durationSeconds time spent completing the test
 * @param accuracy percentage of correctly answered questions
 * @param onContinueClick callback invoked when the user continues
 * @param secondaryActionLabel optional label for a secondary outlined action (e.g. "JUGAR OTRA VEZ"
 *   in the arcade mini-games); when null, only the primary continue button is shown
 * @param onSecondaryActionClick callback invoked when the secondary action is tapped
 */
fun TestResultScreen(
    xpGained: Int,
    baseXp: Int,
    bonusPerfection: Int? = null,
    bonusFast: Int? = null,
    bonusStreak: Int? = null,
    leveledUp: Boolean = false,
    durationSeconds: Int,
    accuracy: Int,
    onContinueClick: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: () -> Unit = {},
) {
    val showConfetti = accuracy >= 70
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60

    InAppReviewEffect(accuracy = accuracy)

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onContinueClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("CONTINUAR", fontWeight = FontWeight.Black)
                }
                if (secondaryActionLabel != null) {
                    OutlinedButton(
                        onClick = onSecondaryActionClick,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(secondaryActionLabel, fontWeight = FontWeight.Black)
                    }
                }
            }
        }) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (showConfetti) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (leveledUp) {
                    LevelUpBadge()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Mascota Bali
                Image(
                    painter = painterResource(id = R.drawable.bali),
                    contentDescription = "Bali",
                    modifier = Modifier.size(120.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(16.dp))

                val (title, subtitle) = remember(accuracy, durationSeconds) {
                    getMotivationalMessage(accuracy, durationSeconds)
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ResultStatCard(
                        modifier = Modifier.weight(1f),
                        label = "EXP Total",
                        value = "+$xpGained",
                        icon = Icons.Rounded.Bolt,
                        color = Color(0xFFFACC15),
                    )

                    ResultStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Aciertos",
                        value = "$accuracy%",
                        icon = Icons.Rounded.CheckCircle,
                        color = Color(0xFF22C55E)
                    )

                    ResultStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Tiempo",
                        value = String.format("%02d:%02d", minutes, seconds),
                        icon = Icons.Rounded.Timer,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // XP Breakdown
                XpBreakdownCard(baseXp, bonusPerfection, bonusFast, bonusStreak)
            }
        }
    }
}

@Composable
fun XpRow(label: String, value: Int, icon: String? = null, isBonus: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Text(text = icon, modifier = Modifier.padding(end = 8.dp), fontSize = 14.sp)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = "+$value",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun LevelUpBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "levelup")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(50),
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Star, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "¡SUBISTE DE NIVEL!",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
fun XpBreakdownCard(baseXp: Int, perfection: Int?, fast: Int?, streak: Int?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Desglose de Experiencia",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            XpRow(label = "Base de la lección", value = baseXp)

            if (perfection != null) {
                XpRow(label = "Bono: Perfección", value = perfection, icon = "🎯", isBonus = true)
            }
            if (fast != null) {
                XpRow(label = "Bono: Velocidad", value = fast, icon = "⚡", isBonus = true)
            }
            if (streak != null) {
                XpRow(label = "Bono: Racha", value = streak, icon = "🔥", isBonus = true)
            }
        }
    }
}

@Composable
fun ResultStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
