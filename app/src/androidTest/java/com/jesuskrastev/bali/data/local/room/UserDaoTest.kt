package com.jesuskrastev.bali.data.local.room

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jesuskrastev.bali.data.local.room.dao.UserDao
import com.jesuskrastev.bali.data.local.room.entities.UserEntity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat

@HiltAndroidTest
class UserDaoTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    private lateinit var database: BaliDatabase
    private lateinit var userDao: UserDao

    @Before
    fun init() {
        hiltRule.inject()
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BaliDatabase::class.java
        ).allowMainThreadQueries().build()
        userDao = database.userDao()
    }

    @After
    fun cleanup() {
        database.close()
    }

    @Test
    fun insertAndGetUserReturnsSameUser() = runTest {
        val user = UserEntity(
            id = "user_123",
            name = "Test User",
            xp = 100,
            level = 5,
            coins = 50,
            currentStreak = 10,
            highestStreak = 15,
            streakFreezes = 1
        )

        userDao.insert(user)
        val retrievedUser = userDao.get().first()

        assertThat(retrievedUser).isNotNull()
        assertThat(retrievedUser?.name).isEqualTo("Test User")
        assertThat(retrievedUser?.xp).isEqualTo(100)
    }

    @Test
    fun updateUserChangesExistingRecord() = runTest {
        val user = UserEntity(
            id = "user_123",
            name = "Test User",
            xp = 100,
            level = 5,
            currentStreak = 10
        )

        userDao.insert(user)
        userDao.updateXp(200, 6)

        val retrieved = userDao.get().first()
        assertThat(retrieved?.xp).isEqualTo(200)
        assertThat(retrieved?.level).isEqualTo(6)
    }

    @Test
    fun userCoinsCanBeIncremented() = runTest {
        val user = UserEntity(
            id = "user_123",
            coins = 50
        )

        userDao.insert(user)
        userDao.incrementCoins(50)

        val retrieved = userDao.get().first()
        assertThat(retrieved?.coins).isEqualTo(100)
    }

    @Test
    fun decrementCoinsIfEnoughSucceedsAndDeductsWhenBalanceIsSufficient() = runTest {
        val user = UserEntity(
            id = "user_123",
            coins = 100
        )

        userDao.insert(user)
        val rowsUpdated = userDao.decrementCoinsIfEnough(100)

        assertThat(rowsUpdated).isEqualTo(1)
        val retrieved = userDao.get().first()
        assertThat(retrieved?.coins).isEqualTo(0)
    }

    @Test
    fun decrementCoinsIfEnoughFailsAndLeavesBalanceUntouchedWhenInsufficient() = runTest {
        val user = UserEntity(
            id = "user_123",
            coins = 50
        )

        userDao.insert(user)
        val rowsUpdated = userDao.decrementCoinsIfEnough(100)

        assertThat(rowsUpdated).isEqualTo(0)
        val retrieved = userDao.get().first()
        assertThat(retrieved?.coins).isEqualTo(50)
    }

    @Test
    fun updateStreakWritesEveryDailyStreakField() = runTest {
        userDao.insert(UserEntity(id = "user_123", currentStreak = 10, coins = 50))

        userDao.updateStreak(
            current = 4,
            highest = 12,
            freezes = 1,
            lastPracticeMillis = 3_000L,
            practiceDaysJson = "[1000,2000,3000]",
            frozenDaysJson = "[1500]"
        )

        val retrieved = userDao.get().first()
        assertThat(retrieved?.currentStreak).isEqualTo(4)
        assertThat(retrieved?.highestStreak).isEqualTo(12)
        assertThat(retrieved?.streakFreezes).isEqualTo(1)
        assertThat(retrieved?.lastPracticeTimestamp).isEqualTo(3_000L)
        assertThat(retrieved?.practiceDays).containsExactly(1000L, 2000L, 3000L).inOrder()
        assertThat(retrieved?.frozenDays).containsExactly(1500L)
        assertThat(retrieved?.coins).isEqualTo(50)
    }

    @Test
    fun aNewUserHasNoFrozenDays() = runTest {
        userDao.insert(UserEntity(id = "user_123"))

        assertThat(userDao.get().first()?.frozenDays).isEmpty()
    }

    @Test
    fun updateExamDateReplacesTheEstimateAndKeepsThePlanDate() = runTest {
        val user = UserEntity(
            id = "user_123",
            examDateMillis = 1_000L,
            planTargetMillis = 2_000L
        )

        userDao.insert(user)
        userDao.updateExamDate(3_000L)

        val retrieved = userDao.get().first()
        assertThat(retrieved?.examDateMillis).isEqualTo(3_000L)
        assertThat(retrieved?.planTargetMillis).isEqualTo(2_000L)
    }
}
