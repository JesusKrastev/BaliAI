package com.jesuskrastev.bali.domain.repository

import kotlinx.coroutines.flow.Flow
import com.jesuskrastev.bali.domain.model.PremiumSubscription
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering

interface SubscriptionRepository {
    /**
     * Hot flow that emits CustomerInfo updates dynamically (e.g., when a purchase happens).
     */
    fun customerInfoStream(): Flow<CustomerInfo>

    /**
     * Fetches the latest CustomerInfo.
     */
    suspend fun getCustomerInfo(): Result<CustomerInfo>

    /**
     * Restores previous purchases.
     */
    suspend fun restorePurchases(): Result<CustomerInfo>

    /**
     * Helper to verify if the 'premium' entitlement is active on the given [CustomerInfo].
     */
    fun hasPremiumEntitlement(customerInfo: CustomerInfo): Boolean

    /**
     * When the user first bought premium, for messages meant for their first days as a
     * subscriber.
     *
     * @param customerInfo the customer info to read
     * @return the original purchase time in millis, or null when premium is not active
     */
    fun premiumSinceMillis(customerInfo: CustomerInfo): Long?

    /**
     * Reads the active premium entitlement as a plan summary.
     *
     * @param customerInfo the customer info to read
     * @return the plan, or null when premium is not active
     */
    fun premiumSubscription(customerInfo: CustomerInfo): PremiumSubscription?

    /**
     * Fetches a specific offering by its RevenueCat identifier, for paywalls that are not the
     * current/default offering (e.g. a win-back offer). Success wraps null when no offering
     * with that identifier exists.
     */
    suspend fun getOffering(identifier: String): Result<Offering?>
}
