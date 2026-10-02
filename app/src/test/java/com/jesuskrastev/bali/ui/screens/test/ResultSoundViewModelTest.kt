package com.jesuskrastev.bali.ui.screens.test

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.util.FakeSoundEffects
import org.junit.Test

/** Which sound each kind of result asks the sound effects for. */
class ResultSoundViewModelTest {

    private val sounds = FakeSoundEffects()
    private val viewModel = ResultSoundViewModel(sounds)

    @Test
    fun `the fanfare plays the lesson complete sound once`() {
        viewModel.play(ResultSound.LESSON_COMPLETE)

        assertThat(sounds.lessonCompletePlays).isEqualTo(1)
        assertThat(sounds.softFinishPlays).isEqualTo(0)
    }

    @Test
    fun `a result that is not celebrated plays only the soft tone`() {
        viewModel.play(ResultSound.SOFT_FINISH)

        assertThat(sounds.softFinishPlays).isEqualTo(1)
        assertThat(sounds.lessonCompletePlays).isEqualTo(0)
    }

    @Test
    fun `a failed exam asks for the soft tone and a passed one for the fanfare`() {
        val failed = ResultTier.of(ResultKind.EXAM, accuracy = 86, score = 26, total = 30)
        val passed = ResultTier.of(ResultKind.EXAM, accuracy = 90, score = 27, total = 30)

        viewModel.play(failed.sound)
        assertThat(sounds.lessonCompletePlays).isEqualTo(0)
        assertThat(sounds.softFinishPlays).isEqualTo(1)

        viewModel.play(passed.sound)
        assertThat(sounds.lessonCompletePlays).isEqualTo(1)
    }
}
