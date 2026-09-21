package com.jesuskrastev.bali.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.jesuskrastev.bali.ui.util.replayMask
import com.jesuskrastev.bali.BuildConfig

/**
 * Published applicationId listed on the Play Store, used to link out to the review page.
 * Deliberately hardcoded rather than read from the running build's package name, since debug
 * builds run under a ".debug"-suffixed id ([app/build.gradle.kts]) that has no Play listing.
 */
private const val PLAY_STORE_PACKAGE = "com.jesuskrastev.bali"

/**
 * Settings destination exposed by the persistent bottom navigation. Hosts the account section
 * (profile, sign out) plus the preference rows — legal links, feedback — that used to live in
 * Home's side drawer, laid out as labeled, grouped sections like a native settings page.
 *
 * @param modifier layout modifier applied to the root column
 * @param viewModel supplies the signed-in profile and the sign-out action
 * @param onAuthClick navigates to sign-in when the viewer is signed out
 * @param onFeedbackClick navigates to the "send feedback" destination
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onAuthClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        SettingsHeader()

        ProfileCard(uiState = uiState, onAuthClick = onAuthClick)

        SettingsSection(
            title = "Soporte",
            items = listOf(
                SettingsRowSpec(
                    icon = Icons.Rounded.ChatBubbleOutline,
                    label = "Enviar sugerencia",
                    onClick = onFeedbackClick
                ),
                SettingsRowSpec(
                    icon = Icons.Rounded.BugReport,
                    label = "Informar de un error",
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:appbali@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Error en Bali")
                        }
                        context.startActivity(emailIntent)
                    }
                ),
                SettingsRowSpec(
                    icon = Icons.Rounded.Star,
                    label = "Valorar la app",
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PLAY_STORE_PACKAGE")))
                        } catch (e: ActivityNotFoundException) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$PLAY_STORE_PACKAGE")))
                        }
                    }
                )
            )
        )

        SettingsSection(
            title = "Legal",
            items = listOf(
                SettingsRowSpec(
                    icon = Icons.Rounded.Security,
                    label = "Política de privacidad",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://baliaipage.vercel.app/privacidad.html")))
                    }
                ),
                SettingsRowSpec(
                    icon = Icons.Rounded.Description,
                    label = "Términos y condiciones",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://baliaipage.vercel.app/terminos.html")))
                    }
                )
            )
        )

        if (uiState.isLoggedIn) {
            SettingsActionCard(
                icon = Icons.AutoMirrored.Rounded.Logout,
                label = "Cerrar sesión",
                isDestructive = true,
                onClick = { showLogoutConfirmDialog = true }
            )
        }

        SettingsFooter()
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Cerrar sesión", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que quieres cerrar sesión? Tu progreso se sincronizará la próxima vez que entres.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.signOut(context)
                    }
                ) {
                    Text("CERRAR SESIÓN", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("CANCELAR", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

/**
 * Screen title and short subtitle shown above the profile card, left-aligned in `headlineLarge`
 * to match the other bottom-navigation root screens (Home, Games) instead of a centered dialog-style title.
 */
@Composable
private fun SettingsHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Tu cuenta y preferencias",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Shows the signed-in profile (avatar, name, email), or a sign-in prompt when signed out.
 * Sits above the grouped [SettingsSection]s as the account header, like a native settings page.
 *
 * @param uiState current [SettingsUiState] driving the avatar/name/email or the signed-out copy
 * @param onAuthClick invoked when a signed-out viewer taps the row to start sign-in
 */
@Composable
private fun ProfileCard(uiState: SettingsUiState, onAuthClick: () -> Unit) {
    if (uiState.isLoggedIn) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            ProfileCardContent(uiState)
        }
    } else {
        Surface(
            onClick = onAuthClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            ProfileCardContent(uiState)
        }
    }
}

/**
 * Lays out the avatar plus name/email (or the sign-in prompt) shared by both [ProfileCard]
 * branches, so the clickable/non-clickable [Surface] wrapper doesn't duplicate this content.
 *
 * @param uiState current [SettingsUiState] driving the avatar/name/email or the signed-out copy
 */
@Composable
private fun ProfileCardContent(uiState: SettingsUiState) {
    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        if (uiState.profilePictureUrl != null) {
            AsyncImage(
                model = uiState.profilePictureUrl,
                contentDescription = "Foto de perfil",
                modifier = Modifier.size(56.dp).clip(CircleShape).replayMask(),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        if (uiState.isLoggedIn) {
            Column {
                Text(
                    uiState.userName,
                    modifier = Modifier.replayMask(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    uiState.userEmail ?: "",
                    modifier = Modifier.replayMask(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        } else {
            Column(Modifier.weight(1f)) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                            append("Regístrate")
                        }
                        append(" o ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                            append("inicia sesión")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "para sincronizar tu progreso",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/** One tappable row inside a [SettingsGroup]: leading icon, label, and the action it triggers. */
private data class SettingsRowSpec(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

/**
 * Small uppercase eyebrow label placed above a [SettingsGroup] — the same treatment used for
 * "CONSEJO DEL DÍA" on Home — so related rows read as one cluster, like a native settings page.
 *
 * @param title section name, upper-cased for display
 */
@Composable
private fun SettingsSectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 8.dp)
    )
}

/**
 * Labeled group of settings rows: a [SettingsSectionLabel] above a single rounded [SettingsGroup]
 * surface, mirroring how native Android/iOS settings pages cluster related preferences.
 *
 * @param title section name shown above the group
 * @param items rows rendered inside the group, in order
 */
@Composable
private fun SettingsSection(title: String, items: List<SettingsRowSpec>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingsSectionLabel(title)
        SettingsGroup(items)
    }
}

/**
 * Renders [items] as one rounded surface with a thin inset divider between rows — the grouped
 * list look shared by native settings screens — instead of every row floating as its own card.
 *
 * @param items rows rendered inside the group, in order
 */
@Composable
private fun SettingsGroup(items: List<SettingsRowSpec>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column {
            items.forEachIndexed { index, item ->
                Surface(onClick = item.onClick, color = Color.Transparent) {
                    SettingsRowContent(
                        icon = item.icon,
                        label = item.label,
                        accentColor = MaterialTheme.colorScheme.primary,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (index != items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 76.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                }
            }
        }
    }
}

/**
 * Leading icon badge, label and trailing chevron shared by every settings row — both the ones
 * grouped inside a [SettingsGroup] and the standalone [SettingsActionCard].
 *
 * @param icon leading glyph, tinted with [accentColor]
 * @param label row name
 * @param accentColor color applied to the icon
 * @param labelColor color applied to the label text
 */
@Composable
private fun SettingsRowContent(icon: ImageVector, label: String, accentColor: Color, labelColor: Color) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

/**
 * Standalone full-width action card used for sign-out, kept isolated from the grouped
 * [SettingsSection]s the way native settings pages separate destructive account actions.
 *
 * @param icon leading glyph for the action, tinted with the accent color
 * @param label action name
 * @param isDestructive true for destructive actions like signing out — tints the badge, icon
 *   and label with the theme's error color instead of the normal primary/on-surface colors
 * @param onClick invoked when the row is tapped
 */
@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val accentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val labelColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        SettingsRowContent(icon = icon, label = label, accentColor = accentColor, labelColor = labelColor)
    }
}

/** Version number anchoring the bottom of the settings list. */
@Composable
private fun SettingsFooter() {
    Text(
        text = "Versión ${BuildConfig.VERSION_NAME}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp)
    )
}
