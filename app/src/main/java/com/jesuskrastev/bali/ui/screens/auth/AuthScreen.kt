package com.jesuskrastev.bali.ui.screens.auth

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.theme.BaliAccentGreen
import com.jesuskrastev.bali.ui.theme.BaliBackgroundGradient
import com.jesuskrastev.bali.ui.util.LegalLinks
import com.jesuskrastev.bali.ui.util.replayMask

/**
 * Google sign-in gate: a gradient hero with the mascot above a rounded sign-in panel, styled
 * after the Workout app's auth gate.
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (errorEmail != null) {
            AuthUnknownAccountScreen(email = errorEmail, onBackClick = onErrorDismiss)
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                AuthHero(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.40f)
                )
                AuthPanel(
                    isMandatory = isMandatory,
                    isLoggingIn = isLoggingIn,
                    onLoginClick = { if (!isLoggingIn) onLoginClick(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.60f)
                )
            }
        }

        // Floats above both branches so closing the gate is always reachable from wherever
        // the flow currently is, exactly like the original top bar did.
        if (!isMandatory) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Gradient-washed hero that anchors the shared-element mascot above the sign-in panel.
 *
 * @param sharedTransitionScope scope shared with the screen the mascot is animating from
 * @param animatedVisibilityScope visibility scope of this destination inside the shared transition
 * @param modifier layout modifier applied to the hero container
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AuthHero(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(BaliBackgroundGradient()),
        contentAlignment = Alignment.BottomCenter
    ) {
        with(sharedTransitionScope) {
            Image(
                painter = painterResource(id = R.drawable.bali_login_mascot),
                contentDescription = "Bali te da la bienvenida",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxHeight(0.96f)
                    .fillMaxWidth(0.86f)
                    .padding(top = 8.dp)
                    .sharedElement(
                        rememberSharedContentState(key = "bali_mascot"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
            )
        }
    }
}

/**
 * Rounded bottom-sheet panel holding the sign-in copy, benefit checklist, CTA and legal footer.
 *
 * @param isMandatory true when the session is required to keep using the app; swaps in the
 *   gate copy that explains why the account can't be skipped
 * @param isLoggingIn true while the Google credential flow is running
 * @param onLoginClick invoked when the user taps the Google button; re-entrant taps while
 *   [isLoggingIn] is true are already filtered out by the caller
 * @param modifier layout modifier applied to the panel container
 */
@Composable
private fun AuthPanel(
    isMandatory: Boolean,
    isLoggingIn: Boolean,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "TU CUENTA BALI",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isMandatory) "Activa tu acceso\ny continúa" else "Sigue justo\ndonde lo dejaste",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isMandatory) {
                "Inicia sesión para activar tu acceso y guardar tu plan, tus estadísticas y tu progreso. Este paso es necesario para continuar."
            } else {
                "Guarda tus test, tu racha y tu nivel para retomarlos cuando quieras, desde cualquier dispositivo."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        AuthBenefitRow(text = "Conserva tu plan de estudio y tu racha")
        Spacer(modifier = Modifier.height(6.dp))
        AuthBenefitRow(text = "Retoma tu progreso desde cualquier dispositivo")

        Spacer(modifier = Modifier.height(16.dp))

        GoogleSignInButton(isLoggingIn = isLoggingIn, onClick = onLoginClick)

        Spacer(modifier = Modifier.height(12.dp))

        val linkStyles = TextLinkStyles(
            style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = buildAnnotatedString {
                append("Al continuar, aceptas nuestros ")
                withLink(LinkAnnotation.Url(LegalLinks.TERMS, linkStyles)) {
                    append("Términos y Condiciones")
                }
                append(" y la ")
                withLink(LinkAnnotation.Url(LegalLinks.PRIVACY, linkStyles)) {
                    append("Política de Privacidad")
                }
                append(".")
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))
    }
}

/**
 * One checklist row inside the sign-in panel, pairing a success glyph with a short benefit.
 *
 * @param text benefit copy shown next to the check icon
 */
@Composable
private fun AuthBenefitRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = BaliAccentGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Google sign-in call to action; disables itself and shows a spinner while a flow is running.
 *
 * @param isLoggingIn true while the Google credential flow is in progress
 * @param onClick invoked on tap
 */
@Composable
private fun GoogleSignInButton(isLoggingIn: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoggingIn,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoggingIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Iniciando sesión...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Guardar mi progreso con Google",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * Shown instead of the sign-in panel when [AuthViewModel] restricts new accounts and the
 * chosen Google account has no matching Bali profile.
 *
 * @param email the Google account email that was rejected
 * @param onBackClick clears the error and returns to the sign-in panel
 */
@Composable
private fun AuthUnknownAccountScreen(email: String, onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
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

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFFE55D57)),
            contentAlignment = Alignment.Center
        ) {
            val initial = if (email.isNotBlank()) email.first().uppercase() else "?"
            Text(
                text = initial,
                modifier = Modifier.replayMask(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = email,
            modifier = Modifier.replayMask(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = onBackClick,
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
}
