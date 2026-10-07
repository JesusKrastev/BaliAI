package com.jesuskrastev.bali.data.repository

import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataStoreGameRecordRepositoryTest {
    /** Actual preference bytes survive recreation, isolate users/guest and preserve record keys. */
    @Test fun `tutorial survives repository recreation without changing records or other users`() = runTest {
        val directory = File("build/drive-preferences/${UUID.randomUUID()}").apply { mkdirs() }
        val context = object : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
            /** Returns the isolated directory for this test’s persisted preference bytes. */
            override fun getFilesDir(): File = directory
            /** Returns this context so the DataStore delegate uses the isolated directory. */
            override fun getApplicationContext(): Context = this
        }
        val first = DataStoreGameRecordRepository(context)
        assertThat(first.tutorialCompleted("bali_drive", "alice")).isFalse()
        first.submitRun("bali_drive", 4200)
        first.completeTutorial("bali_drive", "alice")
        val recreated = DataStoreGameRecordRepository(context)
        assertThat(recreated.tutorialCompleted("bali_drive", "alice")).isTrue()
        assertThat(recreated.tutorialCompleted("bali_drive", "bob")).isFalse()
        assertThat(recreated.tutorialCompleted("bali_drive", null)).isFalse()
        assertThat(recreated.record("bali_drive").bestScore).isEqualTo(4200)
        assertThat(recreated.record("bali_drive").runsPlayed).isEqualTo(1)
        recreated.completeTutorial("bali_drive", null)
        assertThat(recreated.tutorialCompleted("bali_drive", "bob")).isFalse()

        // Load an independent store from the persisted bytes, avoiding two active stores per file.
        val persisted = File(directory, "datastore/game_records.preferences_pb")
        val reopenedFile = File(directory, "reopened.preferences_pb")
        persisted.copyTo(reopenedFile)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = PreferenceDataStoreFactory.create(scope = scope) { reopenedFile }
            val preferences = store.data.first()
            assertThat(preferences[booleanPreferencesKey("bali_drive_tutorial_user_5_alice")]).isTrue()
            assertThat(preferences[intPreferencesKey("bali_drive_best")]).isEqualTo(4200)
            assertThat(preferences[intPreferencesKey("bali_drive_runs")]).isEqualTo(1)
        } finally { scope.cancel() }
    }
}
