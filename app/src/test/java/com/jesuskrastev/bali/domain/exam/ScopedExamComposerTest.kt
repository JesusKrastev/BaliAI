package com.jesuskrastev.bali.domain.exam

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.domain.path.LessonQuestionBank.StaticQuestion
import com.jesuskrastev.bali.domain.util.QuestionId
import com.jesuskrastev.bali.ui.screens.test.QuestionUiState
import org.junit.Test
import kotlin.random.Random

class ScopedExamComposerTest {

    private val random = Random(42)

    private fun bank(lesson: String, count: Int, withImage: Boolean = false) = ExamLesson(
        nodeId = lesson,
        title = "Lección $lesson",
        questions = (1..count).map { n ->
            StaticQuestion(
                text = "Pregunta $n de $lesson",
                options = listOf("Correcta $n", "Falsa A $n", "Falsa B $n"),
                correctAnswerIndex = 0,
                explanation = "Explicación $n",
                imageUrl = if (withImage) "https://img/$lesson-$n.png" else null
            )
        }
    )

    private fun variant(
        text: String,
        options: List<String> = listOf("Sí", "No", "Depende"),
        correct: Int = 0,
        explanation: String = "Porque lo dice la norma.",
        imageUrl: String? = null
    ) = QuestionUiState(text, options, correct, explanation, imageUrl)

    private fun goodVariants(count: Int) = (1..count).map { variant("Variante nueva número $it") }

    private val lessons = listOf(bank("a", 20), bank("b", 20), bank("c", 20))

    // ── plan ──────────────────────────────────────────────────────────────

    @Test
    fun `plan takes the questions the student failed first`() {
        val failed = lessons.flatMap { it.questions }.take(5).map { QuestionId.of(it.text) }.toSet()

        val plan = ScopedExamComposer.plan(lessons, failed, random = random)

        val verbatim = plan.verbatim.map { QuestionId.of(it.text) }
        assertThat(verbatim).containsAtLeastElementsIn(failed)
    }

    @Test
    fun `plan spreads the bank questions over the lessons`() {
        val plan = ScopedExamComposer.plan(lessons, emptySet(), random = random)

        val perLesson = plan.verbatim.groupBy { it.text.substringAfter(" de ") }.mapValues { it.value.size }
        assertThat(perLesson.keys).containsExactly("a", "b", "c")
        assertThat(perLesson.values.max() - perLesson.values.min()).isAtMost(1)
    }

    @Test
    fun `plan never shows Gemini a question that already goes into the exam as written`() {
        val plan = ScopedExamComposer.plan(lessons, emptySet(), random = random)

        val verbatimTexts = plan.verbatim.map { it.text }.toSet()
        val sourceTexts = plan.sources.flatMap { lesson -> lesson.questions.map { it.text } }
        assertThat(sourceTexts.intersect(verbatimTexts)).isEmpty()
        assertThat(sourceTexts.size).isAtMost(ScopedExamComposer.SOURCE_COUNT)
    }

    @Test
    fun `plan keeps only text questions in the reserve`() {
        val withPictures = listOf(bank("a", 30, withImage = true), bank("b", 30))

        val plan = ScopedExamComposer.plan(withPictures, emptySet(), random = random)

        assertThat(plan.reserve).isNotEmpty()
        assertThat(plan.reserve.all { it.imageUrl == null }).isTrue()
    }

    @Test
    fun `plan keeps the right answer pointing at the right text after shuffling`() {
        val plan = ScopedExamComposer.plan(lessons, emptySet(), random = random)

        plan.verbatim.forEach { question ->
            val n = question.text.substringAfter("Pregunta ").substringBefore(" de")
            assertThat(question.options[question.correctAnswerIndex]).isEqualTo("Correcta $n")
        }
    }

    // ── assemble ──────────────────────────────────────────────────────────

    @Test
    fun `assemble builds a full exam of bank questions and accepted variants`() {
        val plan = ScopedExamComposer.plan(lessons, emptySet(), random = random)

        val assembly = ScopedExamComposer.assemble(plan, goodVariants(22), random)

        assertThat(assembly.questions).hasSize(ExamRules.QUESTION_COUNT)
        assertThat(assembly.variantsAccepted).isEqualTo(ScopedExamComposer.GENERATED_TARGET)
        assertThat(assembly.bankFill).isEqualTo(0)
    }

    @Test
    fun `assemble still gives a full exam when Gemini returns nothing`() {
        val plan = ScopedExamComposer.plan(lessons, emptySet(), random = random)

        val assembly = ScopedExamComposer.assemble(plan, emptyList(), random)

        assertThat(assembly.questions).hasSize(ExamRules.QUESTION_COUNT)
        assertThat(assembly.variantsAccepted).isEqualTo(0)
        assertThat(assembly.bankFill).isEqualTo(ScopedExamComposer.GENERATED_TARGET)
    }

