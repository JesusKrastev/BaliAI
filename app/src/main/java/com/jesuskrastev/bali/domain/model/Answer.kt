package com.jesuskrastev.bali.domain.model

import java.util.Date

data class Answer(
    val id: String = "",
    val testId: String,
    val questionText: String,
    val selectedOption: Int,
    val isCorrect: Boolean,
    val date: Date
)