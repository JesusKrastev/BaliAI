package com.jesuskrastev.bali.ui.screens.store

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.StreakBet
import com.jesuskrastev.bali.ui.theme.BaliAccentRed
import com.jesuskrastev.bali.ui.theme.BaliAccentYellow
import com.jesuskrastev.bali.ui.theme.BaliPrimary
import com.jesuskrastev.bali.ui.theme.BaliSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Dark slate behind the featured chest and the purchase sheet, so gold reads as treasure. */
private val ShowcaseDark = Color(0xFF1E293B)

/** Gold of the coin, for prices and the balance. */
private val CoinGold = Color(0xFFFFB020)

/** Rays drawn behind the featured chest. */
private const val CHEST_RAYS = 12

/** Height of every product tile, so the grid stays even whatever the text. */
private val TILE_HEIGHT = 208.dp

/** Height of a tile that takes a whole row. */
private val WIDE_TILE_HEIGHT = 104.dp

/**
 * What one product tile shows beyond the product itself.
 *
 * @property item the product
 * @property subtitle second line, such as how many the user owns
 * @property unavailable text shown instead of the price when it cannot be bought now, or null
 * @property ribbon short tag over the tile's corner, such as "¡Solo hoy!", or null
 * @property ribbonIsUrgent whether [ribbon] pulses in red, for something that runs out today
 */
private data class TileSpec(
    val item: ShopItem,
    val subtitle: String?,
    val unavailable: String? = null,
    val ribbon: String? = null,
    val ribbonIsUrgent: Boolean = false
)

/**
 * Renders the coin shop and sends user actions to [viewModel].
 *
 * It is laid out as a shop rather than a list: the balance always in sight, the surprise chest
 * as the featured offer, and each category as a grid of tiles with their own colour, a floating
 * picture and the price on a button. What a product does, and every limit, is still the text of
 * [shopItemEffect], one tap away behind each tile's info button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onBackClick: () -> Unit,
    viewModel: ShopViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    var infoItem by remember { mutableStateOf<ShopItem?>(null) }

    LaunchedEffect(uiState.purchaseFeedback) {
        uiState.purchaseFeedback?.let { feedback ->
            snackbarHostState.showSnackbar(feedback.message)
            viewModel.onEvent(ShopEvent.DismissFeedback)
        }
    }

    val select: (ShopItem) -> Unit = { viewModel.onEvent(ShopEvent.SelectItem(it)) }
    val showInfo: (ShopItem) -> Unit = { infoItem = it }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                PurchaseFeedbackSnackbar(
                    message = data.visuals.message,
                    isSuccess = uiState.purchaseFeedback?.isSuccess == true
                )
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tienda", fontWeight = FontWeight.Black) },
                navigationIcon = { CoinBalance(coins = uiState.coinsCount) },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            FeaturedChest(
                coins = uiState.coinsCount,
                onClick = { select(ShopItem.SurpriseChest) },
                onInfoClick = { showInfo(ShopItem.SurpriseChest) }
            )

            ShopCategory(
                title = "Racha",
                emoji = "🔥",
                tiles = streakTiles(uiState),
                coins = uiState.coinsCount,
                onSelect = select,
                onInfo = showInfo
            )

            ShopCategory(
                title = "Ayudas para practicar",
                emoji = "🧠",
                tiles = listOf(
                    TileSpec(ShopItem.Hint, ownedLabel(uiState.hints)),
                    TileSpec(ShopItem.FiftyFifty, ownedLabel(uiState.fiftyFifties))
                ),
                coins = uiState.coinsCount,
                onSelect = select,
                onInfo = showInfo
            )

            ShopCategory(
                title = "Potenciadores",
                emoji = "⚡",
                tiles = listOf(
                    TileSpec(ShopItem.DoubleXp, ownedLabel(uiState.doubleXpBoosts)),
                    TileSpec(ShopItem.DoubleCoins, ownedLabel(uiState.doubleCoinBoosts))
                ),
                coins = uiState.coinsCount,
                onSelect = select,
                onInfo = showInfo
            )

            EarnMoreBanner()

            Spacer(modifier = Modifier.height(8.dp))
        }

        infoItem?.let { item ->
            ShopItemInfoDialog(item = item, onDismiss = { infoItem = null })
        }

        uiState.chestReward?.let { reward ->
            ChestOpeningOverlay(
                reward = reward,
                onDismiss = { viewModel.onEvent(ShopEvent.DismissChest) },
                onOpening = { viewModel.onEvent(ShopEvent.ChestOpening) }
            )
        }

        uiState.selectedItem?.let { selected ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.onEvent(ShopEvent.DismissSelection) },
                sheetState = sheetState,
                containerColor = ShowcaseDark,
                contentColor = Color.White
            ) {
                PurchaseConfirmationContent(
                    item = selected,
                    recoverableStreak = uiState.recoverableStreak,
                    coins = uiState.coinsCount,
                    isProcessing = uiState.isProcessing,
                    onConfirm = { viewModel.onEvent(ShopEvent.ConfirmPurchase) }
                )
            }
        }
    }
}

/**
 * The streak tiles, with the state each one is in: the freezer at its limit, the recovery only
 * on the day after a lost streak (and marked as such), the bet only with a streak running.
 *
 * @param uiState what the shop shows
 * @return the tiles of the streak category
 */
