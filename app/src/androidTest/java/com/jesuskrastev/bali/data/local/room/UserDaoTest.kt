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
            energy = 5,
            lastEnergyUpdateTimestamp = System.currentTimeMillis(),
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
            energy = 5,
            currentStreak = 10
        )

        userDao.insert(user)
        userDao.updateXp(200, 6)

        val retrieved = userDao.get().first()
        assertThat(retrieved?.xp).isEqualTo(200)
        assertThat(retrieved?.level).isEqualTo(6)
    }

    @Test
    fun userCoinsCanBeUpdated() = runTest {
        val user = UserEntity(
            id = "user_123",
            coins = 50
        )

        userDao.insert(user)
        userDao.updateCoins(100)

        val retrieved = userDao.get().first()
        assertThat(retrieved?.coins).isEqualTo(100)
    }

    @Test
    fun userStreakCanBeReset() = runTest {
        val user = UserEntity(
            id = "user_123",
            currentStreak = 10
        )

        userDao.insert(user)
        userDao.resetStreak()

        val retrieved = userDao.get().first()
        assertThat(retrieved?.currentStreak).isEqualTo(0)
    }
}
