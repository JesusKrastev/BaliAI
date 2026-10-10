package com.jesuskrastev.bali.ui.screens.test

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerateContentResponse
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.model.NodeType
import com.jesuskrastev.bali.domain.path.DgtLearningPathTemplate
import com.jesuskrastev.bali.domain.path.LessonQuestionBank
import com.jesuskrastev.bali.domain.repository.AnswerRepository
import com.jesuskrastev.bali.domain.usecase.CompleteFirstStepUseCase
import com.jesuskrastev.bali.domain.usecase.CompletePathNodeUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import com.jesuskrastev.bali.domain.util.PendingFirstStepRewards
import com.jesuskrastev.bali.ui.screens.auth.FakeAnswerRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeTestResultRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.util.FakeImagePrefetcher
import com.jesuskrastev.bali.util.FakeSoundEffects
import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.util.RecordingPathRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.util.Date
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** A section review rewords bank questions from the lessons already done and keeps their pictures. */
class TestViewModelReviewTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val userRepository = FakeUserRepository()

    private val reviewNode: LessonNode = DgtLearningPathTemplate.buildInitialPath()
        .first { it.sectionIndex == 0 && it.nodeType == NodeType.REVIEW }

    private val sectionZeroTexts: Set<String> = DgtLearningPathTemplate.buildInitialPath()
        .filter { it.sectionIndex == 0 && it.nodeType == NodeType.LESSON }
        .flatMap { LessonQuestionBank.allQuestionsForNode(it.id) }
        .map { it.text }
        .toSet()

    private fun pathWithLessonsDone(): List<LessonNode> =
        DgtLearningPathTemplate.buildInitialPath().map { node ->
            when {
                node.sectionIndex == 0 && node.nodeType == NodeType.LESSON -> node.copy(status = NodeStatus.COMPLETED)
                node.id == reviewNode.id -> node.copy(status = NodeStatus.UNLOCKED)
                else -> node
            }
        }

    /** A Gemini reply that rewrites sources 1 to 10 and tries to sneak in a picture of its own. */
    private fun rewritesOfEverySource(): GenerateContentResponse {
        val questions = (1..10).joinToString(",") { n ->
            """{"sourceIndex":$n,"text":"Reescrita $n: ¿qué indica la señal de la imagen?",
               "options":["Opción uno $n","Opción dos $n","Opción tres $n"],"correctAnswerIndex":0,
               "explanation":"Porque lo dice la norma.","imageUrl":"https://commons.wikimedia.org/wiki/Special:FilePath/x.svg"}"""
        }
        val response: GenerateContentResponse = mock()
        whenever(response.text).thenReturn("""{"questions":[$questions]}""")
        return response
    }

    private fun createViewModel(
        path: RecordingPathRepository,
        gemini: GenerativeModel,
        images: FakeImagePrefetcher = FakeImagePrefetcher(),
        answers: AnswerRepository = FakeAnswerRepository()
    ) = TestViewModel(
        userRepository = userRepository,
        testResultRepository = FakeTestResultRepository(),
        answerRepository = answers,
        gemini = gemini,
        incrementStreakUseCase = IncrementStreakUseCase(userRepository),
        incrementXpUseCase = IncrementXpUseCase(userRepository),
        incrementCoinsUseCase = IncrementCoinsUseCase(userRepository),
        completeFirstStepUseCase = CompleteFirstStepUseCase(userRepository, PendingFirstStepRewards()),
        pathRepository = path,
        imagePrefetcher = images,
        completePathNodeUseCase = CompletePathNodeUseCase(path),
        analytics = mock<AnalyticsTracker>(),
        soundEffects = FakeSoundEffects(),
        savedStateHandle = SavedStateHandle()
    ).also { it.setAiNodeParams(reviewNode.title, reviewNode.description, reviewNode.id, "REVIEW") }

    @Test
    fun `a review has ten questions, all from lessons of its own unit`() = runTest {
        val gemini: GenerativeModel = mock()
        val reply = rewritesOfEverySource()
        whenever(gemini.generateContent(any<String>())).thenReturn(reply)

        val review = createViewModel(RecordingPathRepository(pathWithLessonsDone()), gemini)

        assertThat(review.uiState.value.error).isNull()
        assertThat(review.uiState.value.questions).hasSize(10)
    }

    @Test
    fun `a rewritten question keeps the picture of its original and never one from Gemini`() = runTest {
        val gemini: GenerativeModel = mock()
        val reply = rewritesOfEverySource()
        whenever(gemini.generateContent(any<String>())).thenReturn(reply)

        val review = createViewModel(RecordingPathRepository(pathWithLessonsDone()), gemini)

        val questions = review.uiState.value.questions
        assertThat(questions.mapNotNull { it.imageUrl }.filter { "wikimedia" in it }).isEmpty()
        // A rewrite only survives when its original has a picture (the rewrite points at it);
        // a text original whose rewrite leans on a picture falls back to the bank's own text.
        assertThat(questions.filter { it.imageUrl == null }.none { it.text.startsWith("Reescrita") }).isTrue()
        assertThat(questions.filter { it.imageUrl != null }.all { it.text.startsWith("Reescrita") }).isTrue()
        assertThat(questions.filter { it.imageUrl == null }.all { it.text in sectionZeroTexts }).isTrue()
    }

    @Test
    fun `when Gemini fails the review is the bank's own questions and still complete`() = runTest {
        val gemini: GenerativeModel = mock()

        val review = createViewModel(RecordingPathRepository(pathWithLessonsDone()), gemini)

        assertThat(review.uiState.value.error).isNull()
        assertThat(review.uiState.value.questions).hasSize(10)
        assertThat(review.uiState.value.questions.all { it.text in sectionZeroTexts }).isTrue()
    }

    @Test
    fun `a review question whose picture fails is replaced by a text one`() = runTest {
        val gemini: GenerativeModel = mock()

        val review = createViewModel(
            RecordingPathRepository(pathWithLessonsDone()),
            gemini,
            images = FakeImagePrefetcher(failEverything = true)
        )

        assertThat(review.uiState.value.questions).hasSize(10)
        assertThat(review.uiState.value.questions.mapNotNull { it.imageUrl }).isEmpty()
    }

    @Test
    fun `a review of a unit with no lesson done shows an error`() = runTest {
        val nothingDone = DgtLearningPathTemplate.buildInitialPath()
            .map { if (it.id == reviewNode.id) it.copy(status = NodeStatus.UNLOCKED) else it }

        val review = createViewModel(RecordingPathRepository(nothingDone), mock())

        assertThat(review.uiState.value.questions).isEmpty()
        assertThat(review.uiState.value.error).isNotNull()
    }

    /** An answer repository whose history is the given answers. */
    private class HistoryOf(private val history: List<Answer>) : AnswerRepository {
        override suspend fun insert(answer: Answer) {}
        override fun getRecentMistakes(): Flow<List<Answer>> = flowOf(history.filter { !it.isCorrect })
        override fun getAll(): Flow<List<Answer>> = flowOf(history)
        override suspend fun markAsCorrected(questionText: String) {}
        override suspend fun clear() {}
    }

    private fun wrong(text: String, at: Long) = Answer(
        testId = "t", questionText = text, selectedOption = 0, isCorrect = false, date = Date(at)
    )

    @Test
    fun `the review asks about the questions the student got wrong and nothing else`() = runTest {
        val sectionQuestions = DgtLearningPathTemplate.buildInitialPath()
            .filter { it.sectionIndex == 0 && it.nodeType == NodeType.LESSON }
            .flatMap { LessonQuestionBank.allQuestionsForNode(it.id) }
        val mistakes = sectionQuestions.take(3).map { it.text }

        val review = createViewModel(
            RecordingPathRepository(pathWithLessonsDone()),
            mock(),
            answers = HistoryOf(mistakes.mapIndexed { i, text -> wrong(text, at = i.toLong()) })
        )

        assertThat(review.uiState.value.questions.map { it.text }).containsExactlyElementsIn(mistakes)
    }

    @Test
    fun `a mistake the student answered right later is not in the review`() = runTest {
        val sectionQuestions = DgtLearningPathTemplate.buildInitialPath()
            .filter { it.sectionIndex == 0 && it.nodeType == NodeType.LESSON }
            .flatMap { LessonQuestionBank.allQuestionsForNode(it.id) }
        val (learned, stillWrong) = sectionQuestions[0].text to sectionQuestions[1].text
        val history = listOf(
            wrong(learned, at = 1),
            Answer(testId = "t", questionText = learned, selectedOption = 0, isCorrect = true, date = Date(2)),
            wrong(stillWrong, at = 3)
        )

        val review = createViewModel(RecordingPathRepository(pathWithLessonsDone()), mock(), answers = HistoryOf(history))

        assertThat(review.uiState.value.questions.map { it.text }).containsExactly(stillWrong)
    }
}
