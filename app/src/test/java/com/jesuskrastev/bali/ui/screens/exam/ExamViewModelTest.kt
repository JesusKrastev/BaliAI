package com.jesuskrastev.bali.ui.screens.exam

import androidx.lifecycle.SavedStateHandle
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.util.FakeSoundEffects
import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.ui.screens.auth.FakeAnswerRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeTestResultRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import org.mockito.kotlin.mock

class ExamViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeUserRepository = FakeUserRepository()
    private val fakeTestResultRepository = FakeTestResultRepository()
    private val fakeAnswerRepository = FakeAnswerRepository()

    private val fakeIncrementStreakUseCase = IncrementStreakUseCase(fakeUserRepository)
    private val fakeIncrementXpUseCase = IncrementXpUseCase(fakeUserRepository)
    private val fakeIncrementCoinsUseCase = IncrementCoinsUseCase(fakeUserRepository)

    private val fakeSoundEffects = FakeSoundEffects()

    private lateinit var viewModel: ExamViewModel

    @Before
    fun setup() {
        viewModel = createViewModel()
    }

    /**
     * Builds the ViewModel with fakes.
     *
     * @param savedStateHandle what the ViewModel restores from; empty means it generates a new exam
     */
    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) = ExamViewModel(
        userRepository = fakeUserRepository,
        testResultRepository = fakeTestResultRepository,
        answerRepository = fakeAnswerRepository,
        gemini = mock(),
        incrementStreakUseCase = fakeIncrementStreakUseCase,
        incrementXpUseCase = fakeIncrementXpUseCase,
        incrementCoinsUseCase = fakeIncrementCoinsUseCase,
        analytics = mock<AnalyticsTracker>(),
        soundEffects = fakeSoundEffects,
        savedStateHandle = savedStateHandle
    )

    /**
     * Builds a ViewModel restored mid-exam on a one-question exam whose right answer is option 1.
     * Restoring skips the Gemini call, so the exam has questions without any network.
     */
    private fun createViewModelOnOneQuestion(): ExamViewModel {
        val farFuture = System.currentTimeMillis() + 30 * 60 * 1000
        val session = """
            {"questions":[{"text":"¿Pregunta?","options":["A","B","C"],"correctAnswerIndex":1,"explanation":"Porque sí"}],
             "currentQuestionIndex":0,"selectedAnswers":{},"isAnswerChecked":false,"sessionStreak":0,
             "examEndAtMillis":$farFuture}
        """.trimIndent()
        return createViewModel(SavedStateHandle(mapOf("exam_saved_session" to session)))
    }

    @Test
    fun `uiState initializes correctly`() = runTest {
        assertThat(viewModel.uiState.value).isNotNull()
        assertThat(viewModel.uiState.value.questions).isEmpty()
    }

    @Test
    fun `timer starts at 1800 seconds for 30 minute exam`() = runTest {
        assertThat(viewModel.uiState.value.timeLeftSeconds).isEqualTo(1800)
    }

    @Test
    fun `isTimeUp is initially false`() = runTest {
        assertThat(viewModel.uiState.value.isTimeUp).isFalse()
    }

    @Test
    fun `sessionStreak is tracked correctly`() = runTest {
        assertThat(viewModel.uiState.value.sessionStreak).isAtLeast(0)
    }

    @Test
    fun `checking a right answer plays the correct sound only`() {
        val exam = createViewModelOnOneQuestion()

        exam.onEvent(ExamEvent.SelectOption(1))
        exam.onEvent(ExamEvent.CheckAnswer)

        assertThat(fakeSoundEffects.correctPlays).isEqualTo(1)
        assertThat(fakeSoundEffects.wrongPlays).isEqualTo(0)
        assertThat(exam.uiState.value.isAnswerChecked).isTrue()
    }

    @Test
    fun `checking a wrong answer plays the wrong sound only`() {
        val exam = createViewModelOnOneQuestion()

        exam.onEvent(ExamEvent.SelectOption(0))
        exam.onEvent(ExamEvent.CheckAnswer)

        assertThat(fakeSoundEffects.wrongPlays).isEqualTo(1)
        assertThat(fakeSoundEffects.correctPlays).isEqualTo(0)
    }

    @Test
    fun `checking with no option selected plays nothing`() {
        val exam = createViewModelOnOneQuestion()

        exam.onEvent(ExamEvent.CheckAnswer)

        assertThat(fakeSoundEffects.correctPlays).isEqualTo(0)
        assertThat(fakeSoundEffects.wrongPlays).isEqualTo(0)
    }

    @Test
    fun `a double tap on check plays once`() {
        val exam = createViewModelOnOneQuestion()

        exam.onEvent(ExamEvent.SelectOption(1))
        exam.onEvent(ExamEvent.CheckAnswer)
        exam.onEvent(ExamEvent.CheckAnswer)

        assertThat(fakeSoundEffects.correctPlays).isEqualTo(1)
        assertThat(exam.uiState.value.sessionStreak).isEqualTo(1)
    }
}
