package com.jesuskrastev.bali.ui.screens.paywall

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter

/**
 * Hosts RevenueCat's Customer Center (manage or cancel the subscription, request support) so it
 * can be shown from any screen, for example from a button in settings.
 *
 * @param modifier layout modifier applied to the Customer Center
 * @param onDismiss invoked when the user closes the Customer Center
 */
@Composable
fun CustomerCenterLauncher(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    CustomerCenter(
        modifier = modifier,
        onDismiss = onDismiss
    )
}
