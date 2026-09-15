package com.jesuskrastev.bali.ui.screens.auth

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R

/**
 * Google sign-in screen.
 *
 * @param sharedTransitionScope scope shared with the previous screen for the mascot transition
 * @param animatedVisibilityScope visibility scope of this destination inside the shared transition
 * @param onBackClick invoked when the user closes the screen; unused when [isMandatory] is true
 * @param onLoginClick invoked with the current [Context] to start the Google credential flow
 * @param isLoggingIn true while the credential flow is running
 * @param errorMessage generic error to show in a dialog, or null
 * @param errorEmail email of a Google account with no Bali profile, or null
 * @param onErrorDismiss clears whichever error is on screen
 * @param isMandatory true when the session is required to keep using the app: the close button
 *   and the back gesture are disabled and the copy explains why the account is needed
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AuthScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBackClick: () -> Unit,
    onLoginClick: (Context) -> Unit,
    isLoggingIn: Boolean,
    errorMessage: String?,
    errorEmail: String?,
    onErrorDismiss: () -> Unit,
    isMandatory: Boolean = false
) {
    val context = LocalContext.current
    val activity = LocalActivity.current

    // A mandatory gate has no screen behind it: back leaves the app instead of slipping
    // into a session-less app the user has already paid for.
    BackHandler(enabled = isMandatory) { activity?.finish() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isMandatory) {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (errorEmail != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(0.5f))
                
                Text(
                    text = "No existe un perfil de\nBali vinculado a esa\ncuenta de Google",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(64.dp))
                
                // Avatar Circle
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color(0xFFE55D57)),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = if (errorEmail.isNotBlank()) errorEmail.first().uppercase() else "?"
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Email text
                Text(
                    text = errorEmail,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                TextButton(
                    onClick = onErrorDismiss,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "VOLVER",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        } else {
            if (errorMessage != null) {
                AlertDialog(
                    onDismissRequest = onErrorDismiss,
                    title = { Text("Aviso", fontWeight = FontWeight.Bold) },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        TextButton(onClick = onErrorDismiss) {
                            Text("Entendido", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Large Mascot
            Image(
                painter = painterResource(id = R.drawable.bali),
                contentDescription = "Bali Mascot",
                modifier = Modifier
                    .size(180.dp)
                    .padding(bottom = 16.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Bold Headline
            Text(
                text = if (isMandatory) "Último paso:\ninicia sesión" else "Tu progreso,\nsiempre a salvo",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subheadline
            Text(
                text = if (isMandatory) {
                    "Tu cuenta activa tu acceso y guarda tu plan, tus estadísticas y tu progreso en la nube. Sin ella no podemos continuar."
                } else {
                    "Inicia sesión para guardar tus estadísticas, test realizados y nivel de experiencia en la nube."
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.weight(1.5f))

            // Google Login Button
            Surface(
                onClick = { if (!isLoggingIn) onLoginClick(context) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(60.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isLoggingIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Iniciando sesión...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Continuar con Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Legal Terms
            Text(
                text = buildAnnotatedString {
                    append("Al registrarte o usar una cuenta en la app, aceptas nuestros ")
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
                        append("Términos y Condiciones")
                    }
                    append(" y la ")
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
                        append("Política de Privacidad")
                    }
                    append(".")
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
        }
    }
}
