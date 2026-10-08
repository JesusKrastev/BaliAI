package com.jesuskrastev.bali.ui.screens.stats

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.ExamRules
import com.jesuskrastev.bali.domain.model.ProgressStats
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.usecase.CalculateProgressStatsUseCase
import com.jesuskrastev.bali.domain.usecase.CalculateReadinessUseCase
import com.jesuskrastev.bali.ui.theme.BaliTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar
import java.util.Date

/** Captures the statistics screen for each verdict, plus a brand-new user and the dark theme. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h3000dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class StatsScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /** Wednesday 7 October 2026, 18:00: a fixed "now" so the figures look the same every run. */
    private val now = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 7, 18, 0) }.timeInMillis

    private val useCase = CalculateProgressStatsUseCase(CalculateReadinessUseCase())

    /** Local midnight of a day relative to [now]. */
    private fun day(offset: Int) = DailyStreak.startOfDayMillis(DailyStreak.epochDay(now) + offset)

    private fun exam(score: Int, daysAgo: Int) = TestResult(
        "", ExamRules.OFFICIAL_EXAM_CATEGORY, score, 30,
        Date(day(-daysAgo) + 10 * 3_600_000L), ExamRules.isPassed(score)
    )

    private fun practice(category: String, score: Int, daysAgo: Int) =
        TestResult("", category, score, 10, Date(day(-daysAgo) + 9 * 3_600_000L), score >= 9)

    /** [perDay] answers a day for the last days, [correctShare] of them right. */
    private fun answers(perDay: List<Int>, correctShare: Float = 0.85f): List<Answer> =
        perDay.reversed().flatMapIndexed { daysAgo, count ->
            (0 until count).map { index ->
                Answer("", "t", "q", 0, index < count * correctShare, Date(day(-daysAgo) + 11 * 3_600_000L))
            }
        }

    private val profile = User(
        level = 7,
        xp = 3480,
        currentStreak = 5,
        highestStreak = 7,
        lastPracticeTimestamp = now,
        examDateMillis = day(12),
        practiceDays = listOf(0, -1, -2, -3, -4, -6, -7, -9, -10, -12, -13, -14, -17, -20, -21, -24).map { day(it) }
    )

    private val topicPractice = listOf(
        practice("Señales", 6, 1), practice("Señales", 7, 4),
        practice("Velocidad", 9, 2), practice("Velocidad", 10, 6),
        practice("Prioridad", 8, 3), practice("Maniobras", 5, 5),
        practice("Marcas viales", 9, 8), practice("Mecánica", 2, 9)
    )

    /** Captures the screen for [stats], counting down to the date [user] has saved. */
    private fun capture(
        stats: ProgressStats,
        user: User = profile,
        darkTheme: Boolean = false,
        path: String? = null
    ) {
        val plan = planSummaryOf(user.examDateMillis, user.planTargetMillis, now)
        composeTestRule.setContent {
            BaliTheme(darkTheme = darkTheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    StatsContent(stats = stats, plan = plan, onDateClick = {})
                }
            }
        }
        if (path == null) composeTestRule.onRoot().captureRoboImage() else composeTestRule.onRoot().captureRoboImage(path)
    }

    /** The statistics as the onboarding intro shows them: phone-sized, dark, ready to pass. */
    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h780dp-xhdpi")
    fun captureOnboardingShowcase() = capture(
        useCase(
            listOf(exam(28, 12), exam(29, 8), exam(29, 5), exam(30, 3), exam(30, 1)) + topicPractice,
            answers(listOf(0, 0, 24, 30, 0, 40, 30)), profile, now
        ),
        darkTheme = true,
        path = "build/onboarding-showcase/shot_stats.png"
    )

    @Test
    fun captureStatsScreen_ready() = capture(
        useCase(
            listOf(exam(28, 12), exam(29, 8), exam(29, 5), exam(30, 3), exam(30, 1)) + topicPractice,
            answers(listOf(0, 0, 24, 30, 0, 40, 30)), profile, now
        )
    )

    @Test
    fun captureStatsScreen_almost() = capture(
        useCase(
            listOf(exam(27, 6), exam(28, 4), exam(28, 2), exam(29, 1), exam(28, 0)) + topicPractice,
            answers(listOf(10, 0, 20, 30, 0, 0, 30), 0.78f), profile, now
        )
    )

    @Test
    fun captureStatsScreen_notYet() {
        val user = profile.copy(examDateMillis = day(3))
        capture(
            useCase(
                listOf(exam(17, 9), exam(20, 5), exam(21, 2)) + topicPractice.take(3),
                answers(listOf(0, 0, 0, 10, 0, 20, 30), 0.6f), user, now
            ),
            user
        )
    }

    @Test
    fun captureStatsScreen_notEnoughData() {
        val user = User(
            level = 2, xp = 240, currentStreak = 1, highestStreak = 1,
            practiceDays = listOf(day(0), day(-1)), planTargetMillis = day(30)
        )
        capture(
            useCase(listOf(exam(23, 1)) + topicPractice.take(2), answers(listOf(0, 0, 0, 0, 0, 10, 30)), user, now),
            user
        )
    }

    @Test
    fun captureStatsScreen_longHistory() = capture(
        useCase(
            listOf(18, 21, 20, 24, 23, 26, 25, 27, 24, 28, 26, 29, 28)
                .mapIndexed { index, score -> exam(score, daysAgo = 25 - index * 2) } + topicPractice,
            answers(listOf(10, 0, 20, 30, 0, 0, 30), 0.82f), profile, now
        )
    )

    @Test
    fun captureStatsScreen_newUser() = capture(useCase(emptyList(), emptyList(), User(), now), User())

    @Test
    fun captureStatsScreen_darkTheme() = capture(
        useCase(
            listOf(exam(22, 12), exam(25, 8), exam(28, 4), exam(28, 2), exam(27, 0)) + topicPractice,
            answers(listOf(10, 0, 20, 30, 0, 0, 30), 0.78f), profile, now
        ),
        darkTheme = true
    )
}
