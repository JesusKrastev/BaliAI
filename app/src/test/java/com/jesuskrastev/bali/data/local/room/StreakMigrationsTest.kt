package com.jesuskrastev.bali.data.local.room

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checks that the streak columns added by the 15 → 16 and 16 → 17 migrations leave the `users`
 * table exactly as Room creates it for version 17. A mismatch would crash every upgrading user
 * on launch, and nothing else exercises these migrations without a device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StreakMigrationsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** One column as `PRAGMA table_info` reports it. */
    private data class Column(val type: String, val notNull: Boolean, val default: String?, val primaryKey: Int)

    /**
     * Reads the `users` table's columns.
     *
     * @param db an open database
     * @return the columns by name
     */
    private fun usersColumns(db: SupportSQLiteDatabase): Map<String, Column> =
        db.query("PRAGMA table_info(users)").use { cursor ->
            buildMap {
                while (cursor.moveToNext()) {
                    put(
                        cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        Column(
                            type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                            notNull = cursor.getInt(cursor.getColumnIndexOrThrow("notnull")) == 1,
                            default = cursor.getString(cursor.getColumnIndexOrThrow("dflt_value")),
                            primaryKey = cursor.getInt(cursor.getColumnIndexOrThrow("pk"))
                        )
                    )
                }
            }
        }

    /**
     * Builds the `users` table of an older version: the current one without [removed].
     *
     * @param current the current table's columns
     * @param removed columns that did not exist yet
     * @return a database at version 1 holding that table with one row in it
     */
    private fun oldDatabase(current: Map<String, Column>, removed: Set<String>): SupportSQLiteDatabase {
        val kept = current.filterKeys { it !in removed }
        val definitions = kept.map { (name, column) ->
            buildString {
                append("$name ${column.type}")
                if (column.notNull) append(" NOT NULL")
                column.default?.let { append(" DEFAULT $it") }
            }
        } + "PRIMARY KEY(id)"
        val row = kept.filter { (name, column) -> column.notNull && column.default == null && name != "id" }
            .mapValues { (_, column) -> if (column.type == "TEXT") "''" else "0" }

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE users (${definitions.joinToString(", ")})")
                        val names = listOf("id") + row.keys
                        val values = listOf("'user'") + row.values
                        db.execSQL("INSERT INTO users (${names.joinToString()}) VALUES (${values.joinToString()})")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        return helper.writableDatabase
    }

    private val streakColumns = setOf("lastStreakSettledDayMillis", "lostStreak", "lostStreakDayMillis")

    private fun currentUsersColumns(): Map<String, Column> {
        val room = Room.inMemoryDatabaseBuilder(context, BaliDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        return try {
            usersColumns(room.openHelper.writableDatabase)
        } finally {
            room.close()
        }
    }

    @Test
    fun `a device on version 15 reaches the schema Room expects`() {
        val expected = currentUsersColumns()
        val db = oldDatabase(expected, removed = streakColumns)

        BaliDatabase.MIGRATION_15_16.migrate(db)
        BaliDatabase.MIGRATION_16_17.migrate(db)

        assertThat(usersColumns(db)).isEqualTo(expected)
    }

    @Test
    fun `a device already on version 16 from the internal build reaches the same schema`() {
        val expected = currentUsersColumns()
        val db = oldDatabase(expected, removed = setOf("lostStreak", "lostStreakDayMillis"))

        BaliDatabase.MIGRATION_16_17.migrate(db)

        assertThat(usersColumns(db)).isEqualTo(expected)
    }

    @Test
    fun `the migrations keep the streak and start with nothing to recover`() {
        val db = oldDatabase(currentUsersColumns(), removed = streakColumns)
        db.execSQL("UPDATE users SET currentStreak = 23, highestStreak = 31")

        BaliDatabase.MIGRATION_15_16.migrate(db)
        BaliDatabase.MIGRATION_16_17.migrate(db)

        db.query("SELECT currentStreak, highestStreak, lostStreak, lostStreakDayMillis FROM users").use {
            assertThat(it.moveToFirst()).isTrue()
            assertThat(it.getInt(0)).isEqualTo(23)
            assertThat(it.getInt(1)).isEqualTo(31)
            assertThat(it.getInt(2)).isEqualTo(0)
            assertThat(it.getLong(3)).isEqualTo(0L)
        }
    }
}
