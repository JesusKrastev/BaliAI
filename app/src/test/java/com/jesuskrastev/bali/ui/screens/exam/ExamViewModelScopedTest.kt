package com.jesuskrastev.bali.ui.screens.exam

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.model.NodeType
import com.jesuskrastev.bali.domain.path.DgtLearningPathTemplate
import com.jesuskrastev.bali.domain.path.LessonQuestionBank
import com.jesuskrastev.bali.domain.usecase.CompletePathNodeUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import com.jesuskrastev.bali.ui.screens.auth.FakeAnswerRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeTestResultRepository
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.ui.screens.test.TestSummary
import com.jesuskrastev.bali.util.FakeImagePrefetcher
import com.jesuskrastev.bali.util.FakeSoundEffects
import com.jesuskrastev.bali.util.MainDispatcherRule
import com.jesuskrastev.bali.util.RecordingPathRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * An exam only asks about what the student has studied, using the real question bank.
 *
 * Gemini is not stubbed: the mock answers nothing, so these tests also cover the case where the
 * model fails and the exam is made of bank questions alone.
 */
class ExamViewModelScopedTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = FakeUserRepository()

    /** The real path with every lesson and review of section 0 done and that section's exam open. */
    private fun pathWithSectionZeroDone(): List<LessonNode> =
        DgtLearningPathTemplate.buildInitialPath().map { node ->
            when {
                node.sectionIndex == 0 && node.nodeType != NodeType.EXAM -> node.copy(status = NodeStatus.COMPLETED)
                node.sectionIndex == 0 -> node.copy(status = NodeStatus.UNLOCKED)
                else -> node
            }
        }

    private val sectionZeroExamId = DgtLearningPathTemplate.buildInitialPath()
        .first { it.sectionIndex == 0 && it.nodeType == NodeType.EXAM }.id

    private val sectionZeroTexts: Set<String> = DgtLearningPathTemplate.buildInitialPath()
        .filter { it.sectionIndex == 0 && it.nodeType == NodeType.LESSON }
        .flatMap { LessonQuestionBank.allQuestionsForNode(it.id) }
        .map { it.text }
        .toSet()

    private fun createViewModel(
        path: RecordingPathRepository,
        nodeId: String? = sectionZeroExamId,
        images: FakeImagePrefetcher = FakeImagePrefetcher()
    ) = ExamViewModel(
        userRepository = userRepository,
        testResultRepository = FakeTestResultRepository(),
        answerRepository = FakeAnswerRepository(),
        pathRepository = path,
        gemini = mock(),
        imagePrefetcher = images,
        incrementStreakUseCase = IncrementStreakUseCase(userRepository),
        incrementXpUseCase = IncrementXpUseCase(userRepository),
        incrementCoinsUseCase = IncrementCoinsUseCase(userRepository),
        completePathNodeUseCase = CompletePathNodeUseCase(path),
        analytics = mock<AnalyticsTracker>(),
        soundEffects = FakeSoundEffects(),
        savedStateHandle = SavedStateHandle(if (nodeId != null) mapOf("nodeId" to nodeId) else emptyMap())
    )

    /** Answers every question with [pick] of its right option index and finishes the exam. */
    private fun finish(exam: ExamViewModel, pick: (Int) -> Int): TestSummary {
        exam.uiState.value.questions.forEachIndexed { index, question ->
            exam.onEvent(ExamEvent.GoToQuestion(index))
            exam.onEvent(ExamEvent.SelectOption(pick(question.correctAnswerIndex)))
        }
        return runBlocking {
            val finished = CompletableDeferred<TestSummary>()
            exam.onEvent(ExamEvent.FinishExam { finished.complete(it) })
            withTimeout(10_000) { finished.await() }
        }
    }

    @Test
    fun `a section exam has the full number of questions even when Gemini gives nothing`() {
        val exam = createViewModel(RecordingPathRepository(pathWithSectionZeroDone()))

        assertThat(exam.uiState.value.error).isNull()
        assertThat(exam.uiState.value.isLoading).isFalse()
        assertThat(exam.uiState.value.questions).hasSize(ExamRules.QUESTION_COUNT)
    }

    @Test
    fun `every question of a section exam comes from a lesson of that section`() {
        val exam = createViewModel(RecordingPathRepository(pathWithSectionZeroDone()))

        val foreign = exam.uiState.value.questions.map { it.text }.filter { it !in sectionZeroTexts }

        assertThat(foreign).isEmpty()
    }

    @Test
    fun `a question keeps its right answer after the options are shuffled`() {
        val exam = createViewModel(RecordingPathRepository(pathWithSectionZeroDone()))

        val bank = DgtLearningPathTemplate.buildInitialPath()
            .filter { it.sectionIndex == 0 && it.nodeType == NodeType.LESSON }
            .flatMap { LessonQuestionBank.allQuestionsForNode(it.id) }
            .associateBy { it.text }

        exam.uiState.value.questions.forEach { question ->
            val original = bank.getValue(question.text)
            assertThat(question.options[question.correctAnswerIndex])
                .isEqualTo(original.options[original.correctAnswerIndex])
        }
    }

    @Test
    fun `pictures that load stay in the exam`() {
        val images = FakeImagePrefetcher()
        val exam = createViewModel(RecordingPathRepository(pathWithSectionZeroDone()), images = images)

        val shown = exam.uiState.value.questions.mapNotNull { it.imageUrl }

        assertThat(shown).isNotEmpty()
        assertThat(images.requested).containsAtLeastElementsIn(shown)
    }

    @Test
    fun `when no picture loads the exam has no picture and is still complete`() {
        val exam = createViewModel(
            RecordingPathRepository(pathWithSectionZeroDone()),
            images = FakeImagePrefetcher(failEverything = true)
        )

        assertThat(exam.uiState.value.error).isNull()
        assertThat(exam.uiState.value.questions).hasSize(ExamRules.QUESTION_COUNT)
        assertThat(exam.uiState.value.questions.mapNotNull { it.imageUrl }).isEmpty()
    }

    @Test
    fun `a broken picture is replaced rather than shown`() {
        val sectionPictures = DgtLearningPathTemplate.buildInitialPath()
            .filter { it.sectionIndex == 0 && it.nodeType == NodeType.LESSON }
            .flatMap { LessonQuestionBank.allQuestionsForNode(it.id) }
            .mapNotNull { it.imageUrl }
            .toSet()
        val broken = sectionPictures.filter { it.hashCode() % 2 == 0 }.toSet()
        assertThat(broken).isNotEmpty()

        val exam = createViewModel(
            RecordingPathRepository(pathWithSectionZeroDone()),
            images = FakeImagePrefetcher(broken = broken)
        )

        assertThat(exam.uiState.value.questions).hasSize(ExamRules.QUESTION_COUNT)
        assertThat(exam.uiState.value.questions.mapNotNull { it.imageUrl }.filter { it in broken }).isEmpty()
    }

    @Test
    fun `an exam of a unit the student has not studied shows an error instead of unrelated questions`() {
        val nothingDone = DgtLearningPathTemplate.buildInitialPath()
            .map { if (it.id == sectionZeroExamId) it.copy(status = NodeStatus.UNLOCKED) else it }

        val exam = createViewModel(RecordingPathRepository(nothingDone))

        assertThat(exam.uiState.value.questions).isEmpty()
        assertThat(exam.uiState.value.error).isNotNull()
        assertThat(exam.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `an exam with no unit shows an error`() {
        val exam = createViewModel(RecordingPathRepository(pathWithSectionZeroDone()), nodeId = null)

        assertThat(exam.uiState.value.questions).isEmpty()
        assertThat(exam.uiState.value.error).isNotNull()
    }

    @Test
    fun `passing the exam completes its node and unlocks the next section`() {
        val path = RecordingPathRepository(pathWithSectionZeroDone())
        val exam = createViewModel(path)

        finish(exam) { right -> right }

        val examNode = path.nodes.first { it.id == sectionZeroExamId }
        val next = path.nodes.first { it.orderIndex == examNode.orderIndex + 1 }
        assertThat(examNode.status).isEqualTo(NodeStatus.COMPLETED)
        assertThat(examNode.scorePercentage).isEqualTo(100)
        assertThat(next.status).isEqualTo(NodeStatus.UNLOCKED)
    }

    @Test
    fun `failing the exam completes its node but keeps the next section locked`() {
        val path = RecordingPathRepository(pathWithSectionZeroDone())
        val exam = createViewModel(path)

        finish(exam) { right -> (right + 1) % 3 }

        val examNode = path.nodes.first { it.id == sectionZeroExamId }
        val next = path.nodes.first { it.orderIndex == examNode.orderIndex + 1 }
        assertThat(examNode.status).isEqualTo(NodeStatus.COMPLETED)
        assertThat(examNode.scorePercentage).isEqualTo(0)
        assertThat(next.status).isEqualTo(NodeStatus.LOCKED)
    }
}
