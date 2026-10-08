package com.jesuskrastev.bali

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jesuskrastev.bali.domain.model.UpdateState
import com.jesuskrastev.bali.ui.navigation.AppNavigation
import com.jesuskrastev.bali.ui.navigation.AuthRoute
import com.jesuskrastev.bali.ui.navigation.GreetingsRoute
import com.jesuskrastev.bali.ui.navigation.HomeRoute
import com.jesuskrastev.bali.ui.navigation.PaywallRoute
import com.jesuskrastev.bali.ui.theme.BaliTheme
import dagger.hilt.android.AndroidEntryPoint
import com.jesuskrastev.bali.ui.screens.main.AppEntryPoint
import com.jesuskrastev.bali.ui.screens.main.MainViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    /** Creates the app UI after resolving migration, onboarding and authentication state. */
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().apply {
            setKeepOnScreenCondition {
                // Mantener el splash screen visible mientras se migra o se cargan datos
                viewModel.isMigrating.value || viewModel.entryPoint.value == null
            }
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BaliTheme {
                val entryPoint by viewModel.entryPoint.collectAsStateWithLifecycle()
                val updateState by viewModel.updateState.collectAsStateWithLifecycle()
                val isMigrating by viewModel.isMigrating.collectAsStateWithLifecycle()
                val migrationError by viewModel.migrationError.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                val activity = this@MainActivity

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

                // This Scaffold only hosts the update snackbar; it must not reserve any system
                // bar insets for itself, otherwise AppNavigation's own Scaffold (and every
                // screen's own statusBarsPadding()/navigationBarsPadding() calls) would see
                // those insets as already consumed and render flush under the status/nav bars.
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    when {
                        migrationError != null -> {
                            // Pantalla de Error en Base de Datos (Única UI bloqueante ahora)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .systemBarsPadding(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Error iniciando la base de datos:\n$migrationError", 
                                    color = Color.Red
                                )
                            }
                        }
                        entryPoint != null && !isMigrating -> {
                            // Navegación Normal (Aparece cuando el SplashScreen se oculta y no hay error)
                            // Sin sesión no se entra a la app, aunque el usuario ya haya pagado.
                            val startDestination: Any = when (entryPoint) {
                                AppEntryPoint.HOME -> HomeRoute
                                AppEntryPoint.LOGIN -> AuthRoute(isMandatory = true)
                                AppEntryPoint.PAYWALL -> PaywallRoute
                                else -> GreetingsRoute
                            }
                            key(entryPoint) {
                                AppNavigation(startDestination = startDestination)
                            }
                        }
                    }
                }
            }
        }
    }

    /** Resumes any Play update that was downloaded or interrupted while backgrounded. */
    override fun onResume() {
        super.onResume()
        // Check for pending downloads or interrupted immediate updates
        viewModel.checkForDownloadedUpdate()
    }
}
