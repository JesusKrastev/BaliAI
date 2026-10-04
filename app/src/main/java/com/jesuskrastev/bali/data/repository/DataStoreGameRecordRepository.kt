package com.jesuskrastev.bali.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jesuskrastev.bali.domain.repository.GameRecord
import com.jesuskrastev.bali.domain.repository.GameRecordRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.gameRecordDataStore by preferencesDataStore(name = "game_records")

/** [GameRecordRepository] kept in DataStore as two integers per game: `<id>_best` and `<id>_runs`. */
@Singleton
class DataStoreGameRecordRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : GameRecordRepository {

    override suspend fun record(gameId: String): GameRecord {
        val preferences = context.gameRecordDataStore.data.first()
        return GameRecord(
            bestScore = preferences[bestKey(gameId)] ?: 0,
            runsPlayed = preferences[runsKey(gameId)] ?: 0,
        )
    }

    override suspend fun submitRun(gameId: String, score: Int): GameRecord {
        var previous = GameRecord()
        context.gameRecordDataStore.edit { preferences ->
            previous = GameRecord(
                bestScore = preferences[bestKey(gameId)] ?: 0,
                runsPlayed = preferences[runsKey(gameId)] ?: 0,
            )
            preferences[bestKey(gameId)] = maxOf(previous.bestScore, score)
            preferences[runsKey(gameId)] = previous.runsPlayed + 1
        }
        return previous
    }

    /** DataStore key of [gameId]'s best score. */
    private fun bestKey(gameId: String) = intPreferencesKey("${gameId}_best")

    /** DataStore key of [gameId]'s finished-run count. */
    private fun runsKey(gameId: String) = intPreferencesKey("${gameId}_runs")
}
