package com.jesuskrastev.bali.domain.model

import com.jesuskrastev.bali.domain.util.QuestionId
import java.util.Date

/**
 * Represents a single answer submitted by the user during a test or practice session.
 *
 * @property id Unique identifier for this specific answer record.
 * @property testId The identifier of the test this answer belongs to.
 * @property questionText The text of the question that was asked.
 * @property selectedOption The index or identifier of the option selected by the user.
 * @property isCorrect Indicates whether the submitted answer was correct.
 * @property date The timestamp when the answer was submitted.
 * @property questionId [QuestionId] of the question, blank for answers saved before ids existed;
 *   read it through [resolvedQuestionId].
 * @property topic The topic the session was about, when its category names one. Null for the
 *   official exam (mixed topics), for categories that name no topic and for old answers.
 * @property mode Where the answer was given; null for answers saved before modes existed.
 */
data class Answer(
    val id: String = "",
    val testId: String,
    val questionText: String,
    val selectedOption: Int,
    val isCorrect: Boolean,
    val date: Date,
    val questionId: String = "",
    val topic: DrivingTopic? = null,
    val mode: AnswerMode? = null
) {
    /**
     * The id of the question this answer was about. Old answers have none stored, so it is
     * worked out from [questionText]: they join the same history as the new ones without
     * rewriting anything in the database.
     */
    val resolvedQuestionId: String
        get() = questionId.ifBlank { QuestionId.of(questionText) }
}
