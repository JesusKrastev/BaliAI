package com.jesuskrastev.bali.ui.screens.chat

import com.jesuskrastev.bali.domain.model.ChatMessage

/**
 * State of the AI tutor chat screen.
 *
 * @property messages the conversation, oldest first, as persisted by the repository.
 * @property draft what the user currently has typed in the input field.
 * @property isSending true while a question is in flight; the screen shows the typing
 *   indicator and blocks a second send.
 * @property error user-facing message when the last question could not be answered.
 * @property failedQuestion the question behind [error], kept so "Reintentar" can re-ask
 *   it without storing it in the transcript a second time.
 * @property isHistoryLoading true until the first emission of the stored conversation
 *   arrives, so an existing chat never flashes the empty state.
 * @property showClearConfirmation true while the "delete conversation" dialog is up.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
    val failedQuestion: String? = null,
    val isHistoryLoading: Boolean = true,
    val showClearConfirmation: Boolean = false
) {
    /** True when the user may send [draft] right now. */
    val canSend: Boolean get() = draft.isNotBlank() && !isSending

    /**
     * True when the welcome state should take over. A question already in flight counts
     * as content: the very first one must show the typing indicator, not bounce the
     * student back to the suggestion list.
     */
    val isEmpty: Boolean get() = messages.isEmpty() && !isHistoryLoading && !isSending
}
