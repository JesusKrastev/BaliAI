package com.jesuskrastev.bali.domain.audio

import kotlinx.coroutines.flow.Flow

/**
 * Short sound effects that confirm what the user just did, plus the user's choice of whether
 * to hear them at all.
 *
 * The play methods never block and never throw: a sound that cannot play must not interrupt a
 * test. They stay silent when [isEnabled] is false.
 */
interface SoundEffects {

    /** Whether the user wants sound effects. On until they switch it off in Settings. */
    val isEnabled: Flow<Boolean>

    /**
     * Stores the user's choice.
     *
     * @param enabled true to play sound effects, false to keep the app silent
     */
    suspend fun setEnabled(enabled: Boolean)

    /** Plays the chime for an answer that was right. */
    fun playCorrect()

    /** Plays the soft sound for an answer that was wrong. */
    fun playWrong()
}
