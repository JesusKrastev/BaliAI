package com.jesuskrastev.bali.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.perf.metrics.AddTrace
import com.jesuskrastev.bali.domain.repository.GameRecord
import com.jesuskrastev.bali.domain.repository.GameRecordRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val Context.gameRecordDataStore by preferencesDataStore(name = "game_records")

/** Device-local game records and user-scoped tutorial flags in the existing game_records store. */
@Singleton
class DataStoreGameRecordRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : GameRecordRepository {

    /** Reads [gameId]'s record on IO and returns zero values when absent. */
    @AddTrace(name = "drive_record_load")
    override suspend fun record(gameId: String): GameRecord = withContext(Dispatchers.IO) {
        val preferences = context.gameRecordDataStore.data.first()
        GameRecord(
            bestScore = preferences[bestKey(gameId)] ?: 0,
            runsPlayed = preferences[runsKey(gameId)] ?: 0,
        )
    }

    /** Atomically records [score] for [gameId] on IO and returns the previous record. */
    @AddTrace(name = "drive_record_save")
    override suspend fun submitRun(gameId: String, score: Int): GameRecord = withContext(Dispatchers.IO) {
        var previous = GameRecord()
        context.gameRecordDataStore.edit { preferences ->
            previous = GameRecord(
                bestScore = preferences[bestKey(gameId)] ?: 0,
                runsPlayed = preferences[runsKey(gameId)] ?: 0,
            )
            preferences[bestKey(gameId)] = maxOf(previous.bestScore, score)
            preferences[runsKey(gameId)] = previous.runsPlayed + 1
        }
        previous
    }

    /** Reads the device-local tutorial flag for [userId] and [gameId], false when absent. */
    @AddTrace(name = "drive_tutorial_load")
    override suspend fun tutorialCompleted(gameId: String, userId: String?): Boolean = withContext(Dispatchers.IO) {
        context.gameRecordDataStore.data.first()[tutorialKey(gameId, userId)] ?: false
    }

    /** Stores completion for [userId] and [gameId] on IO; existing records remain untouched. */
    @AddTrace(name = "drive_tutorial_save")
    override suspend fun completeTutorial(gameId: String, userId: String?) = withContext(Dispatchers.IO) {
        context.gameRecordDataStore.edit { it[tutorialKey(gameId, userId)] = true }
        Unit
    }

    /** Returns a collision-free key with separate guest and authenticated-user scopes. */
    private fun tutorialKey(gameId: String, userId: String?) =
        booleanPreferencesKey("${gameId}_tutorial_${if (userId == null) "guest" else "user_${userId.length}_$userId"}")

    /** DataStore key of [gameId]'s best score. */
    private fun bestKey(gameId: String) = intPreferencesKey("${gameId}_best")

    /** DataStore key of [gameId]'s finished-run count. */
    private fun runsKey(gameId: String) = intPreferencesKey("${gameId}_runs")
}
