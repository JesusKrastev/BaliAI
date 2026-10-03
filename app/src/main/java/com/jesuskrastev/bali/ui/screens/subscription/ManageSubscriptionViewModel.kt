package com.jesuskrastev.bali.ui.screens.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.domain.model.PremiumSubscription
import com.jesuskrastev.bali.domain.repository.SubscriptionRepository
import com.revenuecat.purchases.CustomerInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** What [ManageSubscriptionScreen] shows in its plan card. */
sealed interface ManageSubscriptionUiState {
    data object Loading : ManageSubscriptionUiState

    /**
     * Premium is active; [headline] is the status chip, [detail] the line under it and
     * [productId] the store product, used to reopen it in Google Play.
     */
    data class Active(
        val headline: String,
        val detail: String,
        val isCancelled: Boolean,
        val productId: String? = null
    ) : ManageSubscriptionUiState

    data object Inactive : ManageSubscriptionUiState

    /** The status couldn't be read (offline); the user can still start the cancellation flow. */
    data object Unavailable : ManageSubscriptionUiState
}

/**
 * Backs the "Gestionar suscripción" screen: reads the plan's status from RevenueCat and keeps it
 * live as the customer info changes. Cancelling goes through [CancelSubscriptionViewModel].
 */
@HiltViewModel
class ManageSubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ManageSubscriptionUiState>(ManageSubscriptionUiState.Loading)
    val uiState: StateFlow<ManageSubscriptionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            subscriptionRepository.getCustomerInfo().fold(
                onSuccess = ::publish,
                onFailure = { _uiState.value = ManageSubscriptionUiState.Unavailable }
            )
        }
        viewModelScope.launch {
            subscriptionRepository.customerInfoStream()
                .catch { }
                .collect(::publish)
        }
    }

    private fun publish(customerInfo: CustomerInfo) {
        _uiState.value = describeSubscription(subscriptionRepository.premiumSubscription(customerInfo))
    }
}

private val DATE_FORMAT = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es", "ES"))

/**
 * Turns the entitlement into the copy shown on the plan card.
 *
 * @param subscription the active premium entitlement, or null when there is none
 * @param zone zone used to print dates
 * @return [ManageSubscriptionUiState.Inactive] for null, otherwise an
 *   [ManageSubscriptionUiState.Active] whose wording depends on trial, cancellation and expiry
 */
internal fun describeSubscription(
    subscription: PremiumSubscription?,
    zone: ZoneId = ZoneId.systemDefault()
): ManageSubscriptionUiState {
    if (subscription == null) return ManageSubscriptionUiState.Inactive
    val productId = subscription.productId
    val date = subscription.endsAtMillis?.let { DATE_FORMAT.format(Instant.ofEpochMilli(it).atZone(zone)) }
    return when {
        date == null -> ManageSubscriptionUiState.Active("Activa", "Tu acceso a Bali no caduca.", false, productId)
        !subscription.willRenew -> ManageSubscriptionUiState.Active(
            "Cancelada",
            "Seguirás teniendo acceso completo hasta el $date. No se te volverá a cobrar.",
            true,
            productId
        )
        subscription.isTrial -> ManageSubscriptionUiState.Active(
            "Prueba gratis",
            "Tu prueba termina el $date y después empieza el cobro. Cancela antes si no quieres seguir.",
            false,
            productId
        )
        else -> ManageSubscriptionUiState.Active("Activa", "Se renueva el $date.", false, productId)
    }
}
