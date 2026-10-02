package com.jesuskrastev.bali.ui.screens.store

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onBackClick: () -> Unit,
    viewModel: ShopViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()
    var infoItem by remember { mutableStateOf<ShopItem?>(null) }

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
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            ShopSection(title = "Racha") {
                ShopItemGroup {
                    val freezerLimitReached = uiState.streakFreezes >= DailyStreak.MAX_FREEZES
                    ShopItemRow(
                        imageRes = R.drawable.streak_freezer,
                        title = "Congelador",
                        infoDescription = "Más información sobre ${shopItemTitle(ShopItem.StreakFreezer)}",
                        subtitle = "Tienes ${uiState.streakFreezes} de ${DailyStreak.MAX_FREEZES}",
                        price = STREAK_FREEZER_PRICE,
                        isEnabled = !freezerLimitReached,
                        hasEnoughCoins = uiState.coinsCount >= STREAK_FREEZER_PRICE,
                        priceLabel = if (freezerLimitReached) "Máximo" else null,
                        onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.StreakFreezer)) },
                        onInfoClick = { infoItem = ShopItem.StreakFreezer }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 92.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                    val recoverable = uiState.recoverableStreak > 0
                    ShopItemRow(
                        imageRes = R.drawable.streak_recovery,
                        title = "Recuperador",
                        infoDescription = "Más información sobre ${shopItemTitle(ShopItem.StreakRecovery)}",
                        subtitle = if (recoverable) {
                            "Recupera ${uiState.recoverableStreak} ${if (uiState.recoverableStreak == 1) "día" else "días"}"
                        } else {
                            null
                        },
                        price = DailyStreak.RECOVERY_COST_COINS,
                        isEnabled = recoverable,
                        hasEnoughCoins = uiState.coinsCount >= DailyStreak.RECOVERY_COST_COINS,
                        onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.StreakRecovery)) },
                        onInfoClick = { infoItem = ShopItem.StreakRecovery }
                    )
                }
            }
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
                        val event = when (uiState.selectedItem!!) {
                            ShopItem.StreakFreezer -> ShopEvent.PurchaseStreakFreezer
                            ShopItem.StreakRecovery -> ShopEvent.PurchaseStreakRecovery
                        }
                        viewModel.onEvent(event)
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
                when (item) {
                    ShopItem.StreakFreezer -> {
                        Image(
                            painter = painterResource(id = R.drawable.streak_freezer),
                            contentDescription = null,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                    ShopItem.StreakRecovery -> {
                        Image(
                            painter = painterResource(id = R.drawable.streak_recovery),
                            contentDescription = null,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val title = shopItemTitle(item)
        val description = when (item) {
            ShopItem.StreakFreezer -> "Evita perder tu racha de días si un día no puedes practicar."
            ShopItem.StreakRecovery ->
                "Recupera los $recoverableStreak ${if (recoverableStreak == 1) "día" else "días"} " +
                    "de racha que perdiste ayer. Solo sirve hasta el final de hoy."
        }
        val price = when (item) {
            ShopItem.StreakFreezer -> STREAK_FREEZER_PRICE
            ShopItem.StreakRecovery -> DailyStreak.RECOVERY_COST_COINS
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
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
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
                painter = painterResource(
                    id = when (item) {
                        ShopItem.StreakFreezer -> R.drawable.streak_freezer
                        ShopItem.StreakRecovery -> R.drawable.streak_recovery
                    }
                ),
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
 * Names a product for the shop list, the info dialog and the purchase sheet.
 *
 * @param item the product
 * @return its display name
 */
fun shopItemTitle(item: ShopItem): String = when (item) {
    ShopItem.StreakFreezer -> "Congelador de racha"
    ShopItem.StreakRecovery -> "Recuperador de racha"
}

/**
 * Explains a product: what it does, its limits and its price.
 *
 * @param item the product
 * @return the explanation shown behind the info button
 */
fun shopItemInfo(item: ShopItem): String = when (item) {
    ShopItem.StreakFreezer ->
        "Si un día no estudias, se gasta un congelador y tu racha sigue como si hubieras " +
            "estudiado. Puedes tener hasta ${DailyStreak.MAX_FREEZES} a la vez. " +
            "Cuesta $STREAK_FREEZER_PRICE monedas."
    ShopItem.StreakRecovery ->
        "Si fallas un día sin congelador y pierdes la racha, aquí la recuperas entera. " +
            "Solo está disponible hasta el final del día siguiente a perderla; después se " +
            "pierde para siempre. Cuesta ${DailyStreak.RECOVERY_COST_COINS} monedas."
}

/** Price in coins of one streak freezer; the purchase itself is charged in [ShopViewModel]. */
private const val STREAK_FREEZER_PRICE = 120
