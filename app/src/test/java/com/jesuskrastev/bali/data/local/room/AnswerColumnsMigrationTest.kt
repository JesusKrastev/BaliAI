package com.jesuskrastev.bali.data.local.room

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.data.local.room.entities.AnswerEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checks `MIGRATION_17_18` the way production runs it: a database file that is at version 17 is
 * opened by Room, which applies the migration and then validates the whole schema against the
 * entities. A wrong column name, type or nullability would make that validation throw here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AnswerColumnsMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "answer-columns-migration-test.db"

    @Before
    fun deleteLeftovers() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun cleanUp() {
        context.deleteDatabase(databaseName)
    }

    /**
     * Opens the test database file.
     *
     * @param withMigration whether Room is told about `MIGRATION_17_18`
     */
    private fun open(withMigration: Boolean): BaliDatabase {
        val builder = Room.databaseBuilder(context, BaliDatabase::class.java, databaseName)
            .allowMainThreadQueries()
        if (withMigration) builder.addMigrations(BaliDatabase.MIGRATION_17_18)
        return builder.build()
    }

    /**
     * Leaves a database file as version 17 had it: the current schema, except that `answers` is
     * the table without the three new columns (the shape the 8→9 migration last built), holding
     * one answer saved by that version, and `user_version` is 17.
     */
    private fun createVersion17Database() {
        val fresh = open(withMigration = false)
        val db = fresh.openHelper.writableDatabase
        db.execSQL("DROP TABLE answers")
        db.execSQL(
            """
            CREATE TABLE answers (
                id TEXT NOT NULL PRIMARY KEY,
                testId TEXT NOT NULL,
                questionText TEXT NOT NULL,
                selectedOption INTEGER NOT NULL,
                isCorrect INTEGER NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("INSERT INTO answers VALUES ('old-1', 'test-1', '¿Qué es la prioridad?', 2, 0, 1000)")
        db.execSQL("PRAGMA user_version = 17")
        fresh.close()
    }

    @Test
    fun `without the migration Room refuses to open a version 17 database`() {
        createVersion17Database()

        val failure = assertThrows(IllegalStateException::class.java) {
            open(withMigration = false).openHelper.writableDatabase
        }

        assertThat(failure).hasMessageThat().contains("migration")
    }

    @Test
    fun `the migration opens a version 17 database and Room validates the new schema`() {
        createVersion17Database()

        val database = open(withMigration = true)
        database.openHelper.writableDatabase // opening is what runs the migration and the validation

        assertThat(database.openHelper.readableDatabase.version).isEqualTo(18)
        database.close()
    }

    @Test
    fun `an answer saved before the migration survives with the new columns empty`() = runBlocking {
        createVersion17Database()

        val database = open(withMigration = true)
        val old = database.answerDao().getAll().first().single()

        assertThat(old.id).isEqualTo("old-1")
        assertThat(old.questionText).isEqualTo("¿Qué es la prioridad?")
        assertThat(old.selectedOption).isEqualTo(2)
        assertThat(old.isCorrect).isFalse()
        assertThat(old.timestamp).isEqualTo(1000L)
        assertThat(old.questionId).isNull()
        assertThat(old.topic).isNull()
        assertThat(old.mode).isNull()
        database.close()
    }

    @Test
    fun `after the migration a new answer keeps its question id, topic and mode`() = runBlocking {
        createVersion17Database()
        val database = open(withMigration = true)

        database.answerDao().insert(
            AnswerEntity(
                id = "new-1",
                testId = "test-2",
                questionText = "¿Qué es la velocidad?",
                selectedOption = 0,
                isCorrect = true,
                questionId = "q_abc123def456",
                topic = "SPEED",
                mode = "LESSON"
            )
        )

        val saved = database.answerDao().getAll().first().first { it.id == "new-1" }
        assertThat(saved.questionId).isEqualTo("q_abc123def456")
        assertThat(saved.topic).isEqualTo("SPEED")
        assertThat(saved.mode).isEqualTo("LESSON")
        database.close()
    }
}
