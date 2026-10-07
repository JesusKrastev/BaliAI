package com.jesuskrastev.bali.domain.model

/**
 * What the user's active premium entitlement says about their plan, free of RevenueCat types so
 * the UI and its tests don't depend on the SDK.
 *
 * @property willRenew false once the user has cancelled: access lasts until [endsAtMillis]
 * @property isTrial true while inside a free trial
 * @property endsAtMillis next renewal (or end of access when [willRenew] is false), null when the
 *   entitlement never expires
 * @property sinceMillis when the user first became a subscriber
 * @property productId the store product behind the entitlement, used to open its page in Google
 *   Play; null when unknown
 */
data class PremiumSubscription(
    val willRenew: Boolean,
    val isTrial: Boolean,
    val endsAtMillis: Long?,
    val sinceMillis: Long?,
    val productId: String? = null
)
