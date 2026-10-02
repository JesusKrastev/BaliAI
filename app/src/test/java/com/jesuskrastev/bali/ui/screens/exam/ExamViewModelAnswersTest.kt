package com.jesuskrastev.bali.ui.screens.exam

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.model.AnswerMode
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import com.jesuskrastev.bali.domain.util.QuestionId
import com.jesuskrastev.bali.ui.screens.auth.FakeTestResultRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.ui.screens.test.TestSummary
import com.jesuskrastev.bali.util.FakeSoundEffects
import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.util.RecordingAnswerRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

/** What [ExamViewModel] saves for each answer when the official exam is finished. */
class ExamViewModelAnswersTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val answers = RecordingAnswerRepository()

    /**
     * Builds the ViewModel restored mid-exam on a one-question exam whose right answer is
     * option 1. Restoring skips the Gemini call, so no network is needed.
     */
    private fun createViewModelOnOneQuestion(): ExamViewModel {
        val userRepository = FakeUserRepository()
        val farFuture = System.currentTimeMillis() + 30 * 60 * 1000
        val session = """
            {"questions":[{"text":"¿Pregunta del examen?","options":["A","B","C"],"correctAnswerIndex":1,"explanation":"Porque sí"}],
             "currentQuestionIndex":0,"selectedAnswers":{},"isAnswerChecked":false,"sessionStreak":0,
             "examEndAtMillis":$farFuture}
        """.trimIndent()
        return ExamViewModel(
            userRepository = userRepository,
            testResultRepository = FakeTestResultRepository(),
            answerRepository = answers,
            gemini = mock(),
            incrementStreakUseCase = IncrementStreakUseCase(userRepository),
            incrementXpUseCase = IncrementXpUseCase(userRepository),
            incrementCoinsUseCase = IncrementCoinsUseCase(userRepository),
            analytics = mock<AnalyticsTracker>(),
            soundEffects = FakeSoundEffects(),
            savedStateHandle = SavedStateHandle(mapOf("exam_saved_session" to session))
        )
    }

    @Test
    fun `an exam answer is saved with its question id, the official exam mode and no topic`() {
        val exam = createViewModelOnOneQuestion()
        exam.onEvent(ExamEvent.SelectOption(1))

        runBlocking {
            val finished = CompletableDeferred<TestSummary>()
            exam.onEvent(ExamEvent.FinishExam { finished.complete(it) })
            withTimeout(10_000) { finished.await() }
        }

        val saved = answers.inserted.single()
        assertThat(saved.questionId).isEqualTo(QuestionId.of("¿Pregunta del examen?"))
        assertThat(saved.mode).isEqualTo(AnswerMode.OFFICIAL_EXAM)
        assertThat(saved.topic).isNull()
        assertThat(saved.isCorrect).isTrue()
    }
}
