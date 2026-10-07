package com.jesuskrastev.bali.ui.screens.store

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.model.DailyStreak

/**
 * Renders the coin shop and its full-screen chest-reveal overlay.
 *
 * @param onBackClick invoked when the user closes the shop
 * @param viewModel provides shop state and receives interactions
 * @return Unit; the shop UI is emitted into the current composition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onBackClick: () -> Unit,
    viewModel: ShopViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Tienda", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        Row(
                            modifier = Modifier.padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.coin),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.coinsCount.toString(),
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFACC15),
                                fontSize = 18.sp
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            // Congelación de racha Section
            ShopSection(
                title = "Congelación de racha",
                countLabel = "${uiState.streakFreezes}/2"
            ) {
                val price = ShopCatalog.STREAK_FREEZER_COST
                val hasEnoughCoins = uiState.coinsCount >= price
                val isLimitReached = uiState.streakFreezes >= 2
                
                ShopItemCard(
                    imageRes = R.drawable.streak_freezer,
                    label = "1 Día",
                    price = price,
                    isEnabled = !isLimitReached,
                    hasEnoughCoins = hasEnoughCoins,
                    priceLabel = if (isLimitReached) "Máximo" else null,
                    onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.StreakFreezer)) }
                )
            }

            StreakRecoverySection(
                recoverableStreak = uiState.recoverableStreak,
                coinsCount = uiState.coinsCount,
                onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.StreakRecovery)) }
            )

            ShopSection(
                title = "Apuesta de racha",
                countLabel = if (uiState.hasActiveStreakBet) "Activa" else ""
            ) {
                ShopItemCard(
                    imageRes = R.drawable.shop_streak_bet,
                    label = "50 → 100 monedas",
                    price = ShopCatalog.STREAK_BET_COST,
                    isEnabled = !uiState.hasActiveStreakBet,
                    hasEnoughCoins = uiState.coinsCount >= ShopCatalog.STREAK_BET_COST,
                    priceLabel = if (uiState.hasActiveStreakBet) "En curso" else null,
                    onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.StreakBet)) }
                )
                Text(
                    text = "Apuesta 50 monedas y gana 100 al completar tu próxima jornada de estudio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ShopSection(
                title = "Ayudas para practicar",
                countLabel = "${uiState.hints} pistas · ${uiState.fiftyFifties} 50/50"
            ) {
                ShopItemCard(
                    imageRes = R.drawable.shop_hint,
                    label = "Pista",
                    price = ShopCatalog.HINT_COST,
                    hasEnoughCoins = uiState.coinsCount >= ShopCatalog.HINT_COST,
                    onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.Hint)) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                ShopItemCard(
                    imageRes = R.drawable.shop_fifty_fifty,
                    label = "50/50",
                    price = ShopCatalog.FIFTY_FIFTY_COST,
                    hasEnoughCoins = uiState.coinsCount >= ShopCatalog.FIFTY_FIFTY_COST,
                    onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.FiftyFifty)) }
                )
            }

            ShopSection(
                title = "Recompensas",
                countLabel = "x2 XP: ${uiState.doubleXpBoosts} · x2 monedas: ${uiState.doubleCoinBoosts}"
            ) {
                ShopItemCard(
                    imageRes = R.drawable.shop_surprise_chest,
                    label = "Cofre sorpresa",
                    price = ShopCatalog.SURPRISE_CHEST_COST,
                    hasEnoughCoins = uiState.coinsCount >= ShopCatalog.SURPRISE_CHEST_COST,
                    onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.SurpriseChest)) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                ShopItemCard(
                    imageRes = R.drawable.shop_double_xp,
                    label = "Doble XP",
                    price = ShopCatalog.DOUBLE_XP_COST,
                    hasEnoughCoins = uiState.coinsCount >= ShopCatalog.DOUBLE_XP_COST,
                    onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.DoubleXp)) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                ShopItemCard(
                    imageRes = R.drawable.shop_double_coins,
                    label = "Doble monedas",
                    price = ShopCatalog.DOUBLE_COINS_COST,
                    hasEnoughCoins = uiState.coinsCount >= ShopCatalog.DOUBLE_COINS_COST,
                    onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.DoubleCoins)) }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

            if (uiState.selectedItem != null) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.onEvent(ShopEvent.DismissSelection) },
                    sheetState = sheetState,
                    containerColor = Color(0xFF1E2530),
                    contentColor = Color.White
                ) {
                    PurchaseConfirmationContent(
                        item = uiState.selectedItem!!,
                        recoverableStreak = uiState.recoverableStreak,
                        isProcessing = uiState.isProcessing,
                        onConfirm = {
                            viewModel.onEvent(ShopEvent.ConfirmPurchase)
                        }
                    )
                }
            }
        }

        uiState.chestOpening?.let { chest ->
            SurpriseChestOpeningOverlay(
                chest = chest,
                onTap = { viewModel.onEvent(ShopEvent.AdvanceChestOpening) },
                onOpeningAnimationFinished = {
                    viewModel.onEvent(ShopEvent.ChestOpeningAnimationFinished)
                }
            )
        }
    }
}

/**
 * Shows the complete, tap-driven surprise-chest reveal over the shop.
 *
 * @param chest purchased reward and the reveal stage to render
 * @param onTap advances the reveal when the current stage accepts input
 * @param onOpeningAnimationFinished makes the reward-discovery tap available after Lottie completes
 * @return Unit; the full-screen chest experience is emitted into the current composition.
 */
