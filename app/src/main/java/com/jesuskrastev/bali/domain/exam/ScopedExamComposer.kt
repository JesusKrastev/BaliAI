package com.jesuskrastev.bali.domain.exam

import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.domain.path.LessonQuestionBank
import com.jesuskrastev.bali.domain.util.GeminiQuestionParser
import com.jesuskrastev.bali.domain.util.QuestionId
import com.jesuskrastev.bali.ui.screens.test.QuestionUiState
import kotlin.random.Random

/**
 * Builds an exam out of the question bank of the lessons the student has studied.
 *
 * The exam mixes two kinds of question:
 * - **Bank questions, as written.** Vetted by hand and, when they carry a picture, written for
 *   that picture. They are the only questions that may have an image.
 * - **Variants written by Gemini** from the rest of the bank: the same concepts asked another
 *   way, never with an image. Gemini only ever sees the bank, so it cannot wander to a topic the
 *   student has not studied, and every variant is checked here before it reaches the exam.
 *
 * Whatever Gemini fails to deliver (nothing, too few, malformed, dependent on a picture) is made
 * up with more bank questions, so an exam is always complete and answerable.
 */
object ScopedExamComposer {

    /** Bank questions that go into the exam unchanged. */
    const val VERBATIM_COUNT = 12

    /** Gemini variants the exam is made of. */
    const val GENERATED_TARGET = ExamRules.QUESTION_COUNT - VERBATIM_COUNT

    /** Variants asked of Gemini: a few spare, to absorb the ones that get discarded. */
    const val GENERATED_REQUESTED = GENERATED_TARGET + 4

    /** Bank questions shown to Gemini as study material. */
    const val SOURCE_COUNT = 40

    private const val OPTIONS_PER_QUESTION = 3

    /** Phrases that betray a question written around a picture it no longer has. */
    private val IMAGE_PHRASES = listOf(
        "imagen", "imágenes", "figura", "fotograf", "ilustraci", "dibujo", "esquema",
        "mostrad", "se muestra", "se observa", "se aprecia", "se ve ", "representad",
        "esta señal", "esa señal", "dicha señal", "siguiente señal", "la señal anterior",
        "esta situación", "esa situación", "aparece en", "vehículo señalado"
    )

    /**
     * What is decided before Gemini is called.
     *
     * @property verbatim bank questions that go into the exam as they are (options already shuffled)
     * @property sources the material Gemini rewrites, grouped by lesson
     * @property reserve text-only bank questions held back to fill gaps and replace broken pictures
     */
    data class Plan(
        val verbatim: List<QuestionUiState>,
        val sources: List<ExamLesson>,
        val reserve: List<QuestionUiState>
    )

    /**
     * The finished exam.
     *
     * @property questions the exam, in the order the student will see it
     * @property reserve questions left over, to swap in for any whose picture fails to load
     * @property variantsAccepted how many Gemini variants survived the checks
     * @property bankFill how many bank questions had to stand in for missing variants
     */
    data class Assembly(
        val questions: List<QuestionUiState>,
        val reserve: List<QuestionUiState>,
        val variantsAccepted: Int,
        val bankFill: Int
    )

    /**
     * Picks what goes into the exam and what Gemini gets to read.
     *
     * Questions the student got wrong come first, so the exam goes after their weak points; the
     * rest are taken lesson by lesson in turn, so no lesson dominates.
     *
     * @param lessons the lessons in scope, see [ExamScope]
     * @param failedIds [QuestionId]s of the questions the student has answered wrongly
     * @param random source of randomness, overridable so tests can pin the result
     * @return the plan: bank questions to use as they are, material for Gemini, and a reserve
     */
    fun plan(lessons: List<ExamLesson>, failedIds: Set<String>, random: Random = Random.Default): Plan {
        val ordered = interleaved(lessons, random)
            .sortedByDescending { (_, question) -> QuestionId.of(question.text) in failedIds }
        val verbatim = ordered.take(VERBATIM_COUNT)
        val rest = ordered.drop(VERBATIM_COUNT)

        val sources = rest.take(SOURCE_COUNT)
            .groupBy({ it.first }, { it.second })
            .map { (lesson, questions) -> lesson.copy(questions = questions) }

        return Plan(
            verbatim = verbatim.map { (_, question) -> question.toExamQuestion(random) },
            sources = sources,
            reserve = rest.map { it.second }.filter { it.imageUrl == null }.map { it.toExamQuestion(random) }
        )
    }

