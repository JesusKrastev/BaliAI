package com.jesuskrastev.bali.ui.screens.store

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
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
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.StreakBet

/** Renders the coin shop and sends user actions to [viewModel]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onBackClick: () -> Unit,
    viewModel: ShopViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }
    var infoItem by remember { mutableStateOf<ShopItem?>(null) }

    LaunchedEffect(uiState.purchaseFeedback) {
        uiState.purchaseFeedback?.let { feedback ->
            snackbarHostState.showSnackbar(feedback)
            viewModel.onEvent(ShopEvent.DismissFeedback)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
            ShopSection(title = "Racha") {
                ShopItemGroup {
                    val freezerLimitReached = uiState.streakFreezes >= DailyStreak.MAX_FREEZES
                    ShopRow(
                        item = ShopItem.StreakFreezer,
                        subtitle = "Tienes ${uiState.streakFreezes} de ${DailyStreak.MAX_FREEZES}",
                        isEnabled = !freezerLimitReached,
                        hasEnoughCoins = uiState.coinsCount >= ShopCatalog.STREAK_FREEZER_COST,
                        priceLabel = if (freezerLimitReached) "Máximo" else null,
                        onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.StreakFreezer)) },
                        onInfoClick = { infoItem = ShopItem.StreakFreezer }
                    )
                    ShopItemDivider()
                    val recoverable = uiState.recoverableStreak > 0
                    ShopRow(
                        item = ShopItem.StreakRecovery,
                        subtitle = if (recoverable) {
                            "Recupera ${uiState.recoverableStreak} ${if (uiState.recoverableStreak == 1) "día" else "días"}"
                        } else {
                            null
                        },
                        isEnabled = recoverable,
                        hasEnoughCoins = uiState.coinsCount >= DailyStreak.RECOVERY_COST_COINS,
                        onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.StreakRecovery)) },
                        onInfoClick = { infoItem = ShopItem.StreakRecovery }
                    )
                    ShopItemDivider()
                    ShopRow(
                        item = ShopItem.StreakBet,
                        subtitle = when {
                            uiState.hasActiveStreakBet ->
                                "Llevas ${uiState.streakBetDaysDone} de ${StreakBet.DAYS} días"
                            uiState.canBetOnStreak -> "Gana ${StreakBet.PAYOUT_COINS} en ${StreakBet.DAYS} días"
                            else -> "Necesitas una racha en marcha"
                        },
                        isEnabled = uiState.canBetOnStreak,
                        hasEnoughCoins = uiState.coinsCount >= StreakBet.COST_COINS,
                        priceLabel = if (uiState.hasActiveStreakBet) "En curso" else null,
                        onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.StreakBet)) },
                        onInfoClick = { infoItem = ShopItem.StreakBet }
                    )
                }
            }

            ShopSection(title = "Ayudas para practicar") {
                ShopItemGroup {
                    ShopRow(
                        item = ShopItem.Hint,
                        subtitle = "Tienes ${uiState.hints}",
                        hasEnoughCoins = uiState.coinsCount >= ShopCatalog.HINT_COST,
                        onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.Hint)) },
                        onInfoClick = { infoItem = ShopItem.Hint }
                    )
                    ShopItemDivider()
                    ShopRow(
                        item = ShopItem.FiftyFifty,
                        subtitle = "Tienes ${uiState.fiftyFifties}",
                        hasEnoughCoins = uiState.coinsCount >= ShopCatalog.FIFTY_FIFTY_COST,
                        onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.FiftyFifty)) },
                        onInfoClick = { infoItem = ShopItem.FiftyFifty }
                    )
                }
            }

            ShopSection(title = "Recompensas") {
                ShopItemGroup {
                    ShopRow(
                        item = ShopItem.SurpriseChest,
                        subtitle = "Entre ${ShopCatalog.CHEST_MIN_REWARD} y ${ShopCatalog.CHEST_MAX_REWARD} monedas",
                        hasEnoughCoins = uiState.coinsCount >= ShopCatalog.SURPRISE_CHEST_COST,
                        onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.SurpriseChest)) },
                        onInfoClick = { infoItem = ShopItem.SurpriseChest }
                    )
                    ShopItemDivider()
                    ShopRow(
                        item = ShopItem.DoubleXp,
                        subtitle = "Tienes ${uiState.doubleXpBoosts}",
                        hasEnoughCoins = uiState.coinsCount >= ShopCatalog.DOUBLE_XP_COST,
                        onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.DoubleXp)) },
                        onInfoClick = { infoItem = ShopItem.DoubleXp }
                    )
                    ShopItemDivider()
                    ShopRow(
                        item = ShopItem.DoubleCoins,
                        subtitle = "Tienes ${uiState.doubleCoinBoosts}",
                        hasEnoughCoins = uiState.coinsCount >= ShopCatalog.DOUBLE_COINS_COST,
                        onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.DoubleCoins)) },
                        onInfoClick = { infoItem = ShopItem.DoubleCoins }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        infoItem?.let { item ->
            ShopItemInfoDialog(item = item, onDismiss = { infoItem = null })
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
                    painter = painterResource(id = shopItemImage(item)),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                        text = shopItemPrice(item).toString(),
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

/**
 * A [ShopItemRow] filled in from the product itself: its picture, short name, price and info
 * button description come from the helpers below, so a product reads the same everywhere.
 *
 * @param item the product
 * @param subtitle optional second line, such as how many the user owns
 * @param isEnabled false when the product cannot be bought at all right now
 * @param hasEnoughCoins whether the user's balance covers the price
 * @param priceLabel text shown instead of the price, for example when the limit is reached
 * @param onClick invoked when the product is tapped and can be bought
 * @param onInfoClick invoked by the info button
 */
