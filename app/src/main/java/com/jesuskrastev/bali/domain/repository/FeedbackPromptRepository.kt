package com.jesuskrastev.bali.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Remembers whether Home should invite the student to share their opinion (the green WhatsApp
 * banner). The invitation is raised after an excellent result and stays until it is dismissed or
 * answered, so it survives leaving the result screen and restarting the app.
 */
interface FeedbackPromptRepository {

    /** Emits true while the banner is waiting to be shown on Home. */
    val isPending: Flow<Boolean>

    /** Raises the banner, unless the student already dismissed or answered it before. */
    suspend fun markPending()

    /** Hides the banner for good: the student closed it or tapped through to WhatsApp. */
    suspend fun dismiss()
}
