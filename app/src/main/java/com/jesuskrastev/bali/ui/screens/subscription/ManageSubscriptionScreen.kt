package com.jesuskrastev.bali.ui.screens.subscription

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** What a subscriber keeps while the plan is active, shown so cancelling is an informed choice. */
private val BENEFITS = listOf(
    "Preparación del teórico de la DGT con explicaciones por IA",
    "XP, rachas y monedas para mantener el ritmo",
    "Tu progreso sincronizado entre dispositivos",
    "Práctica sin conexión (la IA necesita internet)"
)

/**
 * "Gestionar suscripción": a plan card with the real status and next date, what the plan
 * includes, and a single action: cancel (through the in-app cancellation flow) or, once
 * cancelled, reactivate in Google Play.
 *
 * @param onBackClick closes the screen
 * @param onCancelClick opens the cancellation flow
 * @param modifier layout modifier applied to the scaffold
 * @param viewModel supplies the plan status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSubscriptionScreen(
    onBackClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageSubscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Tu suscripción", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            PlanCard(uiState)

            if (uiState is ManageSubscriptionUiState.Active || uiState is ManageSubscriptionUiState.Unavailable) {
                BenefitsCard()
            }

            val active = uiState as? ManageSubscriptionUiState.Active
            when {
                active?.isCancelled == true -> Button(
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(playSubscriptionsUrl(active.productId)))
                            )
                        } catch (_: ActivityNotFoundException) {
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Reactivar plan", fontWeight = FontWeight.Bold)
                }
                active != null || uiState is ManageSubscriptionUiState.Unavailable -> OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Cancelar plan", fontWeight = FontWeight.SemiBold)
                }
            }

            Text(
                "La suscripción se cobra y se cancela desde Google Play. Si cancelas, conservas el acceso hasta que termine el periodo ya pagado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Card with the plan name, a status chip and the next date, or the matching empty/offline copy.
 *
 * @param state what [ManageSubscriptionViewModel] currently knows about the plan
 */
@Composable
private fun PlanCard(state: ManageSubscriptionUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Bali Premium",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                when (state) {
                    is ManageSubscriptionUiState.Active ->
                        StatusChip(state.headline, warning = state.isCancelled || state.headline == "Prueba gratis")
                    ManageSubscriptionUiState.Inactive -> StatusChip("Sin suscripción", warning = true)
                    else -> {}
                }
            }
            when (state) {
                ManageSubscriptionUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
                is ManageSubscriptionUiState.Active -> Text(
                    state.detail,
                    style = MaterialTheme.typography.bodyLarge
                )
                ManageSubscriptionUiState.Inactive -> Text(
                    "No hay ninguna suscripción activa en esta cuenta.",
                    style = MaterialTheme.typography.bodyLarge
                )
                ManageSubscriptionUiState.Unavailable -> Text(
                    "No hemos podido leer el estado de tu plan. Revisa tu conexión; aun así puedes gestionarlo con el botón de abajo.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

/**
 * Small pill shown next to the plan name.
 *
 * @param text status label
 * @param warning true to use the amber tone (cancelled, trial, none) instead of the green one
 */
@Composable
private fun StatusChip(text: String, warning: Boolean) {
    val tone = if (warning) Color(0xFFB26A00) else Color(0xFF1B7F3B)
    Surface(shape = RoundedCornerShape(50), color = tone.copy(alpha = 0.14f)) {
        Text(
            text.uppercase(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
            color = tone
        )
    }
}

/** What the plan includes, as a checklist. */
@Composable
private fun BenefitsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "INCLUYE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BENEFITS.forEach { benefit ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(benefit, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