private fun streakTiles(uiState: ShopUiState): List<TileSpec> {
    val freezerFull = uiState.streakFreezes >= DailyStreak.MAX_FREEZES
    val recoverable = uiState.recoverableStreak > 0
    return listOf(
        TileSpec(
            item = ShopItem.StreakFreezer,
            subtitle = "Tienes ${uiState.streakFreezes} de ${DailyStreak.MAX_FREEZES}",
            unavailable = if (freezerFull) "Máximo" else null
        ),
        TileSpec(
            item = ShopItem.StreakRecovery,
            subtitle = if (recoverable) {
                "Recupera ${uiState.recoverableStreak} ${if (uiState.recoverableStreak == 1) "día" else "días"}"
            } else {
                "Sin racha perdida"
            },
            unavailable = if (recoverable) null else "No disponible",
            ribbon = if (recoverable) "¡Solo hoy!" else null,
            ribbonIsUrgent = recoverable
        ),
        TileSpec(
            item = ShopItem.StreakBet,
            subtitle = when {
                uiState.hasActiveStreakBet -> "Llevas ${uiState.streakBetDaysDone} de ${StreakBet.DAYS} días"
                uiState.canBetOnStreak -> "Gana ${StreakBet.PAYOUT_COINS} en ${StreakBet.DAYS} días"
                else -> "Necesitas una racha"
            },
            unavailable = when {
                uiState.hasActiveStreakBet -> "En curso"
                !uiState.canBetOnStreak -> "No disponible"
                else -> null
            },
            ribbon = if (uiState.hasActiveStreakBet) "En curso" else null
        )
    )
}

/**
 * Says how many of a product the user already has.
 *
 * @param count how many they own
 * @return "Tienes N"
 */
private fun ownedLabel(count: Int): String = "Tienes $count"

/**
 * The balance in the top bar: a gold pill whose number counts up or down when it changes, so
 * spending or earning coins is felt.
 *
 * @param coins the balance
 */
@Composable
private fun CoinBalance(coins: Int) {
    val shown by animateIntAsState(targetValue = coins, animationSpec = tween(700), label = "shop_coins")
    Surface(
        modifier = Modifier.padding(start = 12.dp),
        shape = RoundedCornerShape(50),
        color = CoinGold.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.coin),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = shown.toString(),
                fontWeight = FontWeight.Black,
                color = CoinGold,
                fontSize = 18.sp
            )
        }
    }
}

/**
 * The shop window: the surprise chest as the featured offer, floating over turning rays of light
 * with a shine sweeping across, on a dark card so the gold stands out.
 *
 * @param coins the user's balance, to show whether the chest is affordable
 * @param onClick opens the purchase sheet
 * @param onInfoClick explains the chest
 */
