package com.jesuskrastev.bali.domain.repository

import com.jesuskrastev.bali.domain.model.User
import kotlinx.coroutines.flow.Flow
import com.revenuecat.purchases.CustomerInfo

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
}
