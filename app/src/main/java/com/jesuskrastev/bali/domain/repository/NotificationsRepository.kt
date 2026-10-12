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
     * The kinds of notification the user switched off inside the app, or null on a device where
     * they never touched the switches (then nothing is written to OneSignal for them). Each kind
     * is sent to OneSignal in the `notif_off` tag, which the dashboard's journeys exclude, so a
     * switched-off kind is never sent at all.
     */
    val disabledCategories: Flow<Set<NotificationCategory>?>

    /**
     * Remembers whether the user wants a kind of notification.
     *
     * @param category the kind of notification the switch controls
     * @param enabled true to receive it, false to switch it off
     */
    suspend fun setCategoryEnabled(category: NotificationCategory, enabled: Boolean)

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
