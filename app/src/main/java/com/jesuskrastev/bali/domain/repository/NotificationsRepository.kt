package com.jesuskrastev.bali.domain.repository

import com.jesuskrastev.bali.domain.model.NotificationCategory
import com.jesuskrastev.bali.domain.model.StudySchedule
import kotlinx.coroutines.flow.Flow

/**
 * Push notifications. The app never schedules a notification itself: it asks for permission
 * and describes the user through tags, and the messages (who, what and when) are set up in
 * the OneSignal dashboard (D-010 in the project decisions).
 */
interface NotificationsRepository {

    /** The study moment saved on this device; null until the user picks one in onboarding. */
    val studySchedule: Flow<StudySchedule?>

    /**
     * Keeps the study moment on the device, so it can be sent again once the install is
     * linked to an account: linking to an account that already exists drops every tag set
     * before signing in.
     *
     * @param schedule the moment the user picked
     */
    suspend fun saveStudySchedule(schedule: StudySchedule)

    /**
     * Whether Android currently lets this app show notifications at all.
     *
     * @return true when a push would be shown
     */
    fun isPermissionGranted(): Boolean

    /**
     * Whether the user left a category's notification channel on. Each category has its own
     * channel in Android's settings, and a push sent to a channel the user turned off is
     * never shown; the app can read that choice but only the user can change it.
     *
     * @param category the kind of notification to look up
     * @return false when the user turned the channel off; true otherwise, also below Android 8,
     *   which has no channels
     */
    fun isCategoryEnabled(category: NotificationCategory): Boolean

    /**
     * Asks Android for permission to show notifications and lets pushes through when it is
     * granted. The system dialog only exists from Android 13; below that the permission is
     * already granted and no dialog appears.
     *
     * @param openSettingsIfBlocked when Android will not show the dialog again (the user said
     *   no twice), take the user to the app's notification settings instead of doing nothing
     * @return true when notifications can be shown
     */
    suspend fun requestPermission(openSettingsIfBlocked: Boolean = false): Boolean

    /**
     * Keeps pushes off for a user who said no inside the app. Needed below Android 13, where
     * no system dialog asks and notifications would otherwise arrive anyway.
     */
    fun optOut()

    /**
     * Links this install to an account, so messages reach the person on whichever device
     * they use.
     *
     * @param userId the signed-in user's id, or null to unlink after signing out
     */
    fun identify(userId: String?)

    /**
     * Writes the user's tags.
     *
     * @param tags values keyed by tag name; a null value removes that tag
     */
    fun updateTags(tags: Map<String, String?>)
}
