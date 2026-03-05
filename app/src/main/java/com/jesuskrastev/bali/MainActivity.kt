package com.jesuskrastev.bali

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.analytics.FirebaseAnalyticsTracker
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.data.update.InAppUpdateManager
import com.jesuskrastev.bali.domain.model.UpdateState
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.jesuskrastev.bali.domain.usecase.ExecuteFirestoreMigrationsUseCase
import com.jesuskrastev.bali.ui.navigation.AppNavigation
import com.jesuskrastev.bali.ui.navigation.GreetingsRoute
import com.jesuskrastev.bali.ui.navigation.HomeRoute
import com.jesuskrastev.bali.ui.theme.BaliTheme
import dagger.hilt.android.AndroidEntryPoint
import com.jesuskrastev.bali.ui.screens.main.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val viewModel: MainViewModel by viewModels()
        
        installSplashScreen().apply {
            setKeepOnScreenCondition {
                // Mantener el splash screen visible mientras se migra o se cargan datos
                viewModel.isMigrating.value || viewModel.isOnboardingCompleted.value == null
            }
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BaliTheme {
                val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
                val updateState by viewModel.updateState.collectAsState()
                val isMigrating by viewModel.isMigrating.collectAsState()
                val migrationError by viewModel.migrationError.collectAsState()
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
                            message = "ActualizaciÃ³n descargada. Reinicia para aplicar.",
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
                    when {
                        migrationError != null -> {
                            // Pantalla de Error en Base de Datos (Ãšnica UI bloqueante ahora)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Error iniciando la base de datos:\n$migrationError", 
                                    color = Color.Red
                                )
                            }
                        }
                        isOnboardingCompleted != null && !isMigrating -> {
                            // NavegaciÃ³n Normal (Aparece cuando el SplashScreen se oculta y no hay error)
                            val startDestination = if (isOnboardingCompleted == true) HomeRoute else GreetingsRoute
                            AppNavigation(
                                startDestination = startDestination
                            )
                        }
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
