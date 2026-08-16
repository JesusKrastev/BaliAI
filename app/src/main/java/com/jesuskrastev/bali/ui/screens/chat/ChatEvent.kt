package com.jesuskrastev.bali.ui.screens.chat

/** User actions available on the AI tutor chat screen. */
sealed class ChatEvent {
    /** The user edited the input field. */
    data class DraftChanged(val text: String) : ChatEvent()

    /** The user sent whatever is currently in the input field. */
    data object SendDraft : ChatEvent()

    /** The user tapped one of the suggested starter questions. */
    data class SendSuggestion(val question: String) : ChatEvent()

    /** The user asked to re-send the question whose answer failed. */
    data object RetryFailed : ChatEvent()

    /** The user dismissed the error banner. */
    data object DismissError : ChatEvent()

    /** The user tapped "delete conversation". */
    data object RequestClear : ChatEvent()

    /** The user confirmed deleting the conversation. */
    data object ConfirmClear : ChatEvent()

    /** The user backed out of deleting the conversation. */
    data object CancelClear : ChatEvent()
}
