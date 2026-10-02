package com.jesuskrastev.bali.ui.screens.store

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.usecase.DecrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.RecoverStreakUseCase
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.ui.theme.BaliTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class ShopScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * Opens the shop for [user].
     *
     * @param user the profile the shop reads
     * @param darkTheme whether to draw the dark theme
     */
    private fun captureShop(user: User, darkTheme: Boolean = false, openInfoOf: String? = null) {
        val users = FakeUserRepository().apply { runBlocking { insert(user) } }
        val decrement = DecrementCoinsUseCase(users)
        val viewModel = ShopViewModel(users, decrement, RecoverStreakUseCase(users, decrement))
        composeTestRule.setContent {
            BaliTheme(darkTheme = darkTheme) {
                ShopScreen(onBackClick = {}, viewModel = viewModel)
            }
        }
        if (openInfoOf != null) {
            composeTestRule.onNodeWithContentDescription("Más información sobre $openInfoOf").performClick()
            // A dialog is a second window with its own root: capture that one.
            composeTestRule.onAllNodes(isRoot())[1].captureRoboImage()
        } else {
            composeTestRule.onRoot().captureRoboImage()
        }
    }

    /** A streak of 12 days that ended yesterday, so the recovery is on sale. */
    private fun lostYesterday(coins: Int): User {
        val twoDaysAgo = DailyStreak.startOfDayMillis(DailyStreak.epochDay(System.currentTimeMillis()) - 2)
        return User(
            coins = coins,
            currentStreak = 12,
            highestStreak = 12,
            lastPracticeTimestamp = twoDaysAgo,
            practiceDays = listOf(twoDaysAgo)
        )
    }

    @Test
    fun captureShopScreen_streakLostAndAffordable() = captureShop(lostYesterday(coins = 450))

    @Test
    fun captureShopScreen_streakLostWithoutEnoughCoins() = captureShop(lostYesterday(coins = 150))

    @Test
    fun captureShopScreen_nothingToRecover() = captureShop(User(coins = 450))

    @Test
    fun captureShopScreen_infoDialogOfTheDisabledRecovery() =
        captureShop(User(coins = 450), openInfoOf = "Recuperador de racha")

    @Test
    fun captureShopScreen_infoDialogOfTheFreezer() =
        captureShop(lostYesterday(coins = 450), openInfoOf = "Congelador de racha")

    @Test
    fun captureShopScreen_streakLost_darkTheme() = captureShop(lostYesterday(coins = 450), darkTheme = true)

    /** A streak of 12 days that includes today, so the streak bet can be placed. */
    private fun studiedToday(coins: Int, bet: Int = 0, hints: Int = 0): User {
        val today = DailyStreak.startOfDayMillis(DailyStreak.epochDay(System.currentTimeMillis()))
        return User(
            coins = coins,
            currentStreak = 12,
            highestStreak = 12,
            lastPracticeTimestamp = today,
            practiceDays = listOf(today),
            streakBetTarget = bet,
            hints = hints,
            doubleXpBoosts = if (hints > 0) 1 else 0
        )
    }

    // The full list is taller than a phone screen: these captures use a tall window to show all of it.

    @Test
    @Config(qualifiers = "w411dp-h1150dp-xxhdpi")
    fun captureShopScreen_fullList() = captureShop(studiedToday(coins = 450, hints = 2))

    @Test
    @Config(qualifiers = "w411dp-h1150dp-xxhdpi")
    fun captureShopScreen_fullList_streakBetRunning() =
        captureShop(studiedToday(coins = 450, bet = 12 + 7 - 3))

    @Test
    @Config(qualifiers = "w411dp-h1150dp-xxhdpi")
    fun captureShopScreen_fullList_notEnoughCoins() = captureShop(studiedToday(coins = 40))

    @Test
    @Config(qualifiers = "w411dp-h1150dp-xxhdpi")
    fun captureShopScreen_fullList_darkTheme() =
        captureShop(studiedToday(coins = 450, hints = 2), darkTheme = true)

    @Test
    fun captureShopScreen_infoDialogOfTheStreakBet() =
        captureShop(studiedToday(coins = 450), openInfoOf = "Apuesta de racha")

    @Test
    fun captureShopScreen_infoDialogOfTheSurpriseChest() =
        captureShop(studiedToday(coins = 450), openInfoOf = "Cofre sorpresa")
}