    @Test
    fun `assemble never lets a picture through on a variant`() {
        val plan = ScopedExamComposer.plan(lessons, emptySet(), random = random)
        val withPicture = goodVariants(22).map {
            it.copy(imageUrl = "https://commons.wikimedia.org/wiki/Special:FilePath/x.svg")
        }

        val assembly = ScopedExamComposer.assemble(plan, withPicture, random)

        assertThat(assembly.questions.mapNotNull { it.imageUrl }).isEmpty()
    }

    @Test
    fun `assemble drops malformed variants and fills the gap from the bank`() {
        val plan = ScopedExamComposer.plan(lessons, emptySet(), random = random)
        val broken = listOf(
            variant("Con dos opciones", options = listOf("Sí", "No")),
            variant("Con opciones repetidas", options = listOf("Sí", "sí ", "No")),
            variant("Con respuesta fuera de rango", correct = 5),
            variant("Sin explicación", explanation = " "),
            variant("", options = listOf("Sí", "No", "Depende"))
        )

        val assembly = ScopedExamComposer.assemble(plan, broken + goodVariants(3), random)

        assertThat(assembly.variantsAccepted).isEqualTo(3)
        assertThat(assembly.questions).hasSize(ExamRules.QUESTION_COUNT)
    }

    @Test
    fun `assemble drops variants that lean on a picture`() {
        val plan = ScopedExamComposer.plan(lessons, emptySet(), random = random)
        val leaning = listOf(
            variant("¿Qué indica la señal de la imagen?"),
            variant("Observando esta señal, ¿qué debe hacer?"),
            variant("¿Cuál es la velocidad máxima?", options = listOf("La de la figura", "50", "90"))
        )

        val assembly = ScopedExamComposer.assemble(plan, leaning, random)

        assertThat(assembly.variantsAccepted).isEqualTo(0)
    }

    @Test
    fun `assemble drops a variant that repeats a question or another variant`() {
        val plan = ScopedExamComposer.plan(lessons, emptySet(), random = random)
        val copyOfBank = variant(plan.verbatim.first().text)
        val twice = listOf(variant("Una variante repetida"), variant("Una VARIANTE repetida!"))

        val assembly = ScopedExamComposer.assemble(plan, listOf(copyOfBank) + twice, random)

        assertThat(assembly.variantsAccepted).isEqualTo(1)
        val ids = assembly.questions.map { QuestionId.of(it.text) }
        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `assemble keeps surplus variants as reserve`() {
        val plan = ScopedExamComposer.plan(lessons, emptySet(), random = random)

        val assembly = ScopedExamComposer.assemble(plan, goodVariants(22), random)

        assertThat(assembly.reserve.map { it.text }).containsAtLeastElementsIn(
            (ScopedExamComposer.GENERATED_TARGET + 1..22).map { "Variante nueva número $it" }
        )
    }

    // ── replaceBrokenImages ───────────────────────────────────────────────

    @Test
    fun `replaceBrokenImages swaps a question whose picture failed for a text one`() {
        val picture = variant("Con foto", imageUrl = "https://img/roto.png")
        val fine = variant("Normal")
        val spare = variant("De reserva")

        val result = ScopedExamComposer.replaceBrokenImages(
            listOf(picture, fine), listOf(spare), setOf("https://img/roto.png")
        )

        assertThat(result).containsExactly(spare, fine).inOrder()
    }

    @Test
    fun `replaceBrokenImages keeps questions whose picture loaded`() {
        val picture = variant("Con foto", imageUrl = "https://img/bien.png")

        val result = ScopedExamComposer.replaceBrokenImages(
            listOf(picture), listOf(variant("Reserva")), setOf("https://img/otra.png")
        )

        assertThat(result).containsExactly(picture)
    }

    @Test
    fun `replaceBrokenImages never puts a question with a picture in as a replacement`() {
        val picture = variant("Con foto", imageUrl = "https://img/roto.png")
        val spareWithPicture = variant("Reserva con foto", imageUrl = "https://img/otra.png")

        val result = ScopedExamComposer.replaceBrokenImages(
            listOf(picture), listOf(spareWithPicture), setOf("https://img/roto.png")
        )

        assertThat(result).isEmpty()
    }

    // ── dependsOnImage ────────────────────────────────────────────────────

    @Test
    fun `a self-contained question does not depend on a picture`() {
        val question = variant("Circula por una autovía. ¿Qué velocidad máxima tiene como norma general?")

        assertThat(ScopedExamComposer.dependsOnImage(question)).isFalse()
    }

    @Test
    fun `naming a sign by its code does not count as leaning on a picture`() {
        val question = variant(
            "¿Qué prohíbe la señal R-2 de STOP?",
            options = listOf("Avanzar sin parar", "Parar", "Aparcar")
        )

        assertThat(ScopedExamComposer.dependsOnImage(question)).isFalse()
    }
}
