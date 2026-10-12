package com.jesuskrastev.bali.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.jesuskrastev.bali.domain.repository.FeedbackPromptRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.feedbackPromptDataStore by preferencesDataStore(name = "feedback_prompt")

/**
 * [FeedbackPromptRepository] kept in DataStore as two flags: the banner is pending, and it has
 * been answered. The second flag is never cleared, so the invitation is made once per install.
 */
@Singleton
class DataStoreFeedbackPromptRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : FeedbackPromptRepository {

    override val isPending: Flow<Boolean> = context.feedbackPromptDataStore.data
        .map { preferences -> preferences[KEY_PENDING] == true && preferences[KEY_ANSWERED] != true }

    override suspend fun markPending() {
        if (context.feedbackPromptDataStore.data.first()[KEY_ANSWERED] == true) return
        context.feedbackPromptDataStore.edit { preferences -> preferences[KEY_PENDING] = true }
    }

    override suspend fun dismiss() {
        context.feedbackPromptDataStore.edit { preferences ->
            preferences[KEY_PENDING] = false
            preferences[KEY_ANSWERED] = true
        }
    }

    private companion object {
        val KEY_PENDING = booleanPreferencesKey("pending")
        val KEY_ANSWERED = booleanPreferencesKey("answered")
    }
}
