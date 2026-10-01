package com.jesuskrastev.bali.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jesuskrastev.bali.domain.model.StudyRhythm
import com.jesuskrastev.bali.domain.model.StudySchedule
import com.jesuskrastev.bali.domain.model.StudySlot
import com.jesuskrastev.bali.domain.repository.NotificationsRepository
import com.onesignal.OneSignal
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notificationsDataStore by preferencesDataStore(name = "notifications")

/**
 * [NotificationsRepository] backed by OneSignal, with the study moment kept in DataStore.
 *
 * DataStore rather than the user profile: the answer only matters on the device that
 * receives the reminder, and the local profile is wiped on sign-in.
 */
@Singleton
class OneSignalNotificationsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationsRepository {

    override val studySchedule: Flow<StudySchedule?> = context.notificationsDataStore.data
        .map { preferences ->
            StudySlot.fromTag(preferences[KEY_STUDY_SLOT])?.let { slot ->
                StudySchedule(slot, StudyRhythm.fromTag(preferences[KEY_STUDY_RHYTHM]))
            }
        }
        .distinctUntilChanged()

    /**
     * Stores [schedule], replacing any earlier answer.
     *
     * @param schedule the moment the user picked
     */
    override suspend fun saveStudySchedule(schedule: StudySchedule) {
        context.notificationsDataStore.edit { preferences ->
            preferences[KEY_STUDY_SLOT] = schedule.slot.tag
            val rhythm = schedule.rhythm
            if (rhythm != null) preferences[KEY_STUDY_RHYTHM] = rhythm.tag else preferences.remove(KEY_STUDY_RHYTHM)
        }
    }

    /**
     * Shows OneSignal's permission request and opts the push subscription back in when it is
     * granted, in case an earlier "Ahora no" opted it out.
     *
     * @return true when notifications can be shown; false when denied or the request failed
     */
    override suspend fun requestPermission(): Boolean = try {
        OneSignal.Notifications.requestPermission(fallbackToSettings = false).also { granted ->
            if (granted) OneSignal.User.pushSubscription.optIn()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        FirebaseCrashlytics.getInstance().recordException(e)
        false
    }

    /** Opts this install's push subscription out, so OneSignal sends it nothing. */
    override fun optOut() {
        OneSignal.User.pushSubscription.optOut()
    }

    /**
     * Logs OneSignal in as [userId], or out when it is null.
     *
     * @param userId the signed-in user's id, or null after signing out
     */
    override fun identify(userId: String?) {
        if (userId != null) OneSignal.login(userId) else OneSignal.logout()
    }

    /**
     * Adds the non-null entries of [tags] and removes the null ones.
     *
     * @param tags values keyed by tag name; a null value removes that tag
     */
    override fun updateTags(tags: Map<String, String?>) {
        val toAdd = tags.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
        val toRemove = tags.filterValues { it == null }.keys
        if (toAdd.isNotEmpty()) OneSignal.User.addTags(toAdd)
        if (toRemove.isNotEmpty()) OneSignal.User.removeTags(toRemove)
    }

    private companion object {
        val KEY_STUDY_SLOT = stringPreferencesKey("study_slot")
        val KEY_STUDY_RHYTHM = stringPreferencesKey("study_rhythm")
    }
}
