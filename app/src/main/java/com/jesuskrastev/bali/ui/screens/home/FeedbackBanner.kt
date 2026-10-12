package com.jesuskrastev.bali.ui.screens.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.review.FeedbackPromptViewModel
import kotlinx.coroutines.delay

/** Where the student reaches the team. */
internal object SupportContact {
    /** Support WhatsApp number in international format (Spain, +34), digits only, as `wa.me` expects. */
    const val WHATSAPP_NUMBER = "34642040566"
}

private val WhatsAppGreen = Color(0xFF1FA855)
private const val APPEAR_DELAY_MILLIS = 600L

/**
 * Green banner pinned above Home's status row that thanks the student for their opinion and
 * opens a WhatsApp chat with the team. It slides in a moment after Home opens, and is shown only
 * while a feedback invitation is pending (raised by an excellent result).
 *
 * @param modifier layout modifier applied to the banner's container
 * @param viewModel supplies and clears the pending invitation
 */
@Composable
fun FeedbackBanner(
    modifier: Modifier = Modifier,
    viewModel: FeedbackPromptViewModel = hiltViewModel()
) {
    val isPending by viewModel.isPending.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var armed by remember { mutableStateOf(false) }

    // The short wait lets Home settle first, so the banner is noticed arriving.
    LaunchedEffect(Unit) {
        delay(APPEAR_DELAY_MILLIS)
        armed = true
    }

    AnimatedVisibility(
        visible = isPending && armed,
        modifier = modifier,
        enter = expandVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)) +
            slideInVertically(animationSpec = tween(450)) { -it } +
            fadeIn(tween(350)),
        exit = shrinkVertically(tween(250)) + slideOutVertically(tween(250)) { -it } + fadeOut(tween(200))
    ) {
        FeedbackBannerContent(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            onContactClick = {
                openWhatsApp(context)
                viewModel.dismiss()
            },
            onDismissClick = viewModel::dismiss
        )
    }
}

/**
 * The banner's card: WhatsApp logo, message and, when [onDismissClick] is given, a close button.
 *
 * @param modifier outer spacing; the card itself fills the width it is given
 * @param onContactClick invoked when the card is tapped, to open the chat
 * @param onDismissClick invoked when the close button is tapped; null hides the button
 */
@Composable
fun FeedbackBannerContent(
    modifier: Modifier = Modifier,
    onContactClick: () -> Unit,
    onDismissClick: (() -> Unit)? = null
) {
    val pulse = rememberInfiniteTransition(label = "feedback_logo")
    val logoScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "feedback_logo_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WhatsAppGreen)
            .clickable(role = Role.Button, onClick = onContactClick)
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = if (onDismissClick != null) 4.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer {
                    scaleX = logoScale
                    scaleY = logoScale
                }
                .background(Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_whatsapp),
                contentDescription = "WhatsApp",
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Tu opinión es muy valiosa para nosotros",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 19.sp
            )
            Text(
                text = "Si necesitas cualquier cosa, escríbenos por WhatsApp.",
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 13.sp,
                lineHeight = 17.sp
            )
        }
        if (onDismissClick != null) {
            IconButton(onClick = onDismissClick) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Opens the team's WhatsApp chat, falling back silently when no app can handle the link.
 *
 * @param context used to start the intent
 */
fun openWhatsApp(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${SupportContact.WHATSAPP_NUMBER}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No WhatsApp nor browser installed: nothing sensible to open.
    }
}