@Composable
private fun ShopRow(
    item: ShopItem,
    subtitle: String? = null,
    isEnabled: Boolean = true,
    hasEnoughCoins: Boolean = true,
    priceLabel: String? = null,
    onClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    ShopItemRow(
        imageRes = shopItemImage(item),
        title = shopItemShortTitle(item),
        infoDescription = "Más información sobre ${shopItemTitle(item)}",
        subtitle = subtitle,
        price = shopItemPrice(item),
        priceLabel = priceLabel,
        isEnabled = isEnabled,
        hasEnoughCoins = hasEnoughCoins,
        onClick = onClick,
        onInfoClick = onInfoClick
    )
}

/** Thin line between two products of the same [ShopItemGroup], aligned with their names. */
@Composable
private fun ShopItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 92.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    )
}

/**
 * A shop category: its title above the group of products it holds.
 *
 * @param title category name
 * @param content the products, usually one [ShopItemGroup]
 */
@Composable
fun ShopSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black
        )
        content()
    }
}

/**
 * Rounded container that joins the products of one category into a single card.
 *
 * @param content the [ShopItemRow]s, stacked
 */
@Composable
fun ShopItemGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(content = content)
    }
}

/**
 * One product inside a [ShopItemGroup]: picture, name, price and an info button. A product that
 * cannot be bought right now is shown greyed out with its price instead of being hidden or
 * replaced by an explanation; the explanation lives behind the info button.
 *
 * @param imageRes picture of the product
 * @param title short product name; the category title above already says it is about the streak
 * @param infoDescription spoken description of the info button
 * @param subtitle optional second line, such as how many the user owns
 * @param price price in coins
 * @param priceLabel text shown instead of the price, for example when the limit is reached
 * @param isEnabled false when the product cannot be bought at all right now
 * @param hasEnoughCoins whether the user's balance covers [price]
 * @param onClick invoked when the product is tapped and can be bought
 * @param onInfoClick invoked by the info button, which stays active on disabled products
 */
@Composable
fun ShopItemRow(
    imageRes: Int,
    title: String,
    infoDescription: String,
    subtitle: String? = null,
    price: Int,
    priceLabel: String? = null,
    isEnabled: Boolean = true,
    hasEnoughCoins: Boolean = true,
    onClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    val canBuy = isEnabled && hasEnoughCoins
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (canBuy) 1f else 0.4f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(start = 20.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(enabled = canBuy, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = if (canBuy) 1f else 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                // Smaller than the 56 dp circle on purpose: the recovery gem fills its whole canvas
                // (its corners are 0.59 of the side away from the centre) and drawn at 56 dp the
                // circle cut its edges off. At 40 dp it sits inside with a margin.
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    alpha = if (canBuy) 1f else 0.4f
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (canBuy) 1f else 0.6f)
                    )
                }
            }
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
        IconButton(onClick = onInfoClick) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = infoDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        "Si un día no estudias, se gasta un congelador y tu racha sigue como si hubieras " +
            "estudiado. Puedes tener hasta ${DailyStreak.MAX_FREEZES} a la vez."
    ShopItem.StreakRecovery ->
        "Si fallas un día sin congelador y pierdes la racha, aquí la recuperas entera. " +
            "Solo está disponible hasta el final del día siguiente a perderla; después se " +
            "pierde para siempre."
    ShopItem.StreakBet ->
        "Pagas ${StreakBet.COST_COINS} monedas y, si estudias ${StreakBet.DAYS} días más sin " +
            "perder la racha, recibes ${StreakBet.PAYOUT_COINS}. Si la pierdes antes, pierdes las " +
            "${StreakBet.COST_COINS}. Si hoy ya has estudiado, hoy no cuenta, y un día cubierto " +
            "con un congelador mantiene la racha pero no cuenta como día de estudio. Solo puedes " +
            "apostar con una racha en marcha y tener una apuesta a la vez."
    ShopItem.Hint ->
        "En un test de práctica, te enseña la explicación de la pregunta antes de que respondas. " +
            "Gasta una pista por pregunta y no se puede usar en el simulacro."
    ShopItem.FiftyFifty ->
        "En un test de práctica, deja solo dos opciones de la pregunta: la correcta y una " +
            "incorrecta. Gasta un 50/50 por pregunta y no se puede usar en el simulacro."
    ShopItem.SurpriseChest ->
        "Al abrirlo recibes entre ${ShopCatalog.CHEST_MIN_REWARD} y ${ShopCatalog.CHEST_MAX_REWARD} " +
            "monedas al azar. Cuesta ${ShopCatalog.SURPRISE_CHEST_COST}, así que según la suerte " +
            "sales ganando o perdiendo."
    ShopItem.DoubleXp ->
        "El próximo test, simulacro o minijuego que termines da el doble de XP, con sus bonus " +
            "incluidos. Se gasta al terminarlo y cada uno vale para una sola actividad."
    ShopItem.DoubleCoins ->
        "El próximo test, simulacro o minijuego que termines da el doble de monedas. Se gasta " +
            "al terminarlo y cada uno vale para una sola actividad."
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