@Composable
private fun FeaturedChest(coins: Int, onClick: () -> Unit, onInfoClick: () -> Unit) {
    val price = ShopCatalog.SURPRISE_CHEST_COST
    val canBuy = coins >= price
    val motion = rememberInfiniteTransition(label = "featured_chest")
    val rays by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(16_000, easing = LinearEasing)),
        label = "featured_chest_rays"
    )
    val float by motion.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1_400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "featured_chest_float"
    )
    val shine by motion.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(2_800, easing = LinearEasing)),
        label = "featured_chest_shine"
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "featured_chest_press")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interaction, indication = null, enabled = canBuy, onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = ShowcaseDark,
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(listOf(BaliSecondary, ShowcaseDark, Color(0xFF3B2A1A)))
                )
                .shineSweep(progress = { shine })
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(176.dp)
                        .padding(start = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LightRays(rotation = rays, color = BaliAccentYellow, modifier = Modifier.fillMaxSize())
                    // Shut: what is inside is the surprise. It opens, with its animation, once bought.
                    Image(
                        painter = painterResource(id = R.drawable.shop_surprise_chest_closed),
                        contentDescription = null,
                        modifier = Modifier
                            .size(112.dp)
                            .offset(y = float.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 18.dp)
                ) {
                    Surface(shape = RoundedCornerShape(50), color = BaliAccentYellow) {
                        Text(
                            text = "DESTACADO",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = BaliSecondary,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = shopItemTitle(ShopItem.SurpriseChest),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "¡Prueba tu suerte! Monedas, ayudas o potenciadores",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PricePill(price = price, canBuy = canBuy, missing = price - coins, onDark = true)
                }
            }
            InfoButton(
                item = ShopItem.SurpriseChest,
                tint = Color.White.copy(alpha = 0.7f),
                onClick = onInfoClick,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

/**
 * Rays of light turning behind a featured product.
 *
 * @param rotation current angle, in degrees
 * @param color colour of the rays
 * @param modifier layout modifier
 */
@Composable
private fun LightRays(rotation: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.rotate(rotation)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        val halfWidth = (PI / CHEST_RAYS / 2).toFloat()
        repeat(CHEST_RAYS) { index ->
            val angle = (2 * PI * index / CHEST_RAYS).toFloat()
            val path = Path().apply {
                moveTo(center.x, center.y)
                lineTo(center.x + radius * cos(angle - halfWidth), center.y + radius * sin(angle - halfWidth))
                lineTo(center.x + radius * cos(angle + halfWidth), center.y + radius * sin(angle + halfWidth))
                close()
            }
            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.45f), Color.Transparent),
                    center = center,
                    radius = radius
                )
            )
        }
        drawCircle(
            brush = Brush.radialGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent), center, radius * 0.6f),
            radius = radius * 0.6f,
            center = center
        )
    }
}

/**
 * Draws a diagonal band of light that sweeps across the content, the glint of something precious.
 *
 * @param progress where the band is, from about 0 (left) to 1 (right)
 * @return the modifier
 */
private fun Modifier.shineSweep(progress: () -> Float): Modifier = drawWithContent {
    drawContent()
    val x = size.width * progress()
    val band = size.width * 0.25f
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent),
            start = Offset(x - band, 0f),
            end = Offset(x + band, size.height)
        )
    )
}

/**
 * A category of the shop: its title and its products as a grid of two columns.
 *
 * @param title category name
 * @param emoji symbol shown before the name
 * @param tiles the products and their state
 * @param coins the user's balance, to know what is affordable
 * @param onSelect opens the purchase sheet of a product
 * @param onInfo explains a product
 */
