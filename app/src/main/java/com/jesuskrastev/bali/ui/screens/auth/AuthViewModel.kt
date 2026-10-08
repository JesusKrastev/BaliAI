package com.jesuskrastev.bali.ui.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.domain.repository.AnswerRepository
import com.jesuskrastev.bali.domain.repository.TestResultRepository
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.FirstStepsProgress
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.migration.FirestoreMigrationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val testResultRepository: TestResultRepository,
    private val answerRepository: AnswerRepository,
    private val authRepository: AuthRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val migrationManager: FirestoreMigrationManager
) : ViewModel() {

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _errorEmail = MutableStateFlow<String?>(null)
    val errorEmail: StateFlow<String?> = _errorEmail.asStateFlow()

    /** Hides whatever error the last sign-in attempt left on screen. */
    fun clearError() {
        _errorMessage.value = null
        _errorEmail.value = null
    }

    /**
     * Signs in with Google. A brand-new account gets the profile collected so far uploaded as its
     * document, enrolled in the first-steps bar; an existing one just runs its pending migrations.
     * Any failure leaves the screen usable again with an error message instead of a stuck spinner.
     *
     * @param context used to launch the Google account picker.
     * @param restrictNewAccounts when true, an email with no Bali account is rejected instead of
     *   silently creating one.
     * @param onSuccess invoked once the session is open and any new account has been created.
     */
    fun signInWithGoogle(context: Context, restrictNewAccounts: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoggingIn.value = true
            try {
                val signedIn = try {
                    signIn(context, restrictNewAccounts)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    _errorMessage.value = SIGN_IN_FAILED_MESSAGE
                    false
                }
                if (signedIn) onSuccess()
            } finally {
                _isLoggingIn.value = false
            }
        }
    }

    /**
     * Runs the sign-in steps in order, publishing the matching error when one of them fails.
     *
     * @param context used to launch the Google account picker.
     * @param restrictNewAccounts see [signInWithGoogle].
     * @return true when the session is open and the account is ready to use, false when the
     *   attempt ended with an error or the user cancelled the picker.
     */
    private suspend fun signIn(context: Context, restrictNewAccounts: Boolean): Boolean {
        // Read before the login: a new account takes this local data with it.
        val localUser = userRepository.get().first()
        val localTestResults = testResultRepository.get().first()
        val localAnswers = answerRepository.getAll().first()

        val (idToken, email) = authRepository.getGoogleIdTokenAndEmail(context).getOrElse { error ->
            if (error !is CancellationException) _errorMessage.value = GOOGLE_ACCOUNT_ERROR_MESSAGE
            return false
        }

        if (restrictNewAccounts && !authRepository.existsInAuth(email)) {
            authRepository.signOut(context)
            _errorEmail.value = email
            return false
        }

        authRepository.signInWithGoogleCredential(idToken).onFailure { error ->
            if (error !is CancellationException) _errorMessage.value = FIREBASE_AUTH_ERROR_MESSAGE
            return false
        }

        val userId = checkNotNull(authRepository.currentUser()) { "Signed in without a Firebase user" }
        analyticsTracker.identifyUser(userId, email)

        if (userRepository.exists(userId).first()) {
            analyticsTracker.login("google")
            migrationManager.executePendingMigrations(userId)
            return true
        }
        return createAccount(
            context = context,
            userId = userId,
            profile = enrollInFirstSteps(localUser ?: User()),
            testResults = localTestResults,
            answers = localAnswers
        )
    }

    /**
     * Creates the Firestore document of a brand-new account from the data collected locally. When
     * the upload fails the session is closed again and the local data kept, so the user can retry
     * instead of being left signed in to an account with no profile.
     *
     * @param context used to close the session if the upload fails.
     * @param userId the id of the account just signed in.
     * @param profile the profile to upload as the account's document.
     * @param testResults local results to move to the account.
     * @param answers local answers to move to the account.
     * @return true when the account was created and the local copies were cleared.
     */
    private suspend fun createAccount(
        context: Context,
        userId: String,
        profile: User,
        testResults: List<TestResult>,
        answers: List<Answer>
    ): Boolean {
        val uploaded = userRepository.uploadAll(userId, profile, testResults, answers)
        if (uploaded.isFailure) {
            authRepository.signOut(context)
            _errorMessage.value = ACCOUNT_CREATION_ERROR_MESSAGE
            return false
        }
        userRepository.clear()
        testResultRepository.clear()
        answerRepository.clear()
        analyticsTracker.signUp("google")
        return true
    }

    /**
     * Enrolls a brand-new account in the day-0 "Tus primeros pasos" bar. Only accounts created
     * here get it: existing ones never pass through this path, so they keep no enrollment and
     * never see the bar.
     *
     * @param user the profile about to be uploaded as the new account's document.
     * @return [user] with the first-steps bar started now.
     */
    private fun enrollInFirstSteps(user: User): User =
        user.copy(firstSteps = FirstStepsProgress.startingAt(System.currentTimeMillis()))

    private companion object {
        const val GOOGLE_ACCOUNT_ERROR_MESSAGE = "Error al obtener cuenta de Google."
        const val FIREBASE_AUTH_ERROR_MESSAGE = "Error al autenticar con Firebase."
        const val ACCOUNT_CREATION_ERROR_MESSAGE = "No se pudo crear tu cuenta. Comprueba tu conexión e inténtalo de nuevo."
        const val SIGN_IN_FAILED_MESSAGE = "No se pudo iniciar sesión. Inténtalo de nuevo."
    }
}
