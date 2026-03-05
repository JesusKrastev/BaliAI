package com.jesuskrastev.bali.domain.model

import java.util.Date

/**
 * Represents the final result of a test or exam taken by the user.
 *
 * @property id Unique identifier for this test result.
 * @property category The category or topic of the test.
 * @property score Number of correctly answered questions.
 * @property total Total number of questions in the test.
 * @property date The timestamp when the test was completed.
 * @property isPassed Indicates whether the user's score meets the passing threshold.
 */data class TestResult(
    val id: String = "",
    val category: String,
    val score: Int,
    val total: Int,
    val date: Date,
    val isPassed: Boolean
)