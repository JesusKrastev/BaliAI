package com.jesuskrastev.bali.ui.screens.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.FirstStepReward
import com.jesuskrastev.bali.domain.model.FirstStepTask
import com.jesuskrastev.bali.domain.model.FirstStepsProgress
import com.jesuskrastev.bali.ui.theme.BaliTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirstStepsBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val enrolled = FirstStepsProgress.startingAt(1_000L)
    private val startedTasks = mutableListOf<FirstStepTask>()
    private var examClicks = 0
    private var dismissClicks = 0
    private var rewardsShown = 0

    /**
     * Shows the bar with recording callbacks.
     *
     * @param progress the card's progress, or null for an account outside the first-steps window
     * @param reward coins waiting to be celebrated
     * @param isTaskEnabled which pending tasks can start right now
     * @param isExamEnabled whether the closing simulacro can be opened yet
     */
    private fun setBar(
        progress: FirstStepsProgress?,
        reward: FirstStepReward? = null,
        isTaskEnabled: (FirstStepTask) -> Boolean = { true },
        isExamEnabled: Boolean = true,
    ) {
        composeTestRule.setContent {
            BaliTheme(darkTheme = false) {
                FirstStepsBar(
                    progress = progress,
                    reward = reward,
                    onTaskClick = { startedTasks += it },
                    onExamClick = { examClicks++ },
                    onDismissClick = { dismissClicks++ },
                    onRewardShown = { rewardsShown++ },
                    onShown = {},
                    isTaskEnabled = isTaskEnabled,
                    isExamEnabled = isExamEnabled,
                )
            }
        }
    }

    @Test
    fun `the folded bar shows the progress and goes straight to the next task`() {
        setBar(enrolled.copy(completed = setOf(FirstStepTask.FIRST_TEST)))

        composeTestRule.onNodeWithText("Pregunta una duda a Bali").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("1 de 3 completados").assertExists()
        composeTestRule.onNodeWithText("Ir").performClick()

        assertThat(startedTasks).containsExactly(FirstStepTask.ASK_BALI)
    }

    @Test
    fun `the checklist stays folded until the bar is tapped`() {
        setBar(enrolled)

        composeTestRule.onNodeWithText("Juega un minijuego").assertDoesNotExist()
        composeTestRule.onNodeWithText("Tus primeros pasos").performClick()
        composeTestRule.onNodeWithText("Juega un minijuego").assertIsDisplayed().performClick()

        assertThat(startedTasks).containsExactly(FirstStepTask.PLAY_GAME)
    }

    @Test
    fun `hiding from the checklist asks the screen to dismiss the bar`() {
        setBar(enrolled)

        composeTestRule.onNodeWithText("Tus primeros pasos").performClick()
        composeTestRule.onNodeWithText("Ocultar").performClick()

        assertThat(dismissClicks).isEqualTo(1)
    }

    @Test
    fun `while the path is still generating the bar offers a task that can start now`() {
        setBar(enrolled, isTaskEnabled = { it != FirstStepTask.FIRST_TEST })

        composeTestRule.onNodeWithText("Pregunta una duda a Bali").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ir").assertIsEnabled()
    }

    @Test
    fun `with every task done the bar invites to the first simulacro`() {
        setBar(enrolled.copy(completed = FirstStepTask.entries.toSet()))

        composeTestRule.onNodeWithText("¿Aprobarías hoy?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Simulacro").performClick()

        assertThat(examClicks).isEqualTo(1)
    }

    @Test
    fun `the simulacro waits for the first unit and says so`() {
        setBar(enrolled.copy(completed = FirstStepTask.entries.toSet()), isExamEnabled = false)

        composeTestRule.onNodeWithText("Termina tu primera unidad para desbloquearlo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Simulacro").assertIsNotEnabled()
    }

    @Test
    fun `once the first exam is unlocked the simulacro button opens it`() {
        setBar(enrolled.copy(completed = FirstStepTask.entries.toSet()), isExamEnabled = true)

        composeTestRule.onNodeWithText("Simulacro").assertIsEnabled()
    }

    @Test
    fun `a reward is celebrated inside the bar and then handed back`() {
        composeTestRule.mainClock.autoAdvance = false
        setBar(
            progress = enrolled.copy(completed = setOf(FirstStepTask.FIRST_TEST)),
            reward = FirstStepReward(FirstStepTask.FIRST_TEST, coins = 30, completedAll = false),
        )

        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.onNodeWithText("+30 monedas").assertExists()
        assertThat(rewardsShown).isEqualTo(0)

        composeTestRule.mainClock.advanceTimeBy(4_000)
        assertThat(rewardsShown).isEqualTo(1)
    }

    @Test
    fun `nothing is drawn for an account outside the first-steps window`() {
        setBar(progress = null)

        composeTestRule.onNodeWithTag(FIRST_STEPS_BAR_TAG).assertDoesNotExist()
    }
}
