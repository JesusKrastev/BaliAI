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
     * The categories the user switched off in Settings, or null while they never touched the
     * switches. Null is not the same as "none": it keeps whatever the account already has in
     * OneSignal, so a reinstall does not turn back on what the user had silenced.
     */
    val disabledCategories: Flow<Set<NotificationCategory>?>

    /**
     * Switches one category on or off, keeping every other category as it is.
     *
     * @param category the kind of notification to change
     * @param enabled false to stop receiving it
     */
    suspend fun setCategoryEnabled(category: NotificationCategory, enabled: Boolean)

    /**
     * Whether Android currently lets this app show notifications.
     *
     * @return true when a push would be shown
     */
    fun isPermissionGranted(): Boolean

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
