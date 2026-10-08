package com.jesuskrastev.bali.ui.screens.streak

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.ui.theme.BaliTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class StreakScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /** Wednesday 7 October 2026, 18:00: a fixed "now" so the week looks the same every run. */
    private val now = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 7, 18, 0) }.timeInMillis

    /** Local midnight of a day relative to [now]. */
    private fun day(offset: Int) = DailyStreak.startOfDayMillis(DailyStreak.epochDay(now) + offset)

    /** Studied Monday and today, with Tuesday saved by a freeze. */
    private val studiedToday = User(
        currentStreak = 12,
        highestStreak = 15,
        streakFreezes = 1,
        lastPracticeTimestamp = now,
        practiceDays = listOf(day(-2), day(0)),
        frozenDays = listOf(day(-1))
    )

    /** Studied Monday and Tuesday, not yet today. */
    private val atRisk = User(
        currentStreak = 4,
        highestStreak = 4,
        lastPracticeTimestamp = day(-1),
        practiceDays = listOf(day(-2), day(-1))
    )

    /** Studied Monday, nothing yesterday: the 12-day streak is lost and can be bought back today. */
    private val lostYesterday = User(
        currentStreak = 12,
        highestStreak = 15,
        lastPracticeTimestamp = day(-2),
        practiceDays = listOf(day(-2))
    )

    private fun captureStreak(user: User, darkTheme: Boolean = false) {
        composeTestRule.setContent {
            BaliTheme(darkTheme = darkTheme) {
                StreakContent(uiState = streakUiStateOf(user, now), onBackClick = {}, onShopClick = {})
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun captureStreakScreen_studiedToday() = captureStreak(studiedToday)

    @Test
    fun captureStreakScreen_atRisk() = captureStreak(atRisk)

    @Test
    fun captureStreakScreen_noStreak() = captureStreak(User())

    @Test
    fun captureStreakScreen_lostYesterday() = captureStreak(lostYesterday)

    @Test
    fun captureStreakScreen_lostYesterday_darkTheme() = captureStreak(lostYesterday, darkTheme = true)

    @Test
    fun captureStreakScreen_darkTheme() = captureStreak(studiedToday, darkTheme = true)

    @Test
    fun captureLessonStreakScreen_newRecord() {
        val user = atRisk.copy(
            currentStreak = 5,
            highestStreak = 5,
            streakFreezes = 2,
            lastPracticeTimestamp = now,
            practiceDays = atRisk.practiceDays + day(0)
        )
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            BaliTheme(darkTheme = false) {
                LessonStreakContent(uiState = streakUiStateOf(user, now), onContinueClick = {})
            }
        }
        composeTestRule.mainClock.advanceTimeBy(2_000)
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun captureLessonStreakScreen_milestone() {
        val user = atRisk.copy(
            currentStreak = 7,
            highestStreak = 7,
            lastPracticeTimestamp = now,
            practiceDays = atRisk.practiceDays + day(0)
        )
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            BaliTheme(darkTheme = false) {
                LessonStreakContent(uiState = streakUiStateOf(user, now), onContinueClick = {})
            }
        }
        composeTestRule.mainClock.advanceTimeBy(3_000)
        composeTestRule.onRoot().captureRoboImage()
    }
}
