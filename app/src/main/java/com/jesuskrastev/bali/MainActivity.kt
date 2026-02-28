package com.jesuskrastev.bali

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.analytics.FirebaseAnalyticsTracker
import com.jesuskrastev.bali.data.repository.UserRepositoryImpl
import com.jesuskrastev.bali.data.update.InAppUpdateManager
import com.jesuskrastev.bali.domain.model.UpdateState
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
    private val restoreEnergyUseCase: RestoreEnergyUseCase,
    private val analyticsTracker: FirebaseAnalyticsTracker,
    private val inAppUpdateManager: InAppUpdateManager
) : ViewModel() {

    val isOnboardingCompleted: StateFlow<Boolean?> = 
        combine(userRepository.hasCompletedOnboarding(), authRepository.isLoggedIn) { hasCompletedOnboarding, loggedIn ->
            hasCompletedOnboarding || loggedIn
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val updateState: StateFlow<UpdateState> = inAppUpdateManager.updateState

    init {
        viewModelScope.launch {
            val freezersUsed = resetStreakUseCase()
            if (freezersUsed > 0) analyticsTracker.streakFreezerUsed(freezersUsed)
            restoreEnergyUseCase()
        }
        inAppUpdateManager.checkForUpdate()
    }

    fun startFlexibleUpdate(activity: Activity) {
        inAppUpdateManager.startFlexibleUpdate(activity)
    }

    fun startImmediateUpdate(activity: Activity) {
        inAppUpdateManager.startImmediateUpdate(activity)
    }

    fun completeUpdate() {
        inAppUpdateManager.completeUpdate()
    }

    fun checkForDownloadedUpdate() {
        inAppUpdateManager.checkForDownloadedUpdate()
    }

    override fun onCleared() {
        super.onCleared()
        inAppUpdateManager.unregisterListener()
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
                val updateState by viewModel.updateState.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                val activity = androidx.activity.compose.LocalActivity.current!!

                // Launch the official Google Play update UI when available
                LaunchedEffect(updateState) {
                    val state = updateState
                    if (state is UpdateState.Available) {
                        if (state.isImmediate) {
                            viewModel.startImmediateUpdate(activity)
                        } else {
                            viewModel.startFlexibleUpdate(activity)
                        }
                    }
                }

                // Snackbar when the update has been downloaded
                LaunchedEffect(updateState) {
                    if (updateState is UpdateState.Downloaded) {
                        val result = snackbarHostState.showSnackbar(
                            message = "Actualización descargada. Reinicia para aplicar.",
                            actionLabel = "Reiniciar",
                            duration = SnackbarDuration.Indefinite
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.completeUpdate()
                        }
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    if (isOnboardingCompleted != null) {
                        val startDestination = if (isOnboardingCompleted == true) HomeRoute else GreetingsRoute
                        AppNavigation(
                            startDestination = startDestination,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check for pending downloads or interrupted immediate updates
        val viewModel = androidx.lifecycle.ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.checkForDownloadedUpdate()
    }
}
