package com.jesuskrastev.bali.data.repository

import com.jesuskrastev.bali.domain.repository.SubscriptionRepository
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RevenueCatSubscriptionRepository @Inject constructor() : SubscriptionRepository {

    /** Fan-out of every customer update RevenueCat reports, shared by every collector of [customerInfoStream]. */
    private val customerInfoUpdates = MutableSharedFlow<CustomerInfo>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val isListening = AtomicBoolean(false)

    /**
     * RevenueCat keeps a single `updatedCustomerInfoListener`, so it is installed once for the
     * whole app and fanned out through [customerInfoUpdates]. A listener per collector would have
     * each new one replace the previous, and the first to stop collecting would clear them all:
     * the entry point and the paywall watch the same stream, and one of them would silently stop
     * hearing about purchases.
     */
    override fun customerInfoStream(): Flow<CustomerInfo> {
        if (isListening.compareAndSet(false, true)) {
            Purchases.sharedInstance.updatedCustomerInfoListener =
                UpdatedCustomerInfoListener { customerInfo -> customerInfoUpdates.tryEmit(customerInfo) }
        }
        return customerInfoUpdates.asSharedFlow()
    }

    override suspend fun getCustomerInfo(): Result<CustomerInfo> {
        return try {
            val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
            Result.success(customerInfo)
        } catch (e: PurchasesException) {
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restorePurchases(): Result<CustomerInfo> {
        return try {
            val customerInfo = Purchases.sharedInstance.awaitRestore()
            Result.success(customerInfo)
        } catch (e: PurchasesException) {
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun hasPremiumEntitlement(customerInfo: CustomerInfo): Boolean {
        return customerInfo.entitlements.active["premium"] != null
    }

    override suspend fun getOffering(identifier: String): Result<Offering?> {
        return try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            Result.success(offerings.getOffering(identifier))
        } catch (e: PurchasesException) {
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun identify(userId: String): Result<CustomerInfo> {
        return try {
            val purchases = Purchases.sharedInstance
            // Already linked (every launch after the first): no need to log in again, and the
            // cached customer keeps the app's start free of a network round trip.
            val customerInfo = if (purchases.appUserID == userId) {
                purchases.awaitCustomerInfo()
            } else {
                purchases.awaitLogIn(userId).customerInfo
            }
            // Attributes belong to the customer, and logging in switched customer, so the
            // PostHog link (the same uid AnalyticsTracker.identifyUser identified) is set again.
            purchases.setPostHogUserId(userId)
            Result.success(customerInfo)
        } catch (e: PurchasesException) {
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
