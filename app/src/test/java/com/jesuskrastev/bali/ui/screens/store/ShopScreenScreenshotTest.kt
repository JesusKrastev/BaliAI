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
}
