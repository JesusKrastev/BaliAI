package com.jesuskrastev.bali.ui.screens.chat

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.jesuskrastev.bali.domain.model.ChatMessage
import com.jesuskrastev.bali.domain.model.ChatRole
import com.jesuskrastev.bali.domain.usecase.AskDrivingTutorUseCase
import com.jesuskrastev.bali.ui.screens.auth.FakeUserRepository
import com.jesuskrastev.bali.ui.theme.BaliTheme
import com.jesuskrastev.bali.util.MainDispatcherRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Draws the chat tab. It is only reachable from the bottom bar, so its top bar must not show a
 * back arrow: there is nothing behind it to go back to.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class ChatScreenScreenshotTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /** Builds the chat on top of [history], with fakes instead of Firebase and Gemini. */
    private fun chatWith(history: List<ChatMessage>): ChatViewModel {
        val chatRepository = FakeChatRepository(history)
        return ChatViewModel(
            chatRepository = chatRepository,
            askDrivingTutorUseCase = AskDrivingTutorUseCase(
                chatRepository = chatRepository,
                aiTutorRepository = FakeAiTutorRepository(),
                userRepository = FakeUserRepository()
            ),
            analytics = RecordingChatAnalytics()
        )
    }

    private fun showChat(history: List<ChatMessage>, darkTheme: Boolean = false) {
        val viewModel = chatWith(history)
        composeTestRule.setContent {
            BaliTheme(darkTheme = darkTheme) { ChatScreen(viewModel = viewModel) }
        }
    }

    @Test
    fun captureChatScreen_empty_hasNoBackArrow() {
        showChat(emptyList())

        composeTestRule.onNodeWithContentDescription("Atrás").assertDoesNotExist()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun captureChatScreen_withConversation_hasNoBackArrowAndKeepsClearAction() {
        showChat(
            listOf(
                ChatMessage("1", "¿Quién tiene prioridad en una glorieta?", ChatRole.USER, 1_000),
                ChatMessage("2", "En una glorieta tiene **prioridad** quien ya circula por ella.", ChatRole.ASSISTANT, 2_000)
            )
        )

        composeTestRule.onNodeWithContentDescription("Atrás").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Borrar conversación").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun captureChatScreen_darkTheme() {
        showChat(emptyList(), darkTheme = true)

        composeTestRule.onRoot().captureRoboImage()
    }
}
