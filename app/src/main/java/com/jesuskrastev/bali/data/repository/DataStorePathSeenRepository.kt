package com.jesuskrastev.bali.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jesuskrastev.bali.domain.repository.PathSeenRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pathSeenDataStore by preferencesDataStore(name = "path_seen")

/**
 * [PathSeenRepository] kept in DataStore as one `userId:order` entry. Storing the owner next to
 * the number means signing in with another account on the same phone reads as "never seen"
 * (silent) instead of animating the other account's progress.
 */
@Singleton
class DataStorePathSeenRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : PathSeenRepository {

    override suspend fun lastSeenFrontier(userId: String): Int? {
        val stored = context.pathSeenDataStore.data.first()[KEY_FRONTIER] ?: return null
        val owner = stored.substringBeforeLast(SEPARATOR)
        val order = stored.substringAfterLast(SEPARATOR, "").toIntOrNull()
        return if (owner == userId) order else null
    }

    override suspend fun markSeen(userId: String, frontierOrder: Int) {
        context.pathSeenDataStore.edit { preferences ->
            preferences[KEY_FRONTIER] = "$userId$SEPARATOR$frontierOrder"
        }
    }

    private companion object {
        const val SEPARATOR = ":"
        val KEY_FRONTIER = stringPreferencesKey("seen_frontier")
    }
}