@Composable
private fun SurpriseChestOpeningOverlay(
    chest: ChestOpening,
    onTap: () -> Unit,
    onOpeningAnimationFinished: () -> Unit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.bali_chest_opening))
    val openingAnimation = animateLottieCompositionAsState(
        composition = composition,
        isPlaying = chest.step == ChestOpeningStep.OPENING,
        iterations = 1
    )
    val shakeTransition = rememberInfiniteTransition(label = "surprise_chest_shake")
    val shakeOffset by shakeTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 75),
            repeatMode = RepeatMode.Reverse
        ),
        label = "surprise_chest_offset"
    )
    val canAdvance = chest.step != ChestOpeningStep.OPENING
    val isOpened = chest.step == ChestOpeningStep.AWAITING_REWARD ||
        chest.step == ChestOpeningStep.REWARD_REVEALED
    val animationProgress = when {
        chest.step == ChestOpeningStep.OPENING -> openingAnimation.progress
        isOpened -> CHEST_OPENED_PROGRESS
        else -> CHEST_CLOSED_PROGRESS
    }

    BackHandler(enabled = true) { }

    LaunchedEffect(chest.step, openingAnimation.isAtEnd) {
        if (chest.step == ChestOpeningStep.OPENING && openingAnimation.isAtEnd) {
            onOpeningAnimationFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
            .clickable(
                enabled = canAdvance,
                onClickLabel = when (chest.step) {
                    ChestOpeningStep.AWAITING_OPEN -> "Abrir cofre"
                    ChestOpeningStep.AWAITING_REWARD -> "Descubrir recompensa"
                    ChestOpeningStep.REWARD_REVEALED -> "Volver a la tienda"
                    ChestOpeningStep.OPENING -> null
                },
                onClick = onTap
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (chest.step) {
                    ChestOpeningStep.AWAITING_OPEN -> "¡TU COFRE ESTÁ LISTO!"
                    ChestOpeningStep.OPENING -> "ABRIENDO..."
                    ChestOpeningStep.AWAITING_REWARD -> "¡EL COFRE ESTÁ ABIERTO!"
                    ChestOpeningStep.REWARD_REVEALED -> "¡HAS CONSEGUIDO!"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            LottieAnimation(
                composition = composition,
                progress = { animationProgress },
                modifier = Modifier
                    .size(280.dp)
                    .offset(x = if (chest.step == ChestOpeningStep.AWAITING_OPEN) shakeOffset.dp else 0.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            if (chest.step == ChestOpeningStep.REWARD_REVEALED) {
                Image(
                    painter = painterResource(id = R.drawable.coin),
                    contentDescription = "Monedas conseguidas",
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "+${chest.reward} monedas",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFACC15),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            Text(
                text = when (chest.step) {
                    ChestOpeningStep.AWAITING_OPEN -> "Toca el cofre para abrirlo"
                    ChestOpeningStep.OPENING -> ""
                    ChestOpeningStep.AWAITING_REWARD -> "Toca para descubrir tu recompensa"
                    ChestOpeningStep.REWARD_REVEALED -> "Toca para volver a la tienda"
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.76f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private const val CHEST_CLOSED_PROGRESS = 0f
private const val CHEST_OPENED_PROGRESS = 0.62f

/**
 * The streak recovery on sale: it is only available the day after a streak was lost.
 *
 * @param recoverableStreak days the recovery would bring back, 0 when there is nothing to recover
 * @param coinsCount the user's coin balance
 * @param onClick opens the purchase sheet
 */
@Composable
private fun StreakRecoverySection(recoverableStreak: Int, coinsCount: Int, onClick: () -> Unit) {
    val recoverable = recoverableStreak > 0
    ShopSection(title = "Recuperador de racha", countLabel = "") {
        ShopItemCard(
            imageRes = R.drawable.streak_recovery,
            label = if (recoverable) {
                "Recupera $recoverableStreak ${if (recoverableStreak == 1) "día" else "días"}"
            } else {
                "Sin racha perdida"
            },
            price = DailyStreak.RECOVERY_COST_COINS,
            isEnabled = recoverable,
            hasEnoughCoins = coinsCount >= DailyStreak.RECOVERY_COST_COINS,
            priceLabel = if (recoverable) null else "—",
            onClick = onClick
        )
        Text(
            text = "Si fallas un día sin congelador, aquí recuperas la racha entera hasta el " +
                "final del día siguiente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Bottom sheet that confirms buying [item].
 *
 * @param item what is being bought
 * @param recoverableStreak days a streak recovery would bring back, only used for that item
 * @param isProcessing true while the purchase is in flight
 * @param onConfirm invoked by the confirm button
 */
@Composable
fun PurchaseConfirmationContent(
    item: ShopItem,
    recoverableStreak: Int = 0,
    isProcessing: Boolean,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon
        Surface(
            modifier = Modifier.size(100.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF28313D)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = item.imageRes),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val title = when (item) {
            ShopItem.StreakFreezer -> "Congelador de racha"
            ShopItem.StreakRecovery -> "Recuperador de racha"
            ShopItem.StreakBet -> "Apuesta de racha"
            ShopItem.Hint -> "Pista"
            ShopItem.FiftyFifty -> "50/50"
            ShopItem.SurpriseChest -> "Cofre sorpresa"
            ShopItem.DoubleXp -> "Doble XP"
            ShopItem.DoubleCoins -> "Doble monedas"
        }
        val description = when (item) {
            ShopItem.StreakFreezer -> "Protege tu racha si un día no practicas."
            ShopItem.StreakRecovery ->
                "Recupera tus $recoverableStreak ${if (recoverableStreak == 1) "día" else "días"} " +
                    "de racha perdidos ayer. Solo hasta el final de hoy."
            ShopItem.StreakBet -> "Apuesta 50 monedas y recibe 100 si completas tu próxima jornada de estudio."
            ShopItem.Hint -> "Muestra la explicación antes de responder, en una práctica."
            ShopItem.FiftyFifty -> "Descarta las opciones incorrectas y deja solo dos."
            ShopItem.SurpriseChest -> "Ábrelo por 60 monedas y gana entre 30 y 120."
            ShopItem.DoubleXp -> "Duplica el XP de tu próxima actividad."
            ShopItem.DoubleCoins -> "Duplica las monedas de tu próxima actividad."
        }
        val price = when (item) {
            ShopItem.StreakFreezer -> ShopCatalog.STREAK_FREEZER_COST
            ShopItem.StreakRecovery -> DailyStreak.RECOVERY_COST_COINS
            ShopItem.StreakBet -> ShopCatalog.STREAK_BET_COST
            ShopItem.Hint -> ShopCatalog.HINT_COST
            ShopItem.FiftyFifty -> ShopCatalog.FIFTY_FIFTY_COST
            ShopItem.SurpriseChest -> ShopCatalog.SURPRISE_CHEST_COST
            ShopItem.DoubleXp -> ShopCatalog.DOUBLE_XP_COST
            ShopItem.DoubleCoins -> ShopCatalog.DOUBLE_COINS_COST
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onConfirm,
            enabled = !isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "CONFIRMAR COMPRA",
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Image(
                        painter = painterResource(id = R.drawable.coin),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = price.toString(),
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

/** Returns the drawable used consistently in an item card and its confirmation sheet. */
private val ShopItem.imageRes: Int
    get() = when (this) {
        ShopItem.StreakFreezer -> R.drawable.streak_freezer
        ShopItem.StreakRecovery -> R.drawable.streak_recovery
        ShopItem.StreakBet -> R.drawable.shop_streak_bet
        ShopItem.Hint -> R.drawable.shop_hint
        ShopItem.FiftyFifty -> R.drawable.shop_fifty_fifty
        ShopItem.SurpriseChest -> R.drawable.shop_surprise_chest
        ShopItem.DoubleXp -> R.drawable.shop_double_xp
        ShopItem.DoubleCoins -> R.drawable.shop_double_coins
    }

@Composable
fun ShopSection(
    title: String,
    countLabel: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            if (countLabel.isNotEmpty()) {
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        content()
    }
}

@Composable
fun ShopItemCard(
    imageRes: Int? = null,
    icon: ImageVector? = null,
    iconColor: Color = Color.Unspecified,
    label: String,
    price: Int,
    priceLabel: String? = null,
    isEnabled: Boolean = true,
    hasEnoughCoins: Boolean = true,
    onClick: () -> Unit
) {
    val canBuy = isEnabled && hasEnoughCoins
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        onClick = onClick,
        enabled = canBuy
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = if (canBuy) 1f else 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageRes != null) {
                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            alpha = if (canBuy) 1f else 0.4f
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (canBuy) iconColor else iconColor.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (canBuy) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (priceLabel == null) {
                    Image(
                        painter = painterResource(id = R.drawable.coin),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        alpha = if (canBuy) 1f else 0.4f
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = priceLabel ?: price.toString(),
                    fontWeight = FontWeight.Black,
                    color = if (canBuy) Color(0xFFFFA500) else Color.Gray,
                    fontSize = 18.sp
                )
            }
        }
    }
}
