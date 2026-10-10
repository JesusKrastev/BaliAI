package com.jesuskrastev.bali.domain.exam

import com.jesuskrastev.bali.domain.exam.ScopedExamComposer.toExamQuestion
import com.jesuskrastev.bali.domain.path.LessonQuestionBank
import com.jesuskrastev.bali.domain.util.GeminiQuestionParser
import com.jesuskrastev.bali.domain.util.QuestionId
import com.jesuskrastev.bali.ui.screens.test.QuestionUiState
import kotlin.random.Random

/**
 * Builds a review of the questions the student still gets wrong in a section, one rewrite per question.
 *
 * Each review question is a rewrite of one mistake: the same thing is asked, worded another way
 * and a little trickier. When the original has a picture, the rewrite keeps that very picture,
 * taken from the bank and not asked of Gemini, so the question and its picture always agree.
 * Whatever Gemini does not rewrite properly is shown as the bank wrote it, so a review is always
 * complete. A student with no mistake pending still gets a review (of the section's questions),
 * because the review is a step of the path and must be passable.
 */
object ScopedReviewComposer {

    /** Most questions in a review. */
    const val MAX_QUESTIONS = 10

    /**
     * A bank question picked for the review.
     *
     * @property index number the prompt gives it, from 1; the model quotes it back as `sourceIndex`
     * @property lessonTitle title of the lesson it comes from
     * @property question the bank question
     */
    data class Source(val index: Int, val lessonTitle: String, val question: LessonQuestionBank.StaticQuestion)

    /**
     * What is decided before Gemini is called.
     *
     * @property sources the questions to rewrite, numbered from 1
     * @property reserve text-only bank questions held back to replace a question whose picture fails
     */
    data class Plan(val sources: List<Source>, val reserve: List<QuestionUiState>)

    /**
     * Picks the questions the review rewrites: the student's pending mistakes in these lessons, or,
     * when there is none, the lessons' own questions.
     *
     * @param lessons the lessons in scope, see [ExamScope]
     * @param failedIds [QuestionId]s of the questions the student still gets wrong, see [PendingMistakes]
     * @param random source of randomness, overridable so tests can pin the result
     * @return the plan
     */
    fun plan(lessons: List<ExamLesson>, failedIds: Set<String>, random: Random = Random.Default): Plan {
        val ordered = ScopedExamComposer.interleaved(lessons, random)
            .sortedByDescending { (_, question) -> QuestionId.of(question.text) in failedIds }
        val mistakes = ordered.filter { (_, question) -> QuestionId.of(question.text) in failedIds }
        val picked = (mistakes.ifEmpty { ordered }).take(MAX_QUESTIONS)
        val sources = picked.mapIndexed { position, (lesson, question) ->
            Source(index = position + 1, lessonTitle = lesson.title, question = question)
        }
        val reserve = (ordered - picked.toSet())
            .map { it.second }
            .filter { it.imageUrl == null }
            .map { it.toExamQuestion(random) }
        return Plan(sources, reserve)
    }

    /**
     * Pairs each source with its rewrite.
     *
     * A rewrite is taken when it quotes the source's number and is well formed; if the source has a
     * picture the rewrite gets that picture, and may say "la señal de la imagen". A rewrite of a
     * source without a picture must make sense on its own. Anything else falls back to the source
     * itself, as written.
     *
     * @param plan what [plan] returned
     * @param rewrites what Gemini wrote, with the source number it quoted; empty if the call failed
     * @param random source of randomness for the fallback shuffle, overridable for tests
     * @return the review in source order, and how many questions are Gemini rewrites
     */
    fun assemble(
        plan: Plan,
        rewrites: List<GeminiQuestionParser.SourcedQuestion>,
        random: Random = Random.Default
    ): Assembly {
        var rewritten = 0
        val questions = plan.sources.map { source ->
            val candidate = rewrites
                .filter { it.sourceIndex == source.index }
                .map { it.question }
                .firstOrNull { isAcceptable(it, source) && QuestionId.of(it.text) != QuestionId.of(source.question.text) }
            if (candidate != null) {
                rewritten++
                candidate.copy(imageUrl = source.question.imageUrl)
            } else {
                source.question.toExamQuestion(random)
            }
        }
        return Assembly(questions = questions, reserve = plan.reserve, rewritten = rewritten)
    }

    /**
     * The finished review.
     *
     * @property questions the review, one per source
     * @property reserve text-only questions to swap in for any whose picture fails to load
     * @property rewritten how many questions are Gemini rewrites and not the bank's own text
     */
    data class Assembly(val questions: List<QuestionUiState>, val reserve: List<QuestionUiState>, val rewritten: Int)

    /** A rewrite is fine if it is well formed, and either keeps a picture to point at or needs none. */
    private fun isAcceptable(rewrite: QuestionUiState, source: Source): Boolean =
        if (source.question.imageUrl != null) {
            ScopedExamComposer.isWellFormed(rewrite)
        } else {
            ScopedExamComposer.isUsable(rewrite)
        }
}