    /**
     * Puts the exam together from the plan and whatever Gemini wrote.
     *
     * @param plan what [plan] returned
     * @param generated the variants parsed from Gemini's reply; empty when the call failed
     * @param random source of randomness, overridable so tests can pin the result
     * @return the exam, with its leftover reserve
     */
    fun assemble(plan: Plan, generated: List<QuestionUiState>, random: Random = Random.Default): Assembly {
        val seen = plan.verbatim.mapTo(HashSet()) { QuestionId.of(it.text) }
        val variants = generated
            .map { it.copy(imageUrl = null) }
            .filter { isUsable(it) && seen.add(QuestionId.of(it.text)) }

        val accepted = variants.take(GENERATED_TARGET)
        val missing = (ExamRules.QUESTION_COUNT - plan.verbatim.size - accepted.size).coerceAtLeast(0)
        val fill = plan.reserve.filter { QuestionId.of(it.text) !in seen }.take(missing)

        return Assembly(
            questions = (plan.verbatim + accepted + fill).shuffled(random),
            reserve = variants.drop(GENERATED_TARGET) + plan.reserve.filter { it !in fill },
            variantsAccepted = accepted.size,
            bankFill = fill.size
        )
    }

    /**
     * Swaps out every question whose picture could not be loaded.
     *
     * @param questions the exam
     * @param reserve text-only questions to put in their place
     * @param brokenUrls pictures that failed to load
     * @return the exam with each broken question replaced by a reserve one; shorter than
     *   [questions] only if the reserve ran out
     */
    fun replaceBrokenImages(
        questions: List<QuestionUiState>,
        reserve: List<QuestionUiState>,
        brokenUrls: Set<String>
    ): List<QuestionUiState> {
        val spare = ArrayDeque(reserve.filter { it.imageUrl == null })
        return questions.mapNotNull { question ->
            if (question.imageUrl in brokenUrls) spare.removeFirstOrNull() else question
        }
    }

    /**
     * Tells whether a question is fit to put in front of the student.
     *
     * @param question the question to check
     * @return true when it is [isWellFormed] and does not lean on a picture it does not have
     */
    fun isUsable(question: QuestionUiState): Boolean = isWellFormed(question) && !dependsOnImage(question)

    /**
     * Tells whether a question is complete and consistent, whatever it says about pictures.
     *
     * @param question the question to check
     * @return true when it has text and an explanation, exactly three different options and a
     *   valid correct answer
     */
    fun isWellFormed(question: QuestionUiState): Boolean {
        if (question.text.isBlank() || question.explanation.isBlank()) return false
        if (question.options.size != OPTIONS_PER_QUESTION) return false
        if (question.correctAnswerIndex !in question.options.indices) return false
        val distinctOptions = question.options.map { it.trim().lowercase() }.toSet()
        return distinctOptions.size == OPTIONS_PER_QUESTION && "" !in distinctOptions
    }

    /**
     * Tells whether a question reads as if a picture came with it.
     *
     * @param question the question to check; its text and options are read
     * @return true when it points at something that is not there ("la imagen", "esta señal"...)
     */
    fun dependsOnImage(question: QuestionUiState): Boolean {
        val words = (listOf(question.text) + question.options).joinToString(" ").lowercase()
        return IMAGE_PHRASES.any { it in words }
    }

    /**
     * Lists every question of every lesson, taking one lesson after another in turn.
     *
     * @return pairs of lesson and question, shuffled inside each lesson
     */
    internal fun interleaved(
        lessons: List<ExamLesson>,
        random: Random
    ): List<Pair<ExamLesson, LessonQuestionBank.StaticQuestion>> {
        val queues = lessons.shuffled(random).map { it to ArrayDeque(it.questions.shuffled(random)) }
        val result = mutableListOf<Pair<ExamLesson, LessonQuestionBank.StaticQuestion>>()
        while (queues.any { it.second.isNotEmpty() }) {
            for ((lesson, queue) in queues) queue.removeFirstOrNull()?.let { result += lesson to it }
        }
        return result
    }

    /** Turns a bank question into an exam question with its options in a random order. */
    internal fun LessonQuestionBank.StaticQuestion.toExamQuestion(random: Random): QuestionUiState =
        GeminiQuestionParser.shuffleOptions(
            QuestionUiState(
                text = text,
                options = options,
                correctAnswerIndex = correctAnswerIndex,
                explanation = explanation,
                imageUrl = imageUrl
            ),
            random
        )
}
