package com.jesuskrastev.bali.domain.model

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
 */data class Answer(
    val id: String = "",
    val testId: String,
    val questionText: String,
    val selectedOption: Int,
    val isCorrect: Boolean,
    val date: Date
)