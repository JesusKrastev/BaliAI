package com.jesuskrastev.bali.data.local.room

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.data.local.room.entities.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The coin shop's storage: `MIGRATION_18_19` must leave `users` exactly as Room creates it for
 * version 19, and the atomic statements behind purchases, boosts and the streak bet must charge
 * and pay exactly once. Nothing else exercises either without a device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShopInventoryRoomTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: BaliDatabase

    private val shopColumns =
        setOf("hints", "fiftyFifties", "doubleXpBoosts", "doubleCoinBoosts", "streakBetTarget")

    @Before
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(context, BaliDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    /** One column as `PRAGMA table_info` reports it. */
    private data class Column(val type: String, val notNull: Boolean, val default: String?, val primaryKey: Int)

    /** Reads the `users` table's columns by name. */
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
     * Builds the `users` table of version 18: the current one without the shop columns.
     *
     * @param current the current table's columns
     * @return a database holding that table with one user row
     */
    private fun version18Database(current: Map<String, Column>): SupportSQLiteDatabase {
        val kept = current.filterKeys { it !in shopColumns }
        val definitions = kept.map { (name, column) ->
            buildString {
                append("$name ${column.type}")
                if (column.notNull) append(" NOT NULL")
                column.default?.let { append(" DEFAULT $it") }
            }
        } + "PRIMARY KEY(id)"
        val required = kept.filter { (name, column) -> column.notNull && column.default == null && name != "id" }
            .mapValues { (_, column) -> if (column.type == "TEXT") "''" else "0" }

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE users (${definitions.joinToString(", ")})")
                        val names = listOf("id") + required.keys
                        val values = listOf("'user'") + required.values
                        db.execSQL("INSERT INTO users (${names.joinToString()}) VALUES (${values.joinToString()})")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        return helper.writableDatabase
    }

    private fun insertUser(coins: Int = 0, hints: Int = 0, doubleXp: Int = 0, bet: Int = 0) = runBlocking {
        database.userDao().insert(
            UserEntity(id = "u", coins = coins, hints = hints, doubleXpBoosts = doubleXp, streakBetTarget = bet)
        )
    }

    private fun user(): UserEntity = runBlocking { database.userDao().get().first()!! }

    @Test
    fun `the 18 to 19 migration leaves users exactly as Room creates it`() {
        val expected = usersColumns(database.openHelper.writableDatabase)
        val db = version18Database(expected)

        BaliDatabase.MIGRATION_18_19.migrate(db)

        assertThat(usersColumns(db)).isEqualTo(expected)
        assertThat(expected.keys).containsAtLeastElementsIn(shopColumns)
    }

    @Test
    fun `the migration keeps the user and starts with an empty inventory`() {
        val db = version18Database(usersColumns(database.openHelper.writableDatabase))
        db.execSQL("UPDATE users SET coins = 340, currentStreak = 9")

        BaliDatabase.MIGRATION_18_19.migrate(db)

        db.query(
            "SELECT coins, currentStreak, hints, fiftyFifties, doubleXpBoosts, doubleCoinBoosts, streakBetTarget FROM users"
        ).use {
            assertThat(it.moveToFirst()).isTrue()
            assertThat(it.getInt(0)).isEqualTo(340)
            assertThat(it.getInt(1)).isEqualTo(9)
            for (column in 2..6) assertThat(it.getInt(column)).isEqualTo(0)
        }
    }

    @Test
    fun `buying an item charges the coins and adds exactly one`() = runBlocking {
        insertUser(coins = 100)

        val bought = database.userDao().purchaseInventoryItem("HINT", 30)

        assertThat(bought).isEqualTo(1)
        assertThat(user().coins).isEqualTo(70)
        assertThat(user().hints).isEqualTo(1)
        assertThat(user().fiftyFifties).isEqualTo(0)
    }

    @Test
    fun `an item is not sold, and nothing is charged, when the balance falls short`() = runBlocking {
        insertUser(coins = 29)

        val bought = database.userDao().purchaseInventoryItem("HINT", 30)

        assertThat(bought).isEqualTo(0)
        assertThat(user().coins).isEqualTo(29)
        assertThat(user().hints).isEqualTo(0)
    }

    @Test
    fun `the chest charges its price and credits the prize in one statement`() = runBlocking {
        insertUser(coins = 100)

        assertThat(database.userDao().openSurpriseChest(cost = 60, reward = 75)).isEqualTo(1)
        assertThat(user().coins).isEqualTo(115)
        assertThat(database.userDao().openSurpriseChest(cost = 200, reward = 90)).isEqualTo(0)
        assertThat(user().coins).isEqualTo(115)
    }

    @Test
    fun `an owned item is consumed once and never goes below zero`() = runBlocking {
        insertUser(doubleXp = 1)

        assertThat(database.userDao().consumeInventoryItem("DOUBLE_XP")).isEqualTo(1)
        assertThat(database.userDao().consumeInventoryItem("DOUBLE_XP")).isEqualTo(0)
        assertThat(user().doubleXpBoosts).isEqualTo(0)
    }

    @Test
    fun `a streak bet charges its stake only when none is running`() = runBlocking {
        insertUser(coins = 200)

        assertThat(database.userDao().placeStreakBet(cost = 50, target = 10)).isEqualTo(1)
        assertThat(database.userDao().placeStreakBet(cost = 50, target = 12)).isEqualTo(0)

        assertThat(user().coins).isEqualTo(150)
        assertThat(user().streakBetTarget).isEqualTo(10)
    }

    @Test
    fun `a streak bet is not placed without enough coins`() = runBlocking {
        insertUser(coins = 49)

        assertThat(database.userDao().placeStreakBet(cost = 50, target = 10)).isEqualTo(0)

        assertThat(user().coins).isEqualTo(49)
        assertThat(user().streakBetTarget).isEqualTo(0)
    }

    @Test
    fun `a won streak bet pays once and a lost one pays nothing`() = runBlocking {
        insertUser(coins = 150, bet = 10)

        assertThat(database.userDao().claimStreakBet(payout = 100)).isEqualTo(1)
        assertThat(database.userDao().claimStreakBet(payout = 100)).isEqualTo(0)
        assertThat(user().coins).isEqualTo(250)
        assertThat(user().streakBetTarget).isEqualTo(0)

        database.userDao().placeStreakBet(cost = 50, target = 10)
        database.userDao().clearStreakBet()
        assertThat(user().coins).isEqualTo(200)
        assertThat(user().streakBetTarget).isEqualTo(0)
    }
}
