package com.jesuskrastev.bali.domain.usecase

import com.jesuskrastev.bali.domain.model.NotificationCategory
import com.jesuskrastev.bali.domain.model.StudySchedule
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.repository.NotificationsRepository
import com.jesuskrastev.bali.domain.repository.SubscriptionRepository
import com.jesuskrastev.bali.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Keeps OneSignal's picture of the user in step with the app, so the segments and journeys
 * set up in the OneSignal dashboard can choose who gets which reminder without the app
 * scheduling anything.
 *
 * It also owns linking OneSignal to the account: tags set before linking are dropped when the
 * account already exists in OneSignal, so linking and re-sending the tags have to happen in
 * that order, in one place.
 *
 * The free OneSignal plan keeps at most 6 tags per user. This sends the 6 in [Companion], so a
 * new tag means dropping one (the notification categories share [TAG_NOTIFICATIONS_OFF] for
 * that reason).
 */
class SyncNotificationTagsUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val notificationsRepository: NotificationsRepository
) {

    /**
     * What RevenueCat reported about premium.
     *
     * @property sinceMillis when premium was first bought, or null when it is not active
     */
    private data class Premium(val sinceMillis: Long?)

    /**
     * Mirrors the account and the tags into OneSignal for as long as the caller's scope lives.
     * Suspends forever; launch it and cancel the scope to stop.
     */
    suspend operator fun invoke() {
        var linkedUserId: String? = null
        combine(
            authRepository.currentUserFlow,
            userRepository.get(),
            premium(),
            notificationsRepository.studySchedule,
            notificationsRepository.disabledCategories
        ) { userId, user, premium, schedule, disabled ->
            userId to notificationTags(user, premium?.sinceMillis, premiumKnown = premium != null, schedule, disabled)
        }
            .distinctUntilChanged()
            .collect { (userId, tags) ->
                if (userId != linkedUserId) {
                    notificationsRepository.identify(userId)
                    linkedUserId = userId
                }
                notificationsRepository.updateTags(tags)
            }
    }

    /**
     * Streams the premium state, starting with null ("not known yet") so the other tags and
     * the account link do not wait on RevenueCat.
     *
     * @return null until RevenueCat answers, then its latest answer
     */
    private fun premium(): Flow<Premium?> = flow {
        subscriptionRepository.getCustomerInfo().getOrNull()?.let { emit(it) }
        emitAll(subscriptionRepository.customerInfoStream())
    }
        .map { Premium(subscriptionRepository.premiumSinceMillis(it)) }
        .onStart<Premium?> { emit(null) }

    companion object {
        /** `morning`, `noon`, `afternoon` or `night`: the hour the study reminder goes out. */
        const val TAG_STUDY_SLOT = "study_slot"

        /** `daily`, `often` or `whenever`: the days a study reminder is due. */
        const val TAG_STUDY_RHYTHM = "study_rhythm"

        /** Unix seconds of the first premium purchase; absent when not subscribed. */
        const val TAG_PREMIUM_SINCE = "premium_since"

        /** Unix seconds of the first study session of the user's latest study day. */
        const val TAG_LAST_SESSION_AT = "last_session_at"

        /** Consecutive days with study, the same streak Home shows. */
        const val TAG_STREAK_DAYS = "streak_days"

        /**
         * The notification categories the user switched off in Settings, as `study`, `promos`
         * or `study,promos` ([NotificationCategory.offTagValue]); absent when none is off. The
         * OneSignal journeys skip whoever has their category here.
         */
        const val TAG_NOTIFICATIONS_OFF = "notif_off"

        /**
         * Builds the tags to write. A tag is only removed when its absence is known to be
         * true: a missing profile or an unanswered RevenueCat call leaves the tag as it is,
         * so a slow network never drops a subscriber from the paying segments.
         *
         * @param user the profile, or null while it is loading
         * @param premiumSinceMillis when premium was first bought, or null when not subscribed
         * @param premiumKnown false while RevenueCat has not answered yet
         * @param schedule the study moment saved on this device, or null if never answered
         * @param disabledCategories what the user switched off in Settings, or null if they never
         *   opened the switches (the tag is then left as the account has it)
         * @return values keyed by tag name; a null value removes that tag
         */
        fun notificationTags(
            user: User?,
            premiumSinceMillis: Long?,
            premiumKnown: Boolean,
            schedule: StudySchedule?,
            disabledCategories: Set<NotificationCategory>? = null
        ): Map<String, String?> = buildMap {
            disabledCategories?.let { put(TAG_NOTIFICATIONS_OFF, NotificationCategory.offTagValue(it)) }
            // Never removed when missing: a reinstall starts without it, and the answer the
            // account already has in OneSignal is better than none.
            schedule?.let {
                put(TAG_STUDY_SLOT, it.slot.tag)
                it.rhythm?.let { rhythm -> put(TAG_STUDY_RHYTHM, rhythm.tag) }
            }
            if (premiumKnown) put(TAG_PREMIUM_SINCE, premiumSinceMillis?.let(::unixSeconds))
            user?.let {
                it.lastPracticeTimestamp.takeIf { millis -> millis > 0 }?.let { millis ->
                    put(TAG_LAST_SESSION_AT, unixSeconds(millis))
                }
                put(TAG_STREAK_DAYS, it.currentStreak.toString())
            }
        }

        /**
         * Formats a time the way OneSignal's time-elapsed segment filters read it.
         *
         * @param millis epoch time in milliseconds
         * @return whole seconds since the epoch, as text
         */
        private fun unixSeconds(millis: Long): String = (millis / 1000).toString()
    }
}