@Composable
private fun ShopCategory(
    title: String,
    emoji: String,
    tiles: List<TileSpec>,
    coins: Int,
    onSelect: (ShopItem) -> Unit,
    onInfo: (ShopItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "$emoji  $title",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        tiles.chunked(2).forEachIndexed { rowIndex, row ->
            // An odd product out takes the whole row as a wide tile instead of leaving a hole.
            if (row.size == 1) {
                ShopTile(
                    spec = row.single(),
                    coins = coins,
                    floatPhase = rowIndex * 1.8f,
                    wide = true,
                    onClick = { onSelect(row.single().item) },
                    onInfoClick = { onInfo(row.single().item) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEachIndexed { columnIndex, spec ->
                        ShopTile(
                            spec = spec,
                            coins = coins,
                            floatPhase = (rowIndex * 2 + columnIndex) * 0.9f,
                            onClick = { onSelect(spec.item) },
                            onInfoClick = { onInfo(spec.item) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * One product of a category: a card tinted with the product's colour, its picture floating in a
 * glow, the name, a line of state and the price on a button. It sinks a little under the finger.
 * A product that cannot be bought now stays visible, dimmed, with the reason instead of the
 * price; one that is only short of coins says how many are missing.
 *
 * @param spec the product and its state
 * @param coins the user's balance
 * @param floatPhase offset of the floating motion, so the pictures do not move in step
 * @param wide whether it takes a whole row, laid out sideways
 * @param onClick opens the purchase sheet, only when it can be bought
 * @param onInfoClick explains the product; always active
 * @param modifier layout modifier
 */
@Composable
private fun ShopTile(
    spec: TileSpec,
    coins: Int,
    floatPhase: Float,
    wide: Boolean = false,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val item = spec.item
    val price = shopItemPrice(item)
    val available = spec.unavailable == null
    val canBuy = available && coins >= price
    val accent = shopItemAccent(item)

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "shop_tile_press")
    val motion = rememberInfiniteTransition(label = "shop_tile")
    val wave by motion.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2_400, easing = LinearEasing)),
        label = "shop_tile_float"
    )

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (wide) WIDE_TILE_HEIGHT else TILE_HEIGHT)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable(interactionSource = interaction, indication = null, enabled = canBuy, onClick = onClick),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, accent.copy(alpha = if (canBuy) 0.45f else 0.15f)),
            shadowElevation = if (canBuy) 3.dp else 0.dp
        ) {
            val tint = Brush.verticalGradient(
                listOf(accent.copy(alpha = if (available) 0.20f else 0.06f), Color.Transparent)
            )
            val picture: @Composable () -> Unit = {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            Brush.radialGradient(listOf(accent.copy(alpha = 0.30f), Color.Transparent)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = shopItemImage(item)),
                        contentDescription = null,
                        alpha = if (available) 1f else 0.45f,
                        modifier = Modifier
                            .size(58.dp)
                            .graphicsLayer {
                                translationY = if (available) sin(wave + floatPhase) * 4.dp.toPx() else 0f
                            }
                    )
                }
            }
            val pricePill: @Composable () -> Unit = {
                if (available) {
                    PricePill(price = price, canBuy = canBuy, missing = price - coins, onDark = false)
                } else {
                    UnavailablePill(text = spec.unavailable.orEmpty())
                }
            }
            val nameColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (available) 1f else 0.5f)
            val stateLine = if (available && !canBuy) "Te faltan ${price - coins}" else spec.subtitle.orEmpty()
            val stateColor = if (available && !canBuy) BaliAccentRed else MaterialTheme.colorScheme.onSurfaceVariant
            val stateWeight = if (available && !canBuy) FontWeight.Bold else FontWeight.Normal

            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(tint)
                        .padding(start = 12.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    picture()
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = shopItemTitle(item),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = nameColor,
                            maxLines = 2
                        )
                        Text(
                            text = stateLine,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = stateWeight,
                            color = stateColor,
                            maxLines = 2
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    pricePill()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(tint)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    picture()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = shopItemShortTitle(item),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = nameColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    Text(
                        text = stateLine,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = stateWeight,
                        color = stateColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    pricePill()
                }
            }
        }

        spec.ribbon?.let { Ribbon(text = it, urgent = spec.ribbonIsUrgent, color = accent) }

        InfoButton(
            item = item,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onInfoClick,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

/**
 * The price on a button: gold coin and number on the brand orange when affordable, a padlock on
 * grey when not.
 *
 * @param price the price in coins
 * @param canBuy whether the user can buy it now
 * @param missing coins short, used for the spoken description
 * @param onDark whether it sits on the dark featured card
 */
@Composable
private fun PricePill(price: Int, canBuy: Boolean, missing: Int, onDark: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = when {
            canBuy -> BaliPrimary
            onDark -> Color.White.copy(alpha = 0.12f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        shadowElevation = if (canBuy) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (canBuy) {
                Image(
                    painter = painterResource(id = R.drawable.coin),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Te faltan $missing monedas",
                    tint = if (onDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = price.toString(),
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                color = when {
                    canBuy -> Color.White
                    onDark -> Color.White.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * What a product that cannot be bought now shows instead of its price.
 *
 * @param text the reason, such as "Máximo"
 */
@Composable
private fun UnavailablePill(text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * A short tag on a tile's corner. An urgent one is red and pulses: it runs out today.
 *
 * @param text the tag
 * @param urgent whether it runs out today
 * @param color the tile's colour, used when it is not urgent
 */
@Composable
private fun BoxScope.Ribbon(text: String, urgent: Boolean, color: Color) {
    val pulse = rememberInfiniteTransition(label = "shop_ribbon")
    val alpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (urgent) 0.55f else 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "shop_ribbon_alpha"
    )
    Surface(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = (-4).dp, y = (-8).dp)
            .graphicsLayer { this.alpha = alpha },
        shape = RoundedCornerShape(50),
        color = if (urgent) BaliAccentRed else color,
        shadowElevation = 3.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}

/**
 * The info button of a product, which stays active even when the product cannot be bought.
 *
 * @param item the product it explains
 * @param tint colour of the icon
 * @param onClick opens the explanation
 * @param modifier layout modifier
 */
@Composable
private fun InfoButton(item: ShopItem, tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "Más información sobre ${shopItemTitle(item)}",
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** The bottom of the shop: Bali with a basket of coins, saying where more coins come from. */
@Composable
private fun EarnMoreBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CoinGold.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.bali_coins),
                contentDescription = null,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "¿Te faltan monedas?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Gánalas con tests, simulacros y minijuegos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Bottom sheet that confirms buying [item]: its picture in a glow of its colour, what it does,
 * and the balance it leaves.
 *
 * @param item what is being bought
 * @param recoverableStreak days a streak recovery would bring back, only used for that item
 * @param coins the user's balance before buying
 * @param isProcessing true while the purchase is in flight
 * @param onConfirm invoked by the confirm button
 */
@Composable
fun PurchaseConfirmationContent(
    item: ShopItem,
    recoverableStreak: Int = 0,
    coins: Int,
    isProcessing: Boolean,
    onConfirm: () -> Unit
) {
    val accent = shopItemAccent(item)
    val price = shopItemPrice(item)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .navigationBarsPadding()
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.45f), Color.Transparent)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = shopItemImage(item)),
                contentDescription = null,
                modifier = Modifier.size(84.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = shopItemTitle(item),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = purchaseDescription(item, recoverableStreak),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.08f)) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Te quedarán",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Image(
                    painter = painterResource(id = R.drawable.coin),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = (coins - price).coerceAtLeast(0).toString(),
                    fontWeight = FontWeight.Black,
                    color = CoinGold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

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

/**
 * Shows a high-contrast, celebratory confirmation for successful purchases and a clear error for
 * failed ones.
 *
 * @param message feedback text supplied by the ViewModel
 * @param isSuccess whether the purchase completed successfully
 */
@Composable
private fun PurchaseFeedbackSnackbar(message: String, isSuccess: Boolean) {
    val container = if (isSuccess) Color(0xFF14532D) else MaterialTheme.colorScheme.errorContainer
    val content = if (isSuccess) Color.White else MaterialTheme.colorScheme.onErrorContainer
    Snackbar(
        modifier = Modifier.padding(16.dp),
        containerColor = container,
        contentColor = content,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                contentDescription = if (isSuccess) "Compra completada" else "Error de compra",
                tint = if (isSuccess) Color(0xFF86EFAC) else content
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(message, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Explains what a product does, when it can be used and what it costs.
 *
 * @param item the product being explained
 * @param onDismiss closes the dialog
 */
@Composable
fun ShopItemInfoDialog(item: ShopItem, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(
                painter = painterResource(id = shopItemImage(item)),
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
        },
        title = { Text(shopItemTitle(item), fontWeight = FontWeight.Black, textAlign = TextAlign.Center) },
        text = { Text(shopItemInfo(item)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ENTENDIDO", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/**
 * The colour of a product's tile, glow and border, taken from its picture: ice for the freezer,
 * fire for the recovery, gold for coins, and so on.
 *
 * @param item the product
 * @return its accent colour
 */
fun shopItemAccent(item: ShopItem): Color = when (item) {
    ShopItem.StreakFreezer -> Color(0xFF38BDF8)
    ShopItem.StreakRecovery -> BaliPrimary
    ShopItem.StreakBet -> Color(0xFFF59E0B)
    ShopItem.Hint -> Color(0xFFEAB308)
    ShopItem.FiftyFifty -> Color(0xFFA855F7)
    ShopItem.SurpriseChest -> BaliAccentYellow
    ShopItem.DoubleXp -> Color(0xFF22C55E)
    ShopItem.DoubleCoins -> CoinGold
}

/**
 * Names a product for the info dialog, the purchase sheet and the info button's description.
 *
 * @param item the product
 * @return its full display name
 */
fun shopItemTitle(item: ShopItem): String = when (item) {
    ShopItem.StreakFreezer -> "Congelador de racha"
    ShopItem.StreakRecovery -> "Recuperador de racha"
    ShopItem.StreakBet -> "Apuesta de racha"
    ShopItem.Hint -> "Pista"
    ShopItem.FiftyFifty -> "50/50"
    ShopItem.SurpriseChest -> "Cofre sorpresa"
    ShopItem.DoubleXp -> "Doble XP"
    ShopItem.DoubleCoins -> "Doble de monedas"
}

/**
 * Names a product for the shop list, where its category title already gives the context.
 *
 * @param item the product
 * @return the short name; only the two streak products that repeat "de racha" differ from [shopItemTitle]
 */
fun shopItemShortTitle(item: ShopItem): String = when (item) {
    ShopItem.StreakFreezer -> "Congelador"
    ShopItem.StreakRecovery -> "Recuperador"
    else -> shopItemTitle(item)
}

/**
 * The picture of a product, the same in the list, the info dialog and the purchase sheet.
 *
 * @param item the product
 * @return the drawable resource id
 */
fun shopItemImage(item: ShopItem): Int = when (item) {
    ShopItem.StreakFreezer -> R.drawable.streak_freezer
    ShopItem.StreakRecovery -> R.drawable.streak_recovery
    ShopItem.StreakBet -> R.drawable.shop_streak_bet
    ShopItem.Hint -> R.drawable.shop_hint
    ShopItem.FiftyFifty -> R.drawable.shop_fifty_fifty
    ShopItem.SurpriseChest -> R.drawable.shop_surprise_chest
    ShopItem.DoubleXp -> R.drawable.shop_double_xp
    ShopItem.DoubleCoins -> R.drawable.shop_double_coins
}

/**
 * The price of a product, the single place the list, the sheet and the dialog read it from.
 *
 * @param item the product
 * @return its price in coins
 */
fun shopItemPrice(item: ShopItem): Int = when (item) {
    ShopItem.StreakFreezer -> ShopCatalog.STREAK_FREEZER_COST
    ShopItem.StreakRecovery -> DailyStreak.RECOVERY_COST_COINS
    ShopItem.StreakBet -> StreakBet.COST_COINS
    ShopItem.Hint -> ShopCatalog.HINT_COST
    ShopItem.FiftyFifty -> ShopCatalog.FIFTY_FIFTY_COST
    ShopItem.SurpriseChest -> ShopCatalog.SURPRISE_CHEST_COST
    ShopItem.DoubleXp -> ShopCatalog.DOUBLE_XP_COST
    ShopItem.DoubleCoins -> ShopCatalog.DOUBLE_COINS_COST
}

/**
 * What a product does and its limits, without the price. Every sentence has to be true of what
 * the app does: the shop must not promise more than the code delivers.
 *
 * @param item the product
 * @return the effect, in Spanish
 */
fun shopItemEffect(item: ShopItem): String = when (item) {
    ShopItem.StreakFreezer ->
        "Protege automáticamente un día sin estudiar. Se consume al usarlo y puedes guardar " +
            "hasta ${DailyStreak.MAX_FREEZES}."
    ShopItem.StreakRecovery ->
        "Recupera completa la racha que perdiste ayer. Solo puedes usarlo hasta terminar hoy."
    ShopItem.StreakBet ->
        "Apuesta ${StreakBet.COST_COINS} monedas: estudia ${StreakBet.DAYS} días más sin perder " +
            "la racha y gana ${StreakBet.PAYOUT_COINS}. Si fallas, pierdes la apuesta; los días " +
            "congelados no avanzan el reto."
    ShopItem.Hint ->
        "Muestra la explicación antes de responder. Usa una pista cada vez; solo en práctica."
    ShopItem.FiftyFifty ->
        "Descarta dos respuestas. Usa un 50/50 cada vez; solo en práctica."
    ShopItem.SurpriseChest ->
        "Consigue una recompensa: monedas (65 %), pista (14 %), 50/50 (10 %), doble XP (6 %) " +
            "o doble moneda (5 %)."
    ShopItem.DoubleXp ->
        "Duplica el XP de tu próximo test, simulacro o minijuego. Se consume al terminarlo."
    ShopItem.DoubleCoins ->
        "Duplica las monedas de tu próximo test, simulacro o minijuego. Se consume al terminarlo."
}

/**
 * Explains a product: what it does, its limits and its price.
 *
 * @param item the product
 * @return the explanation shown behind the info button
 */
fun shopItemInfo(item: ShopItem): String = "${shopItemEffect(item)} Cuesta ${shopItemPrice(item)} monedas."

/**
 * The text of the purchase sheet: the effect of the product, with the real count for the recovery.
 *
 * @param item the product being bought
 * @param recoverableStreak days a streak recovery would bring back, only used for that item
 * @return the description shown above the confirm button
 */
private fun purchaseDescription(item: ShopItem, recoverableStreak: Int): String = when (item) {
    ShopItem.StreakRecovery ->
        "Recupera los $recoverableStreak ${if (recoverableStreak == 1) "día" else "días"} " +
            "de racha que perdiste ayer. Solo sirve hasta el final de hoy."
    else -> shopItemEffect(item)
}
