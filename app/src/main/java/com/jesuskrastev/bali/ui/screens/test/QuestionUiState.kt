package com.jesuskrastev.bali.ui.screens.test

import kotlinx.serialization.Serializable

@Serializable
data class QuestionUiState(
    val text: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String,
    val imageUrl: String? = null
)
