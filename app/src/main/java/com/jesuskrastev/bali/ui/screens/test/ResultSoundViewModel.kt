package com.jesuskrastev.bali.ui.screens.test

import androidx.lifecycle.ViewModel
import com.jesuskrastev.bali.domain.audio.SoundEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Gives the result screen a way to make its sound without knowing about audio: the screen reports
 * which [ResultSound] it wants and this plays it. Whether it is played at all is up to
 * [SoundEffects], which stays silent when the user switched sounds off in Settings.
 */
@HiltViewModel
class ResultSoundViewModel @Inject constructor(
    private val soundEffects: SoundEffects
) : ViewModel() {

    /**
     * Plays the sound a result opens with.
     *
     * @param sound the fanfare of a celebrated result or the quiet tone of one that is not
     */
    fun play(sound: ResultSound) {
        when (sound) {
            ResultSound.LESSON_COMPLETE -> soundEffects.playLessonComplete()
            ResultSound.SOFT_FINISH -> soundEffects.playSoftFinish()
        }
    }
}
