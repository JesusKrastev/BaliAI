package com.jesuskrastev.bali.data.migration.migrations

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.perf.metrics.AddTrace
import com.jesuskrastev.bali.BuildConfig
import com.jesuskrastev.bali.domain.migration.FirestoreMigration
import com.jesuskrastev.bali.domain.model.DailyStreak
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Migrates the daily streak into the seven-level speedometer model.
 *
 * Existing counts are capped at [DailyStreak.MAX_LEVEL] and a settlement marker is seeded from
 * the last practice day, preventing historical missed days from being charged repeatedly.
 */
class MigrationV8ToV9 @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreMigration {

    override val targetVersion: Int = 9
    override val description: String = "Convertir la racha diaria al velocímetro de siete niveles"

    /**
     * Caps the legacy streak fields and seeds their single-settlement marker for [userId].
     *
     * @param userId identifier of the Firestore profile to migrate
     */
    @AddTrace(name = "firestore_migration_streak_speedometer")
    override suspend fun migrate(userId: String) {
        val userRef = firestore.collection("env")
            .document(BuildConfig.BUILD_TYPE)
            .collection("users")
            .document(userId)
        val snapshot = userRef.get().await()
        val current = (snapshot.getLong("currentStreak") ?: 0L).toInt()
        val highest = (snapshot.getLong("highestStreak") ?: 0L).toInt()
        val lastPractice = snapshot.getLong("lastPracticeTimestamp") ?: 0L

        val cappedCurrent = current.coerceIn(0, DailyStreak.MAX_LEVEL)
        userRef.update(
            mapOf(
                "currentStreak" to cappedCurrent,
                "highestStreak" to highest.coerceIn(0, DailyStreak.MAX_LEVEL).coerceAtLeast(cappedCurrent),
                "lastStreakSettledDayMillis" to lastPractice
            )
        ).await()
    }
}
