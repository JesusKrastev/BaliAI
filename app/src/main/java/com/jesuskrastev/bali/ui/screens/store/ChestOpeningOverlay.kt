package com.jesuskrastev.bali.ui.screens.store

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.model.ChestReward
import com.jesuskrastev.bali.domain.model.ShopInventoryItem
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import com.jesuskrastev.bali.ui.theme.BaliDarkBackground
import kotlinx.coroutines.delay

/** Marker of `bali_chest_opening.json` that holds the lid shut: frame 0, a single still frame. */
private const val CHEST_CLOSED_MARKER = "closed_idle"

/** Marker that plays the lid opening and the glow rising out of the chest (frames 1–37). */
private const val CHEST_OPENING_MARKER = "opening"

/** Marker that loops the open chest's gentle idle shimmer (frames 37–60). */
private const val CHEST_OPENED_MARKER = "opened_idle"

/** How long the shut chest is seen before it starts to open, so the user registers it first. */
private const val CLOSED_BEAT_MS = 500L

/** Where the overlay is in its sequence: shut, lid opening, or open with the prize on show. */
private enum class ChestPhase { Closed, Opening, Opened }

/**
 * Full-screen reveal of a surprise chest the user has just paid for: the shut chest waits a
 * beat, the lid opens with the Lottie animation `bali_chest_opening`, and then the prize pops out
 * above a button to collect it. The coins are already credited when this is shown, so leaving it
 * early (system back) loses nothing.
 *
 * If the animation cannot be loaded the prize is shown straight away instead of hanging on an
 * empty screen.
 *
 * @param reward coins or a consumable item granted by the chest
 * @param onDismiss invoked when the user closes the reveal
 */
@Composable
fun ChestOpeningOverlay(reward: ChestReward, onDismiss: () -> Unit) {
    val compositionResult = rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.bali_chest_opening)
    )
    val composition = compositionResult.value
    val animatable = rememberLottieAnimatable()
    val haptics = LocalHapticFeedback.current
    var phase by remember { mutableStateOf(ChestPhase.Closed) }

    LaunchedEffect(composition, compositionResult.isFailure) {
        if (compositionResult.isFailure) {
            phase = ChestPhase.Opened
            return@LaunchedEffect
        }
        val loaded = composition ?: return@LaunchedEffect
        animatable.snapTo(
            composition = loaded,
            progress = 0f
        )
        delay(CLOSED_BEAT_MS)
        phase = ChestPhase.Opening
        animatable.animate(
            composition = loaded,
            iterations = 1,
            clipSpec = LottieClipSpec.Marker(CHEST_OPENING_MARKER)
        )
        phase = ChestPhase.Opened
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        animatable.animate(
            composition = loaded,
            iterations = LottieConstants.IterateForever,
            clipSpec = LottieClipSpec.Marker(CHEST_OPENED_MARKER)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BaliDarkBackground)
                .background(
                    Brush.radialGradient(listOf(BaliAccentYellow.copy(alpha = 0.18f), Color.Transparent))
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LottieAnimation(
                    composition = composition,
                    progress = { animatable.progress },
                    modifier = Modifier
                        .size(300.dp)
                        .semantics {
                            contentDescription =
                                if (phase == ChestPhase.Opened) "Cofre abierto" else "Cofre sorpresa"
                        }
                )

                Spacer(modifier = Modifier.height(16.dp))

                ChestRewardSlot(isOpen = phase == ChestPhase.Opened, reward = reward)

                Spacer(modifier = Modifier.height(32.dp))

                // Present from the start so the layout is stable; it only answers once the chest is open.
                Button(
                    onClick = onDismiss,
                    enabled = phase == ChestPhase.Opened,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("RECOGER", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

/**
 * The strip under the chest: a waiting message while it opens, then the prize popping in. Its
 * height is fixed so nothing jumps when the prize appears.
 *
 * @param isOpen whether the chest is open and the prize may show
 * @param reward coins or an inventory item granted by the chest
 */
@Composable
private fun ChestRewardSlot(isOpen: Boolean, reward: ChestReward) {
    Box(modifier = Modifier.height(112.dp), contentAlignment = Alignment.Center) {
        if (!isOpen) {
            Text(
                text = "Abriendo cofre…",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn() + scaleIn(
                initialScale = 0.4f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        ) {
            ChestRewardReveal(reward = reward)
        }
    }
}

/**
 * Uses a distinct motion language for coins, practice aids and rare boosters.
 *
 * @param reward reward whose artwork and motion should be shown
 */
@Composable
private fun ChestRewardReveal(reward: ChestReward) {
    val transition = rememberInfiniteTransition(label = "chest_reward")
    val rotation = transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "coin_swing"
    )
    val bob = transition.animateFloat(
        initialValue = 3f,
        targetValue = -9f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "utility_bob"
    )
    val pulse = transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "booster_pulse"
    )
    val artModifier = when (reward) {
        is ChestReward.Coins -> Modifier.graphicsLayer { rotationZ = rotation.value }
        is ChestReward.Inventory -> when (reward.item) {
            ShopInventoryItem.HINT, ShopInventoryItem.FIFTY_FIFTY ->
                Modifier.graphicsLayer { translationY = bob.value }
            ShopInventoryItem.DOUBLE_XP, ShopInventoryItem.DOUBLE_COINS ->
                Modifier.graphicsLayer {
                    scaleX = pulse.value
                    scaleY = pulse.value
                    rotationZ = rotation.value / 2f
                }
        }
    }

    Column(
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = rewardImage(reward)),
                contentDescription = null,
                modifier = artModifier.size(56.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = rewardTitle(reward),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = BaliAccentYellow
            )
        }
        Text(
            text = rewardMessage(reward),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
    }
}

/** Returns the drawable that visually represents [reward]. */
private fun rewardImage(reward: ChestReward): Int = when (reward) {
    is ChestReward.Coins -> R.drawable.coin
    is ChestReward.Inventory -> when (reward.item) {
        ShopInventoryItem.HINT -> R.drawable.shop_hint
        ShopInventoryItem.FIFTY_FIFTY -> R.drawable.shop_fifty_fifty
        ShopInventoryItem.DOUBLE_XP -> R.drawable.shop_double_xp
        ShopInventoryItem.DOUBLE_COINS -> R.drawable.shop_double_coins
    }
}

/** Returns the short, prominent label displayed beside [reward]. */
private fun rewardTitle(reward: ChestReward): String = when (reward) {
    is ChestReward.Coins -> "+${reward.amount}"
    is ChestReward.Inventory -> when (reward.item) {
        ShopInventoryItem.HINT -> "Pista ×${reward.quantity}"
        ShopInventoryItem.FIFTY_FIFTY -> "50/50 ×${reward.quantity}"
        ShopInventoryItem.DOUBLE_XP -> "Doble XP"
        ShopInventoryItem.DOUBLE_COINS -> "Doble moneda"
    }
}

/** Returns the accessible result sentence announced when [reward] appears. */
private fun rewardMessage(reward: ChestReward): String = when (reward) {
    is ChestReward.Coins -> "¡Has ganado ${reward.amount} monedas!"
    is ChestReward.Inventory -> when (reward.item) {
        ShopInventoryItem.HINT -> "¡Has encontrado una pista!"
        ShopInventoryItem.FIFTY_FIFTY -> "¡Has encontrado un 50/50!"
        ShopInventoryItem.DOUBLE_XP -> "¡Recompensa rara: doble XP!"
        ShopInventoryItem.DOUBLE_COINS -> "¡Recompensa rara: doble de monedas!"
    }
}
