package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.jesuskrastev.bali.ui.theme.BaliTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Captures each hand-drawn scene of the problem → risk → solution block at a moment where it reads:
 * mid-loop for the problem and risk scenes, fully solved for the solution.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h400dp-xhdpi")
@RunWith(RobolectricTestRunner::class)
class NarrativeScenesScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun capture(scene: NarrativeScene, atMillis: Long, name: String) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            BaliTheme(darkTheme = true) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    NarrativeSceneView(scene, Modifier.fillMaxWidth().height(260.dp))
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(atMillis)
        composeTestRule.onRoot().captureRoboImage("build/onboarding-steps/scene_$name.png")
    }

    @Test
    fun topicPile() = capture(NarrativeScene.TopicPile, 3_000, "topic_pile")

    @Test
    fun sameMistake() = capture(NarrativeScene.SameMistake, 2_400, "same_mistake")

    @Test
    fun scatteredWeek() = capture(NarrativeScene.ScatteredWeek, 1_500, "scattered_week")

    @Test
    fun coinFlipApto() = capture(NarrativeScene.CoinFlip, 300, "coin_flip_apto")

    @Test
    fun coinFlipNoApto() = capture(NarrativeScene.CoinFlip, 1_400, "coin_flip_no_apto")

    @Test
    fun mismatch() = capture(NarrativeScene.Mismatch, 1_300, "mismatch")

    @Test
    fun trapWord() = capture(NarrativeScene.TrapWord, 2_200, "trap_word")

    @Test
    fun solved() = capture(
        NarrativeScene.Solved(
            listOf(
                SolvedRow("Estudio y no avanzo", "Ves cuánto te falta para aprobar"),
                SolvedRow("Que el examen me pille por sorpresa", "Simulacros de 30 preguntas, como el real"),
                SolvedRow("Fallaste: Agentes y Semáforos", "Lo reforzamos en tu plan")
            )
        ),
        4_000,
        "solved"
    )

    @Test
    fun solvingMidway() = capture(
        NarrativeScene.Solved(listOf(SolvedRow("No sé por dónde empezar", "Un camino ordenado, tema a tema"))),
        650,
        "solving"
    )
}
