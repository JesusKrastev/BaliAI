package com.jesuskrastev.bali.util

import com.jesuskrastev.bali.domain.audio.SoundEffects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * [SoundEffects] that plays nothing and counts what was asked for. Tests use it instead of the
 * real `SoundPool`, which must not run under Robolectric.
 */
class FakeSoundEffects(enabled: Boolean = true) : SoundEffects {
    private val enabledState = MutableStateFlow(enabled)

    var correctPlays = 0
        private set
    var wrongPlays = 0
        private set
    var lessonCompletePlays = 0
        private set
    var softFinishPlays = 0
        private set
    var chestOpenPlays = 0
        private set

    override val isEnabled: Flow<Boolean> = enabledState

    override suspend fun setEnabled(enabled: Boolean) {
        enabledState.value = enabled
    }

    override fun playCorrect() {
        correctPlays++
    }

    override fun playWrong() {
        wrongPlays++
    }

    override fun playLessonComplete() {
        lessonCompletePlays++
    }

    override fun playSoftFinish() {
        softFinishPlays++
    }

    override fun playChestOpen() {
        chestOpenPlays++
    }
}
