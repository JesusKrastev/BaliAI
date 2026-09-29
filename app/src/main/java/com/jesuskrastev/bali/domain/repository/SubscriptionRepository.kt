package com.jesuskrastev.bali.domain.repository

import com.jesuskrastev.bali.domain.model.User
import kotlinx.coroutines.flow.Flow
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
     * Fetches a specific offering by its RevenueCat identifier, for paywalls that are not the
     * current/default offering (e.g. a win-back offer). Success wraps null when no offering
     * with that identifier exists.
     */
    suspend fun getOffering(identifier: String): Result<Offering?>

    /**
     * Ties this device's customer to the app account, so the subscription follows the account
     * instead of the install and every purchase event carries the account's id.
     *
     * [userId] must be the Firebase uid: it is the id PostHog is identified with, so RevenueCat's
     * PostHog integration then attributes its events to the same person as the in-app events.
     * A purchase made anonymously, before signing in, is carried over to the account by
     * RevenueCat. Safe to call repeatedly with the same id.
     *
     * The user stays logged in to RevenueCat after signing out of the app on purpose: logging
     * out would hand the device a fresh anonymous customer with no entitlement, which is a
     * product decision (the hard paywall would show again), not an analytics one.
     *
     * @param userId Firebase uid of the signed-in user
     * @return the customer info of the account; a failure only means the link is retried on the
     *   next app launch, it never affects sign-in
     */
    suspend fun identify(userId: String): Result<CustomerInfo>
}
