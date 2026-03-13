package com.jesuskrastev.bali.ui.screens.paywall

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter
@Composable
fun CustomerCenterLauncher(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    // Renders the RevenueCat Customer Center screen logic inside its own component overlay
    // Can be triggered from a button somewhere else.
    CustomerCenter(
        modifier = modifier,
        onDismiss = onDismiss
    )
}
