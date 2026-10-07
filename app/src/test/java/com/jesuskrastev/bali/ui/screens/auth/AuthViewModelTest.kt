package com.jesuskrastev.bali.ui.screens.auth

import androidx.lifecycle.SavedStateHandle
import com.jesuskrastev.bali.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat

import org.mockito.kotlin.mock
import com.google.firebase.analytics.FirebaseAnalytics

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeUserRepository = FakeUserRepository()
    private val fakeTestResultRepository = FakeTestResultRepository()
    private val fakeAnswerRepository = FakeAnswerRepository()
    private val fakeAuthRepository = FakeAuthRepository()
    private val fakeAnalyticsTracker = FakeAnalyticsTracker(mock(), mock())
    private val fakeMigrationManager = FakeFirestoreMigrationManager()

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        viewModel = AuthViewModel(
            userRepository = fakeUserRepository,
            testResultRepository = fakeTestResultRepository,
            answerRepository = fakeAnswerRepository,
            authRepository = fakeAuthRepository,
            analyticsTracker = fakeAnalyticsTracker,
            migrationManager = fakeMigrationManager
        )
    }


    @Test
    fun `clearError clears both error messages`() = runTest {
        viewModel.run {
            assertThat(errorMessage.value).isNull()
            assertThat(errorEmail.value).isNull()
            clearError()
            assertThat(errorMessage.value).isNull()
            assertThat(errorEmail.value).isNull()
        }
    }

    @Test
    fun `isLoggingIn starts as false`() = runTest {
        assertThat(viewModel.isLoggingIn.value).isFalse()
    }

    @Test
    fun `errorMessage is initially null`() = runTest {
        assertThat(viewModel.errorMessage.value).isNull()
    }

    @Test
    fun `errorEmail is initially null`() = runTest {
        assertThat(viewModel.errorEmail.value).isNull()
    }
}
