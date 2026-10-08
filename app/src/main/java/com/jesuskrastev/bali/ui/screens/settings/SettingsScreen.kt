package com.jesuskrastev.bali.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.jesuskrastev.bali.domain.model.NotificationCategory
import com.jesuskrastev.bali.ui.util.LegalLinks
import com.jesuskrastev.bali.ui.util.SupportLinks
import com.jesuskrastev.bali.ui.util.replayMask
import com.jesuskrastev.bali.BuildConfig

/**
 * Published applicationId listed on the Play Store, used to link out to the review page.
 * Deliberately hardcoded rather than read from the running build's package name, since debug
 * builds run under a ".debug"-suffixed id ([app/build.gradle.kts]) that has no Play listing.
 */
private const val PLAY_STORE_PACKAGE = "com.jesuskrastev.bali"

/**
 * Starts [intent], swallowing the failure when no installed app can handle it (no mail client,
 * no browser) instead of crashing the settings screen.
 *
 * @param intent intent to launch
 * @return true if an activity was started, false if none could handle it
 */
private fun Context.startActivityOrFalse(intent: Intent): Boolean =
    try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }

/**
 * Opens [url] in the default handler, ignoring the case where nothing can open it.
 *
 * @param url absolute URL or URI to view
 */
private fun Context.openLink(url: String) {
    startActivityOrFalse(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

/**
 * Settings destination exposed by the persistent bottom navigation. Hosts the account section
 * (profile, subscription management, sign out) plus the preference rows — legal links,
 * feedback — that used to live in Home's side drawer, laid out as labeled, grouped sections
 * like a native settings page.
 *
 * @param modifier layout modifier applied to the root column
 * @param viewModel supplies the signed-in profile and the sign-out action
 * @param onAuthClick navigates to sign-in when the viewer is signed out
 * @param onFeedbackClick navigates to the "send feedback" destination
 * @param onManageSubscriptionClick opens the "Tu suscripción" screen
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onAuthClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
    onManageSubscriptionClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // The user switches each channel in the system settings, outside the app.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshNotificationChannels() }
    LaunchedEffect(viewModel) {
        viewModel.openSystemNotificationSettings.collect { context.openAppNotificationSettings() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        SettingsHeader()

        ProfileCard(uiState = uiState, onAuthClick = onAuthClick)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsSectionLabel("Notificaciones")
            SettingsGroup(
                listOf(
                    SettingsRowSpec(
                        icon = if (uiState.notificationsEnabled) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff,
                        label = "Recibir notificaciones",
                        description = if (uiState.notificationsEnabled) {
                            "Elige abajo qué avisos quieres"
                        } else {
                            "No te avisaremos a tu hora de estudio ni si tu racha está en peligro"
                        },
                        checked = uiState.notificationsEnabled,
                        onClick = { viewModel.setNotificationsEnabled(!uiState.notificationsEnabled) }
                    )
                )
            )
            // Each kind has its own Android channel; they only matter while notifications are on.
            AnimatedVisibility(
                visible = uiState.notificationsEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SettingsGroup(
                    NotificationCategory.entries.map { category ->
                        val isOn = category !in uiState.disabledNotificationCategories
                        SettingsRowSpec(
                            icon = category.icon(),
                            label = category.title,
                            description = "${if (isOn) "Activado" else "Desactivado"} · ${category.description}",
                            onClick = { context.openNotificationSettings(category) }
                        )
                    }
                )
            }
        }

        SettingsSection(
            title = "Preferencias",
            items = listOf(
                SettingsRowSpec(
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    label = "Sonidos",
                    checked = uiState.soundsEnabled,
                    onClick = { viewModel.setSoundsEnabled(!uiState.soundsEnabled) }
                )
            )
        )

        WhatsAppFeedbackCard(onClick = { context.openLink(SupportLinks.whatsappUrl()) })

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
                        context.startActivityOrFalse(emailIntent)
                    }
                ),
                SettingsRowSpec(
                    icon = Icons.Rounded.Star,
                    label = "Valorar la app",
                    onClick = {
                        val openedInPlayStore = context.startActivityOrFalse(
                            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PLAY_STORE_PACKAGE"))
                        )
                        if (!openedInPlayStore) {
                            context.openLink("https://play.google.com/store/apps/details?id=$PLAY_STORE_PACKAGE")
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
                    onClick = { context.openLink(LegalLinks.PRIVACY) }
                ),
                SettingsRowSpec(
                    icon = Icons.Rounded.Description,
                    label = "Términos y condiciones",
                    onClick = { context.openLink(LegalLinks.TERMS) }
                )
            )
        )

        SettingsSection(
            title = "Suscripción",
            items = listOf(
                SettingsRowSpec(
                    icon = Icons.Rounded.CreditCard,
                    label = "Gestionar suscripción",
                    onClick = onManageSubscriptionClick
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
            Column(Modifier.weight(1f)) {
                Text(
                    uiState.userName,
                    modifier = Modifier.replayMask(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    uiState.userEmail ?: "",
                    modifier = Modifier.replayMask(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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

/**
 * One tappable row inside a [SettingsGroup]: leading icon, label, and the action it triggers.
 *
 * @property checked null for a plain row that ends in a chevron; true or false for a switch row,
 *   which shows a [Switch] in that position. The whole row is tappable either way, so [onClick]
 *   is also what flips the switch.
 * @property description smaller text under the label, or null for a one-line row
 */
private data class SettingsRowSpec(
    val icon: ImageVector,
    val label: String,
    val checked: Boolean? = null,
    val description: String? = null,
    val onClick: () -> Unit
)

/**
 * The glyph Settings shows next to this notification category.
 *
 * @return the icon for the category's row
 */
private fun NotificationCategory.icon(): ImageVector = when (this) {
    NotificationCategory.STUDY -> Icons.Rounded.NotificationsActive
    NotificationCategory.STREAK -> Icons.Rounded.LocalFireDepartment
    NotificationCategory.PROMOTIONS -> Icons.Rounded.LocalOffer
}

/**
 * Opens Android's settings for [category]'s channel, where the user switches that kind of
 * notification on or off. Below Android 8 there are no channels, and some manufacturers' builds
 * have no channel screen: both fall back to [openAppNotificationSettings].
 *
 * @param category the kind of notification to configure
 */
private fun Context.openNotificationSettings(category: NotificationCategory) {
    val openedChannel = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && startActivityOrFalse(
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, category.channelId)
    )
    if (!openedChannel) openAppNotificationSettings()
}

/**
 * Opens the app's notification settings in Android, where the user lets notifications through
 * when the system dialog will not show again; failing that, the app's details screen, which
 * every Android has and links to them.
 */
private fun Context.openAppNotificationSettings() {
    val openedNotifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && startActivityOrFalse(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    )
    if (!openedNotifications) {
        startActivityOrFalse(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        )
    }
}

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
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        checked = item.checked,
                        description = item.description
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
 * @param checked null to end the row with a chevron; otherwise the state of the [Switch] shown
 *   instead. The switch only displays the state: the tap is handled by the row around it.
 * @param description optional smaller text under the label
 */
@Composable
private fun SettingsRowContent(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    labelColor: Color,
    checked: Boolean? = null,
    description: String? = null
) {
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = labelColor
            )
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (checked != null) {
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = null)
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
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

/**
 * Permanent invitation to chat with the team on WhatsApp. It lives in Settings, a root destination
 * that is always one tap away, so it is visible every time without ever getting in the way of
 * studying or playing.
 *
 * @param onClick opens the team's WhatsApp chat
 */
@Composable
private fun WhatsAppFeedbackCard(onClick: () -> Unit) {
    val green = Color(0xFF25D366)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, green.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(green.copy(alpha = 0.22f), green.copy(alpha = 0.06f))))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = CircleShape, color = green, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Forum, contentDescription = null, tint = Color.White)
                    }
                }
                Text(
                    text = "Tu opinión vale oro",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                text = "Queremos saber qué piensas de Bali: qué te gusta, qué te falla y qué echas de menos. " +
                    "Escríbenos directamente por WhatsApp; leemos todos los mensajes y tu feedback nos ayuda a seguir mejorando la app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(shape = CircleShape, color = green, contentColor = Color.White) {
                Text(
                    text = "Escribir por WhatsApp",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    fontWeight = FontWeight.Black
                )
            }
        }
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
