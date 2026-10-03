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

    /** Plays the short fanfare of a result worth celebrating: a passed lesson, game or exam. */
    fun playLessonComplete()

    /**
     * Plays the quiet closing tone of a result that is not celebrated (a failed exam, a low
     * score): it marks the end of the session without a fanfare that would contradict the result.
     */
    fun playSoftFinish()

    /** Plays the creak and shimmer of a surprise chest as its lid opens. */
    fun playChestOpen()
}
