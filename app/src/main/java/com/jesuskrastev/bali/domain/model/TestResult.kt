package com.jesuskrastev.bali.domain.model

import java.util.Date

data class TestResult(
    val id: String = "",
    val category: String,
    val score: Int,
    val total: Int,
    val date: Date,
    val isPassed: Boolean
)