package com.jesuskrastev.bali.ui.screens.paywall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterOptions

/**
 * Hosts RevenueCat's Customer Center (manage or cancel the subscription, request support) so it
 * can be shown from any screen. Used by the "Gestionar suscripción" row in settings.
 *
 * @param modifier layout modifier applied to the Customer Center
 * @param listener receives the Customer Center's events (option picked, cancellation survey
 *   answered, hand-off to Google Play), or null to ignore them
 * @param onDismiss invoked when the user closes the Customer Center
 */
@Composable
fun CustomerCenterLauncher(
    modifier: Modifier = Modifier,
    listener: CustomerCenterListener? = null,
    onDismiss: () -> Unit = {}
) {
    val options = remember(listener) {
        CustomerCenterOptions.Builder().setListener(listener).build()
    }
    CustomerCenter(
        modifier = modifier,
        options = options,
        onDismiss = onDismiss
    )
}
