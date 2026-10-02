package com.jesuskrastev.bali.data.repository

import com.jesuskrastev.bali.domain.repository.SubscriptionRepository
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RevenueCatSubscriptionRepository @Inject constructor() : SubscriptionRepository {

    /** Owns the shared listener stream; lives as long as this singleton repository. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * RevenueCat keeps a single `updatedCustomerInfoListener` slot, so every collector has to
     * share one registration: with one listener per collector, the paywall replaced
     * MainViewModel's listener and nulled it on close, silencing MainViewModel for good.
     */
    private val sharedCustomerInfo: Flow<CustomerInfo> = callbackFlow {
        Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
            trySend(customerInfo)
        }
        awaitClose {
            Purchases.sharedInstance.updatedCustomerInfoListener = null
        }
    }.shareIn(scope, SharingStarted.WhileSubscribed())

    /**
     * Streams customer info updates pushed by RevenueCat.
     *
     * @return a flow that all collectors share, backed by a single RevenueCat listener
     */
    override fun customerInfoStream(): Flow<CustomerInfo> = sharedCustomerInfo

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

    /**
     * Checks for an active premium entitlement.
     *
     * @param customerInfo the customer info to read
     * @return true when premium is active
     */
    override fun hasPremiumEntitlement(customerInfo: CustomerInfo): Boolean =
        customerInfo.entitlements.active[PREMIUM_ENTITLEMENT] != null

    /**
     * Reads when the active premium entitlement was first bought.
     *
     * @param customerInfo the customer info to read
     * @return the original purchase time in millis, or null when premium is not active
     */
    override fun premiumSinceMillis(customerInfo: CustomerInfo): Long? =
        customerInfo.entitlements.active[PREMIUM_ENTITLEMENT]?.originalPurchaseDate?.time

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

    private companion object {
        /** Identifier of the entitlement every paid plan grants, as set in RevenueCat. */
        const val PREMIUM_ENTITLEMENT = "premium"
    }
}
