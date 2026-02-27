package com.jesuskrastev.bali

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.repository.UserRepositoryImpl
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.usecase.RestoreEnergyUseCase
import com.jesuskrastev.bali.domain.usecase.ResetStreakUseCase
import com.jesuskrastev.bali.ui.navigation.AppNavigation
import com.jesuskrastev.bali.ui.navigation.GreetingsRoute
import com.jesuskrastev.bali.ui.navigation.HomeRoute
import com.jesuskrastev.bali.ui.theme.BaliTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepositoryImpl,
    private val authRepository: AuthRepository,
    private val resetStreakUseCase: ResetStreakUseCase,
    private val restoreEnergyUseCase: RestoreEnergyUseCase
) : ViewModel() {

    val isOnboardingCompleted: StateFlow<Boolean?> = 
        combine(userRepository.hasCompletedOnboarding(), authRepository.isLoggedIn) { hasCompletedOnboarding, loggedIn ->
            hasCompletedOnboarding || loggedIn
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            resetStreakUseCase()
            restoreEnergyUseCase()
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BaliTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()

                if (isOnboardingCompleted != null) {
                    val startDestination = if (isOnboardingCompleted == true) HomeRoute else GreetingsRoute
                    AppNavigation(startDestination = startDestination)
                }
            }
        }
    }
}
