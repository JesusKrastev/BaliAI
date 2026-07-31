package com.jesuskrastev.bali.ui.screens.onboarding.steps

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.launch

private val testimonials = listOf(
    Triple("Sara M.", "Aprobé al primer intento 🎉", "Llevaba meses estudiando sola sin avanzar. Con Bali en 3 semanas lo clavé."),
    Triple("Carlos R.", "Lo recomiendo sin dudar ⭐", "Las explicaciones de la IA son mucho mejores que estudiar el manual. No me aburrí en ningún momento."),
    Triple("Ana G.", "El mejor dinero que he gastado ✨", "Tenía miedo a las preguntas trampa. Bali me enseñó exactamente cómo detectarlas.")
)

/**
 * Walks up the [Context] wrapper chain to find the hosting [Activity].
 * The Play In-App Review flow requires an Activity reference to display its dialog.
 *
 * @return the hosting [Activity], or null if none is found in the chain
 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Opens the app's Google Play Store listing so the user can leave a rating.
 * Used as a fallback when the in-app review flow itself throws (e.g. Play Services unavailable).
 * Falls back further to the Play Store web page if the Play Store app is not installed.
 */
private fun Context.openPlayStoreListing() {
    val uri = Uri.parse("market://details?id=$packageName")
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        startActivity(Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/**
 * Onboarding step showing social proof (rating, testimonials) and a button
 * that triggers Google Play's in-app review dialog without leaving the app.
 *
 * Note: per Play Core's own contract, the review dialog is quota-limited and Google
 * may silently skip showing it even on success — this only surfaces actual failures.
 * It also only ever renders on an app installed from Play (internal testing or higher);
 * a sideloaded/debug build will call through successfully but never show a dialog.
 *
 * @param onRateAppClicked invoked when the user taps the rate button
 */
@Composable
fun StepSocialProof(onRateAppClicked: () -> Unit = {}) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500),
        label = "social_proof_fade"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero stat
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "9.000+",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "alumnos ya aprobaron con Bali",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.Center) {
                    repeat(5) {
                        Text(text = "⭐", fontSize = 20.sp)
                    }
                }
                Text(
                    text = "4.8 / 5 en Google Play",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onRateAppClicked()
                        val activity = context.findActivity()
                        if (activity != null) {
                            coroutineScope.launch {
                                runCatching {
                                    val manager = ReviewManagerFactory.create(context)
                                    val reviewInfo = manager.requestReview()
                                    manager.launchReview(activity, reviewInfo)
                                }.onFailure {
                                    context.openPlayStoreListing()
                                }
                            }
                        } else {
                            context.openPlayStoreListing()
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Filled.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Valóranos en Google Play")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Testimonials
        testimonials.forEachIndexed { index, (name, title, body) ->
            TestimonialCard(name = name, title = title, body = body)
            if (index < testimonials.lastIndex) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TestimonialCard(name: String, title: String, body: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\"$body\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "— $name",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
