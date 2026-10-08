package com.jesuskrastev.bali.data.local.room

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.data.local.room.entities.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/** Checks the v20 upgrade and the streak bet DAO on an Android device. */
@RunWith(AndroidJUnit4::class)
class StreakBetMigrationTest {

    /** Verifies the migration keeps a user's coins and the DAO pays a bet only once. */
    @Test
    fun migrationAddsFlagAndDaoClaimsBetOnce() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val databaseName = "streak-bet-migration-test.db"
        context.deleteDatabase(databaseName)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(20) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE users (id TEXT NOT NULL PRIMARY KEY, coins INTEGER NOT NULL)")
                        db.execSQL("INSERT INTO users VALUES ('existing', 75)")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        try {
            val db = helper.writableDatabase
            BaliDatabase.MIGRATION_20_21.migrate(db)
            db.query("SELECT coins, activeStreakBet FROM users WHERE id = 'existing'").use {
                assertThat(it.moveToFirst()).isTrue()
                assertThat(it.getInt(0)).isEqualTo(75)
                assertThat(it.getInt(1)).isEqualTo(0)
            }
        } finally {
            helper.close()
            context.deleteDatabase(databaseName)
        }

        val room = Room.inMemoryDatabaseBuilder(context, BaliDatabase::class.java).build()
        try {
            val dao = room.userDao()
            dao.insert(UserEntity(id = "existing", coins = 75))
            assertThat(dao.purchaseInventoryItem("STREAK_BET", 40)).isEqualTo(1)
            assertThat(dao.claimStreakBetHead(100)).isEqualTo(1)
            assertThat(dao.claimStreakBetHead(100)).isEqualTo(0)
            assertThat(dao.get().first()?.coins).isEqualTo(135)
        } finally {
            room.close()
        }
    }
}
