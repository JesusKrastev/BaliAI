package com.jesuskrastev.bali.domain.exam

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.path.LessonQuestionBank.StaticQuestion
import com.jesuskrastev.bali.domain.util.GeminiQuestionParser.SourcedQuestion
import com.jesuskrastev.bali.domain.util.QuestionId
import com.jesuskrastev.bali.ui.screens.test.QuestionUiState
import org.junit.Test
import kotlin.random.Random

class ScopedReviewComposerTest {

    private val random = Random(7)

    private fun lesson(id: String, count: Int, picture: Boolean = false) = ExamLesson(
        nodeId = id,
        title = "Lección $id",
        questions = (1..count).map { n ->
            StaticQuestion(
                text = "Pregunta $n de $id",
                options = listOf("Correcta $n", "Falsa A $n", "Falsa B $n"),
                correctAnswerIndex = 0,
                explanation = "Explicación $n",
                imageUrl = if (picture) "https://img/$id-$n.png" else null
            )
        }
    )

    private fun rewrite(
        source: Int,
        text: String = "Versión reescrita de la pregunta $source",
        options: List<String> = listOf("Sí", "No", "Depende"),
        explanation: String = "Porque lo dice la norma."
    ) = SourcedQuestion(source, QuestionUiState(text, options, 0, explanation, imageUrl = null))

    @Test
    fun `the review has ten sources numbered from one`() {
        val plan = ScopedReviewComposer.plan(listOf(lesson("a", 20), lesson("b", 20)), emptySet(), random)

        assertThat(plan.sources.map { it.index }).containsExactlyElementsIn(1..ScopedReviewComposer.MAX_QUESTIONS).inOrder()
    }

    @Test
    fun `the review is made of the questions the student still gets wrong, and only those`() {
        val lessons = listOf(lesson("a", 20), lesson("b", 20))
        val failed = lessons.flatMap { it.questions }.take(4).map { QuestionId.of(it.text) }.toSet()

        val plan = ScopedReviewComposer.plan(lessons, failed, random)

        assertThat(plan.sources.map { QuestionId.of(it.question.text) }).containsExactlyElementsIn(failed)
    }

    @Test
    fun `a review never has more than ten questions even with more mistakes`() {
        val lessons = listOf(lesson("a", 30))
        val failed = lessons.flatMap { it.questions }.take(25).map { QuestionId.of(it.text) }.toSet()

        val plan = ScopedReviewComposer.plan(lessons, failed, random)

        assertThat(plan.sources).hasSize(ScopedReviewComposer.MAX_QUESTIONS)
        assertThat(plan.sources.all { QuestionId.of(it.question.text) in failed }).isTrue()
    }

    @Test
    fun `with no mistake pending the review falls back to the section questions so the node can be passed`() {
        val plan = ScopedReviewComposer.plan(listOf(lesson("a", 20)), emptySet(), random)

        assertThat(plan.sources).hasSize(ScopedReviewComposer.MAX_QUESTIONS)
    }

    @Test
    fun `only the question that is still wrong is reviewed`() {
        val lessons = listOf(lesson("a", 20))
        val stillWrong = QuestionId.of(lessons[0].questions[0].text)

        val plan = ScopedReviewComposer.plan(lessons, setOf(stillWrong), random)

        assertThat(plan.sources).hasSize(1)
    }

    @Test
    fun `a rewrite of a question with a picture keeps that very picture`() {
        val plan = ScopedReviewComposer.plan(listOf(lesson("a", 20, picture = true)), emptySet(), random)
        val rewrites = plan.sources.map {
            rewrite(it.index, text = "¿Qué indica la señal de la imagen? (${it.index})")
        }

        val assembly = ScopedReviewComposer.assemble(plan, rewrites, random)

        assertThat(assembly.rewritten).isEqualTo(ScopedReviewComposer.MAX_QUESTIONS)
        assembly.questions.forEachIndexed { position, question ->
            assertThat(question.imageUrl).isEqualTo(plan.sources[position].question.imageUrl)
            assertThat(question.text).contains("señal de la imagen")
        }
    }

    @Test
    fun `a picture that Gemini invents is never used`() {
        val plan = ScopedReviewComposer.plan(listOf(lesson("a", 20)), emptySet(), random)
        val rewrites = plan.sources.map {
            val base = rewrite(it.index)
            base.copy(question = base.question.copy(imageUrl = "https://commons.wikimedia.org/x.svg"))
        }

        val assembly = ScopedReviewComposer.assemble(plan, rewrites, random)

        assertThat(assembly.questions.mapNotNull { it.imageUrl }).isEmpty()
    }

    @Test
    fun `a rewrite of a text question may not lean on a picture it does not have`() {
        val plan = ScopedReviewComposer.plan(listOf(lesson("a", 20)), emptySet(), random)
        val rewrites = plan.sources.map { rewrite(it.index, text = "¿Qué indica la señal de la imagen?") }

        val assembly = ScopedReviewComposer.assemble(plan, rewrites, random)

        assertThat(assembly.rewritten).isEqualTo(0)
        assertThat(assembly.questions.map { it.text }).containsExactlyElementsIn(plan.sources.map { it.question.text })
    }

    @Test
    fun `a question Gemini did not rewrite properly is shown as the bank wrote it`() {
        val plan = ScopedReviewComposer.plan(listOf(lesson("a", 20)), emptySet(), random)
        val rewrites = listOf(
            rewrite(1),
            rewrite(2, options = listOf("Sí", "Sí", "No")),
            rewrite(3, explanation = ""),
            rewrite(99)
        )

        val assembly = ScopedReviewComposer.assemble(plan, rewrites, random)

        assertThat(assembly.questions).hasSize(ScopedReviewComposer.MAX_QUESTIONS)
        assertThat(assembly.rewritten).isEqualTo(1)
        assertThat(assembly.questions[1].text).isEqualTo(plan.sources[1].question.text)
    }

    @Test
    fun `a rewrite that copies the original text is not counted as a rewrite`() {
        val plan = ScopedReviewComposer.plan(listOf(lesson("a", 20)), emptySet(), random)
        val copy = rewrite(1, text = plan.sources[0].question.text)

        val assembly = ScopedReviewComposer.assemble(plan, listOf(copy), random)

        assertThat(assembly.rewritten).isEqualTo(0)
    }

    @Test
    fun `with no rewrites at all the review is still complete and keeps its pictures`() {
        val plan = ScopedReviewComposer.plan(listOf(lesson("a", 20, picture = true)), emptySet(), random)

        val assembly = ScopedReviewComposer.assemble(plan, emptyList(), random)

        assertThat(assembly.questions).hasSize(ScopedReviewComposer.MAX_QUESTIONS)
        assertThat(assembly.questions.all { it.imageUrl != null }).isTrue()
    }

    @Test
    fun `an unshuffled fallback keeps its right answer pointing at the right text`() {
        val plan = ScopedReviewComposer.plan(listOf(lesson("a", 20)), emptySet(), random)

        val assembly = ScopedReviewComposer.assemble(plan, emptyList(), random)

        assembly.questions.forEach { question ->
            assertThat(question.options[question.correctAnswerIndex]).startsWith("Correcta")
        }
    }
}
