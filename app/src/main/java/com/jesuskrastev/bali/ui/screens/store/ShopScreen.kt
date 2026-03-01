package com.jesuskrastev.bali.ui.screens.store

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
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
import com.jesuskrastev.bali.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onBackClick: () -> Unit,
    viewModel: ShopViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()

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
            // Congelación de racha Section
            ShopSection(
                title = "Congelación de racha",
                countLabel = "${uiState.streakFreezes}/2"
            ) {
                val price = 120
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

            // Energía Section
            ShopSection(
                title = "Energía",
                countLabel = ""
            ) {
                val price = 35
                val isEnergyFull = uiState.energyCount >= 5
                val hasEnoughCoins = uiState.coinsCount >= price
                
                ShopItemCard(
                    icon = Icons.Rounded.Bolt,
                    iconColor = Color(0xFFFACC15),
                    label = "+1 Energía",
                    price = price,
                    isEnabled = !isEnergyFull,
                    hasEnoughCoins = hasEnoughCoins,
                    priceLabel = if (isEnergyFull) "Lleno" else null,
                    onClick = { viewModel.onEvent(ShopEvent.SelectItem(ShopItem.EnergyRefill)) }
                )
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
                    onConfirm = {
                        val event = when (uiState.selectedItem!!) {
                            ShopItem.StreakFreezer -> ShopEvent.PurchaseStreakFreezer
                            ShopItem.EnergyRefill -> ShopEvent.PurchaseEnergyRefill
                        }
                        viewModel.onEvent(event)
                    }
                )
            }
        }
    }
}

@Composable
fun PurchaseConfirmationContent(
    item: ShopItem,
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
                    ShopItem.EnergyRefill -> {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFFACC15),
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val title = when (item) {
            ShopItem.StreakFreezer -> "Congelador de racha"
            ShopItem.EnergyRefill -> "Recarga de energía"
        }
        val description = when (item) {
            ShopItem.StreakFreezer -> "Evita perder tu racha de días si un día no puedes practicar."
            ShopItem.EnergyRefill -> "Añade +1 a tu energía al instante para seguir practicando."
        }
        val price = when (item) {
            ShopItem.StreakFreezer -> 120
            ShopItem.EnergyRefill -> 35
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
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
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
