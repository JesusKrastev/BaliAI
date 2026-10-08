package com.jesuskrastev.bali.ui.screens.test

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.usecase.CompleteFirstStepUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import com.jesuskrastev.bali.domain.util.PendingFirstStepRewards
import com.jesuskrastev.bali.ui.screens.auth.FakePathRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.ui.screens.exam.ExamEvent
import com.jesuskrastev.bali.ui.screens.exam.ExamViewModel
import com.jesuskrastev.bali.util.FakeSoundEffects
import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.util.RecordingAnswerRepository
import com.jesuskrastev.bali.util.StaticTestResultRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import java.util.Date

/**
 * What a finished lesson or exam reports about the results saved before it: the first win, the
 * record and the experience the level bar fills from.
 */
class ResultMilestonesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun saved(category: String, score: Int, total: Int) = TestResult(
        category = category, score = score, total = total, date = Date(), isPassed = score * 100 / total >= 90
    )

    /** A lesson from the question bank, answered right in full when [allRight] and left blank otherwise. */
    private fun finishLesson(earlier: List<TestResult>, allRight: Boolean): TestSummary {
        val userRepository = FakeUserRepository()
        val viewModel = TestViewModel(
            userRepository = userRepository,
            testResultRepository = StaticTestResultRepository(earlier),
            answerRepository = RecordingAnswerRepository(),
            gemini = mock(),
            incrementStreakUseCase = IncrementStreakUseCase(userRepository),
            incrementXpUseCase = IncrementXpUseCase(userRepository),
            incrementCoinsUseCase = IncrementCoinsUseCase(userRepository),
            completeFirstStepUseCase = CompleteFirstStepUseCase(userRepository, PendingFirstStepRewards()),
            pathRepository = FakePathRepository(),
            analytics = mock<AnalyticsTracker>(),
            soundEffects = FakeSoundEffects(),
            savedStateHandle = SavedStateHandle()
        )
        viewModel.setAiNodeParams("Conductor", "Factores", "node_0_0_lesson", "LESSON")
        if (allRight) {
            val questions = viewModel.uiState.value.questions
            questions.forEachIndexed { index, question ->
                viewModel.onEvent(TestEvent.SelectOption(question.correctAnswerIndex))
                viewModel.onEvent(TestEvent.CheckAnswer)
                if (index < questions.lastIndex) viewModel.onEvent(TestEvent.NextQuestion)
            }
        }
        return runBlocking {
            val finished = CompletableDeferred<TestSummary>()
            viewModel.onEvent(TestEvent.FinishTest { finished.complete(it) })
            withTimeout(10_000) { finished.await() }
        }
    }

    /** The one-question exam of the answers test, whose right answer is option 1, so it always fails. */
    private fun finishExam(earlier: List<TestResult>): TestSummary {
        val userRepository = FakeUserRepository()
        val farFuture = System.currentTimeMillis() + 30 * 60 * 1000
        val session = """
            {"questions":[{"text":"¿Pregunta del examen?","options":["A","B","C"],"correctAnswerIndex":1,"explanation":"Porque sí"}],
             "currentQuestionIndex":0,"selectedAnswers":{},"isAnswerChecked":false,"sessionStreak":0,
             "examEndAtMillis":$farFuture}
        """.trimIndent()
        val exam = ExamViewModel(
            userRepository = userRepository,
            testResultRepository = StaticTestResultRepository(earlier),
            answerRepository = RecordingAnswerRepository(),
            gemini = mock(),
            incrementStreakUseCase = IncrementStreakUseCase(userRepository),
            incrementXpUseCase = IncrementXpUseCase(userRepository),
            incrementCoinsUseCase = IncrementCoinsUseCase(userRepository),
            analytics = mock<AnalyticsTracker>(),
            soundEffects = FakeSoundEffects(),
            savedStateHandle = SavedStateHandle(mapOf("exam_saved_session" to session))
        )
        exam.onEvent(ExamEvent.SelectOption(1))
        return runBlocking {
            val finished = CompletableDeferred<TestSummary>()
            exam.onEvent(ExamEvent.FinishExam { finished.complete(it) })
            withTimeout(10_000) { finished.await() }
        }
    }

    @Test
    fun `a lesson won with nothing saved is the first win`() {
        assertThat(finishLesson(earlier = emptyList(), allRight = true).isFirstWin).isTrue()
    }

    @Test
    fun `a lesson lost with nothing saved is not the first win`() {
        assertThat(finishLesson(earlier = emptyList(), allRight = false).isFirstWin).isFalse()
    }

    @Test
    fun `a lesson won after an earlier win is not the first win again`() {
        val earlier = listOf(saved("Conductor", score = 9, total = 10))
        assertThat(finishLesson(earlier, allRight = true).isFirstWin).isFalse()
    }

    @Test
    fun `a lesson won after only losses is still the first win`() {
        val earlier = listOf(saved("Conductor", score = 2, total = 10))
        assertThat(finishLesson(earlier, allRight = true).isFirstWin).isTrue()
    }

    @Test
    fun `a lesson is never a record`() {
        assertThat(finishLesson(emptyList(), allRight = true).isNewRecord).isFalse()
    }

    @Test
    fun `the lesson reports the experience the user has after it`() {
        val summary = finishLesson(emptyList(), allRight = true)
        assertThat(summary.newTotalXp).isAtLeast(summary.xpGained)
    }

    @Test
    fun `the first exam is not a record`() {
        val summary = finishExam(earlier = emptyList())
        assertThat(summary.isNewRecord).isFalse()
        assertThat(summary.previousBestScore).isEqualTo(-1)
    }

    @Test
    fun `an exam that beats the best earlier one is a record, even when it is a fail`() {
        val earlier = listOf(saved(ExamRules.OFFICIAL_EXAM_CATEGORY, score = 0, total = 30))
        val summary = finishExam(earlier)
        assertThat(summary.isFailedExam).isTrue()
        assertThat(summary.isNewRecord).isTrue()
        assertThat(summary.previousBestScore).isEqualTo(0)
    }

    @Test
    fun `an exam that does not beat the best earlier one is not a record`() {
        val earlier = listOf(saved(ExamRules.OFFICIAL_EXAM_CATEGORY, score = 20, total = 30))
        val summary = finishExam(earlier)
        assertThat(summary.isNewRecord).isFalse()
        assertThat(summary.previousBestScore).isEqualTo(20)
    }

    @Test
    fun `lesson results do not count towards the exam record`() {
        val earlier = listOf(saved("Conductor", score = 10, total = 10))
        assertThat(finishExam(earlier).isNewRecord).isFalse()
    }
}
