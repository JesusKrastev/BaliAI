package com.jesuskrastev.bali.domain.repository

import com.jesuskrastev.bali.domain.model.EnablePushesResult
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
     * Whether a push sent to this install would be shown: Android lets the app show
     * notifications and the install has not opted out (the onboarding's "Ahora no" opts out,
     * which below Android 13 is the only thing that stops them). Emits again whenever either
     * changes, including when the user comes back from the system settings.
     */
    val pushesAllowed: Flow<Boolean>

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
     * @return true when notifications can be shown
     */
    suspend fun requestPermission(): Boolean

    /**
     * Lets pushes through again for a user who asks for them later, from Settings: opts the
     * install back in when Android already allows notifications, and otherwise shows the system
     * dialog if Android still will. It never shows a dialog of its own.
     *
     * @return what happened; [EnablePushesResult.NEEDS_SYSTEM_SETTINGS] leaves it to the caller
     *   to take the user to the system settings
     */
    suspend fun enablePushes(): EnablePushesResult

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
