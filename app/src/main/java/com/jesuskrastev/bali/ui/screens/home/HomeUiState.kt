package com.jesuskrastev.bali.ui.screens.home

data class HomeUiState(
    val userName: String = "Futuro Conductor",
    val userEmail: String? = null,
    val profilePictureUrl: String? = null,
    val examDateDays: Int? = null,
    val streak: Int = 0,
    val readinessPercent: Int = 5,
    val avgScore: Int = 0,
    val totalTests: Int = 0,
    val xpLevel: Int = 1,
    val xpProgress: Float = 0.3f,
    val energyCount: Int = 3,
    val coinsCount: Int = 0,
    val mistakesCount: Int = 0,
    val streakFreezes: Int = 0,
    val difficultTopics: List<String> = emptyList(),
    val dailyTip: String = "",
    val isLoggedIn: Boolean = false,
    val showEnergyDialog: Boolean = false,
    val showNoCoinsDialog: Boolean = false,
    val isLoading: Boolean = true,
)
