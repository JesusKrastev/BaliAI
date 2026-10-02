package com.jesuskrastev.bali.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.ui.util.replayMask
import com.jesuskrastev.bali.domain.model.ChatMessage
import com.jesuskrastev.bali.domain.model.ChatRole

/**
 * Starter questions offered on an empty conversation. They double as an explanation of
 * what the tutor is for — a blank chat box tells the student nothing.
 */
private val SuggestedQuestions = listOf(
    "¿Qué significa la señal R-1?",
    "¿Quién tiene prioridad en una glorieta?",
    "¿Cuál es la tasa de alcohol permitida?",
    "¿A qué velocidad puedo ir en ciudad?",
    "¿Cuándo tengo que usar la baliza V-16?"
)

/**
 * Chat screen where the student asks the AI tutor anything about the DGT theory exam.
 *
 * It is a tab of the bottom bar and the only way in, so there is nothing behind it to go back to
 * and its top bar has no back arrow; the user leaves by choosing another tab.
 *
 * @param viewModel state holder for the conversation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Keep the newest turn in view as the conversation grows. While a question is in
    // flight the typing indicator is the extra last item, so it is what we scroll to.
    LaunchedEffect(uiState.messages.size, uiState.isSending) {
        val itemCount = uiState.messages.size + if (uiState.isSending) 1 else 0
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    if (uiState.showClearConfirmation) {
        ClearConversationDialog(
            onConfirm = { viewModel.onEvent(ChatEvent.ConfirmClear) },
            onDismiss = { viewModel.onEvent(ChatEvent.CancelClear) }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Pregunta a Bali", fontWeight = FontWeight.Black)
                        Text(
                            text = "Tu profesor de teórico 24/7",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (uiState.messages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onEvent(ChatEvent.RequestClear) }) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Borrar conversación"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                draft = uiState.draft,
                canSend = uiState.canSend,
                onDraftChange = { viewModel.onEvent(ChatEvent.DraftChanged(it)) },
                onSendClick = { viewModel.onEvent(ChatEvent.SendDraft) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedVisibility(visible = uiState.error != null) {
                ChatErrorBanner(
                    message = uiState.error.orEmpty(),
                    canRetry = uiState.failedQuestion != null && !uiState.isSending,
                    onRetryClick = { viewModel.onEvent(ChatEvent.RetryFailed) },
                    onDismissClick = { viewModel.onEvent(ChatEvent.DismissError) }
                )
            }

            if (uiState.isEmpty) {
                ChatWelcome(
                    modifier = Modifier.weight(1f),
                    onSuggestionClick = { viewModel.onEvent(ChatEvent.SendSuggestion(it)) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        ChatBubble(message = message)
                    }

                    if (uiState.isSending) {
                        item(key = "typing-indicator") { TypingIndicator() }
                    }
                }
            }
        }
    }
}

/**
 * Empty state: says what the tutor does and offers questions to tap.
 *
 * @param onSuggestionClick invoked with the tapped question
 */
@Composable
private fun ChatWelcome(
    modifier: Modifier = Modifier,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.bali),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "¿Tienes alguna duda?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Pregúntame lo que quieras sobre señales, normas de circulación, " +
                "prioridades o cualquier cosa del teórico. Te lo explico al momento.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = "PARA EMPEZAR",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )

        Spacer(Modifier.height(12.dp))

        SuggestedQuestions.forEach { question ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSuggestionClick(question) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = question,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * One turn of the conversation. Student turns sit right on the brand colour, the tutor's
 * on a neutral surface, which is the fastest way to tell them apart while scrolling.
 *
 * @param message the turn to render
 */
@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Image(
                painter = painterResource(id = R.drawable.bali),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.width(8.dp))
        }

        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            border = if (isUser) {
                null
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            }
        ) {
            Text(
                text = rememberFormattedAnswer(message.content),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).replayMask(),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                color = if (isUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

/**
 * Renders the small subset of markdown the tutor is told to use: `**bold**` spans and
 * `- ` bullets. Anything else is shown verbatim rather than stripped, so a stray symbol
 * never eats part of an answer.
 *
 * @param raw the model's reply
 * @return the styled text to display
 */
@Composable
private fun rememberFormattedAnswer(raw: String): AnnotatedString = remember(raw) {
    buildAnnotatedString {
        val withBullets = raw.lines().joinToString("\n") { line ->
            val trimmed = line.trimStart()
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                "•  " + trimmed.removeRange(0, 2)
            } else {
                line
            }
        }

        var index = 0
        while (index < withBullets.length) {
            val open = withBullets.indexOf(BOLD_MARKER, index)
            if (open == -1) {
                append(withBullets.substring(index))
                break
            }
            val close = withBullets.indexOf(BOLD_MARKER, open + BOLD_MARKER.length)
            if (close == -1) {
                append(withBullets.substring(index))
                break
            }

            append(withBullets.substring(index, open))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(withBullets.substring(open + BOLD_MARKER.length, close))
            }
            index = close + BOLD_MARKER.length
        }
    }
}

private const val BOLD_MARKER = "**"

/** Three pulsing dots shown while the tutor is composing an answer. */
@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.bali),
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
        )

        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(3) { dotIndex ->
                    val alpha by transition.animateFloat(
                        initialValue = 0.25f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse,
                            initialStartOffset = StartOffset(dotIndex * 200)
                        ),
                        label = "dot$dotIndex"
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .alpha(alpha)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}

/**
 * Banner shown when a question went unanswered.
 *
 * @param message the user-facing explanation
 * @param canRetry true when re-asking is possible right now
 * @param onRetryClick invoked to re-ask the failed question
 * @param onDismissClick invoked to hide the banner
 */
@Composable
private fun ChatErrorBanner(
    message: String,
    canRetry: Boolean,
    onRetryClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )

            if (canRetry) {
                TextButton(onClick = onRetryClick) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "REINTENTAR",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                TextButton(onClick = onDismissClick) {
                    Text(
                        text = "CERRAR",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * Bottom input row.
 *
 * @param draft the text currently typed
 * @param canSend whether the send button is enabled
 * @param onDraftChange invoked on every keystroke
 * @param onSendClick invoked when the user sends the draft
 */
@Composable
private fun ChatInputBar(
    draft: String,
    canSend: Boolean,
    onDraftChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe tu duda...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            FilledIconButton(
                onClick = onSendClick,
                enabled = canSend,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Enviar")
            }
        }
    }
}

/**
 * Confirmation for wiping the conversation, which cannot be undone.
 *
 * @param onConfirm invoked when the user accepts
 * @param onDismiss invoked when the user backs out
 */
@Composable
private fun ClearConversationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Borrar conversación", fontWeight = FontWeight.Black) },
        text = { Text("Se eliminarán todos los mensajes de este chat. No se puede deshacer.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("BORRAR", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
