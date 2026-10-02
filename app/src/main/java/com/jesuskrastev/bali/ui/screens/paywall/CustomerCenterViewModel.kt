package com.jesuskrastev.bali.ui.screens.paywall

import androidx.lifecycle.ViewModel
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.customercenter.CustomerCenterManagementOption
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Backs the "Gestionar suscripción" destination: relays what the user does inside RevenueCat's
 * Customer Center to analytics, above all the answer to the cancellation survey, so every
 * cancellation started from the app leaves a reason behind.
 */
@HiltViewModel
class CustomerCenterViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    /** Listener handed to [CustomerCenterLauncher]; the callbacks it doesn't override do nothing. */
    val listener: CustomerCenterListener = object : CustomerCenterListener {
        override fun onManagementOptionSelected(action: CustomerCenterManagementOption) {
            analyticsTracker.customerCenterOptionSelected(analyticsNameOf(action))
        }

        override fun onFeedbackSurveyCompleted(feedbackSurveyOptionId: String) {
            analyticsTracker.subscriptionCancelReason(feedbackSurveyOptionId)
        }

        override fun onShowingManageSubscriptions() {
            analyticsTracker.subscriptionManagementOpened()
        }
    }
}

/**
 * Maps a Customer Center option to the stable name reported to analytics.
 *
 * @param action the option the user picked
 * @return `cancel`, `missing_purchase`, `custom_url`, `custom_action`, or `other` for any
 *   option added by a newer SDK
 */
internal fun analyticsNameOf(action: CustomerCenterManagementOption): String = when (action) {
    CustomerCenterManagementOption.Cancel -> "cancel"
    CustomerCenterManagementOption.MissingPurchase -> "missing_purchase"
    is CustomerCenterManagementOption.CustomUrl -> "custom_url"
    is CustomerCenterManagementOption.CustomAction -> "custom_action"
    else -> "other"
}
