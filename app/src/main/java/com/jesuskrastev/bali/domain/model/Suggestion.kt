package com.jesuskrastev.bali.domain.model

data class Suggestion(
    val text: String,
    val userId: String?,
    val email: String?,
    val timestamp: Long,
)