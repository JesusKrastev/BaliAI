package com.jesuskrastev.bali.data.local.room

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jesuskrastev.bali.data.local.room.dao.AnswerDao
import com.jesuskrastev.bali.data.local.room.entities.AnswerEntity
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
class AnswerDaoTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    private lateinit var database: BaliDatabase
    private lateinit var answerDao: AnswerDao

    @Before
    fun init() {
        hiltRule.inject()
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BaliDatabase::class.java
        ).allowMainThreadQueries().build()
        answerDao = database.answerDao()
    }

    @After
    fun cleanup() {
        database.close()
    }

    @Test
    fun insertAnswerAndRetrieveIt() = runTest {
        val answer = AnswerEntity(
            id = "answer_1",
            testId = "test_1",
            questionText = "What is 2+2?",
            selectedOption = 1,
            isCorrect = true,
            timestamp = System.currentTimeMillis()
        )

        answerDao.insert(answer)
        val retrieved = answerDao.getAll().first()

        assertThat(retrieved).hasSize(1)
        assertThat(retrieved[0].questionText).isEqualTo("What is 2+2?")
    }

    @Test
    fun getRecentMistakesReturnsOnlyWrongAnswers() = runTest {
        val correctAnswer = AnswerEntity(
            id = "answer_1",
            testId = "test_1",
            questionText = "What is 2+2?",
            selectedOption = 1,
            isCorrect = true,
            timestamp = System.currentTimeMillis()
        )

        val incorrectAnswer = AnswerEntity(
            id = "answer_2",
            testId = "test_1",
            questionText = "What is 2+3?",
            selectedOption = 1,
            isCorrect = false,
            timestamp = System.currentTimeMillis()
        )

        answerDao.insert(correctAnswer)
        answerDao.insert(incorrectAnswer)
        val incorrect = answerDao.getRecentMistakes().first()

        assertThat(incorrect).hasSize(1)
        assertThat(incorrect[0].isCorrect).isFalse()
    }

    @Test
    fun markAsCorrectedUpdatesIsCorrectStatus() = runTest {
        val incorrectAnswer = AnswerEntity(
            id = "answer_1",
            testId = "test_1",
            questionText = "Question",
            selectedOption = 1,
            isCorrect = false,
            timestamp = System.currentTimeMillis()
        )

        answerDao.insert(incorrectAnswer)
        answerDao.markAsCorrected("Question")

        val all = answerDao.getAll().first()
        assertThat(all[0].isCorrect).isTrue()
    }

    @Test
    fun clearRemovesAllAnswers() = runTest {
        val answer = AnswerEntity(
            id = "answer_1",
            testId = "test_1",
            questionText = "Q",
            selectedOption = 1,
            isCorrect = true,
            timestamp = System.currentTimeMillis()
        )

        answerDao.insert(answer)
        answerDao.clear()
        val all = answerDao.getAll().first()

        assertThat(all).isEmpty()
    }

    @Test
    fun insertAnswerKeepsItsQuestionIdTopicAndMode() = runTest {
        answerDao.insert(
            AnswerEntity(
                id = "answer_1",
                testId = "test_1",
                questionText = "Q",
                selectedOption = 1,
                isCorrect = true,
                questionId = "q_abc123def456",
                topic = "SPEED",
                mode = "LESSON"
            )
        )

        val retrieved = answerDao.getAll().first().single()

        assertThat(retrieved.questionId).isEqualTo("q_abc123def456")
        assertThat(retrieved.topic).isEqualTo("SPEED")
        assertThat(retrieved.mode).isEqualTo("LESSON")
    }

    @Test
    fun anAnswerWithoutTheNewFieldsReadsThemAsNull() = runTest {
        answerDao.insert(
            AnswerEntity(
                id = "answer_1",
                testId = "test_1",
                questionText = "Q",
                selectedOption = 1,
                isCorrect = true
            )
        )

        val retrieved = answerDao.getAll().first().single()

        assertThat(retrieved.questionId).isNull()
        assertThat(retrieved.topic).isNull()
        assertThat(retrieved.mode).isNull()
    }
}
