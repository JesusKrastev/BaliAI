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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RevenueCatSubscriptionRepository @Inject constructor() : SubscriptionRepository {

    override fun customerInfoStream(): Flow<CustomerInfo> = callbackFlow {
        val listener = UpdatedCustomerInfoListener { customerInfo ->
            trySend(customerInfo)
        }
        Purchases.sharedInstance.updatedCustomerInfoListener = listener

        awaitClose {
            Purchases.sharedInstance.updatedCustomerInfoListener = null
        }
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
}
