package com.jesuskrastev.bali.data.local.room

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jesuskrastev.bali.data.local.room.dao.TestResultDao
import com.jesuskrastev.bali.data.local.room.entities.TestResultEntity
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
class TestResultDaoTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    private lateinit var database: BaliDatabase
    private lateinit var testResultDao: TestResultDao

    @Before
    fun init() {
        hiltRule.inject()
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BaliDatabase::class.java
        ).allowMainThreadQueries().build()
        testResultDao = database.testResultDao()
    }

    @After
    fun cleanup() {
        database.close()
    }

    @Test
    fun insertTestResultAndRetrieveIt() = runTest {
        val result = TestResultEntity(
            id = "result_1",
            category = "Spanish",
            score = 8,
            total = 10,
            timestamp = System.currentTimeMillis(),
            isPassed = true
        )

        testResultDao.insert(result)
        val retrieved = testResultDao.get().first()

        assertThat(retrieved).hasSize(1)
        assertThat(retrieved[0].score).isEqualTo(8)
    }

    @Test
    fun countReturnsCorrectNumberOfResults() = runTest {
        val result1 = TestResultEntity(
            id = "result_1",
            category = "Spanish",
            score = 8,
            total = 10,
            timestamp = System.currentTimeMillis(),
            isPassed = true
        )

        val result2 = TestResultEntity(
            id = "result_2",
            category = "French",
            score = 9,
            total = 10,
            timestamp = System.currentTimeMillis() + 10,
            isPassed = true
        )

        testResultDao.insert(result1)
        testResultDao.insert(result2)
        val count = testResultDao.count().first()

        assertThat(count).isEqualTo(2)
    }

    @Test
    fun getAverageScoreCalculatesCorrectAverage() = runTest {
        testResultDao.insert(TestResultEntity(
            id = "1",
            category = "C",
            score = 8,
            total = 10,
            timestamp = System.currentTimeMillis(),
            isPassed = true
        ))

        testResultDao.insert(TestResultEntity(
            id = "2",
            category = "C",
            score = 9,
            total = 10,
            timestamp = System.currentTimeMillis() + 10,
            isPassed = true
        ))

        // (8/10 + 9/10) / 2 = (80% + 90%) / 2 = 85.0%
        val average = testResultDao.getAverageScore().first()
        assertThat(average).isEqualTo(85.0)
    }

    @Test
    fun getRecentReturnsLast10Results() = runTest {
        repeat(15) { i ->
            testResultDao.insert(TestResultEntity(
                id = "result_$i",
                category = "Cat",
                score = 8,
                total = 10,
                timestamp = System.currentTimeMillis() + i,
                isPassed = true
            ))
        }

        val recent = testResultDao.getRecent().first()
        assertThat(recent).hasSize(10)
    }

    @Test
    fun clearRemovesAllResults() = runTest {
        testResultDao.insert(TestResultEntity(
            id = "result_1",
            category = "C",
            score = 8,
            total = 10,
            timestamp = System.currentTimeMillis(),
            isPassed = true
        ))

        testResultDao.clear()
        val all = testResultDao.get().first()

        assertThat(all).isEmpty()
    }
}
