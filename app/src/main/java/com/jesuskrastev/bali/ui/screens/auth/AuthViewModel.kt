package com.jesuskrastev.bali.ui.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.data.analytics.FirebaseAnalyticsTracker
import com.jesuskrastev.bali.data.repository.AnswerRepositoryImpl
import com.jesuskrastev.bali.data.repository.TestResultRepositoryImpl
import com.jesuskrastev.bali.data.repository.UserRepositoryImpl
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.TestResult
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.usecase.AppInitializationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepositoryImpl,
    private val testResultRepository: TestResultRepositoryImpl,
    private val answerRepository: AnswerRepositoryImpl,
    private val authRepository: AuthRepository,
    private val analyticsTracker: FirebaseAnalyticsTracker,
    private val appInitializationUseCase: AppInitializationUseCase
) : ViewModel() {

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _errorEmail = MutableStateFlow<String?>(null)
    val errorEmail: StateFlow<String?> = _errorEmail.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
        _errorEmail.value = null
    }

    fun signInWithGoogle(context: Context, restrictNewAccounts: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoggingIn.value = true
            
            // Leemos los datos locales de Room antes del login
            val localUser = userRepository.get().first()
            val localTestResults = testResultRepository.getAll().first()
            val localAnswers = answerRepository.getAll().first()
            
            val tokenResult = authRepository.getGoogleIdTokenAndEmail(context)

            if (tokenResult.isSuccess) {
                val (idToken, email) = tokenResult.getOrNull()!!
                
                if (restrictNewAccounts) {
                    val existsInAuth = authRepository.existsInAuth(email)
                    if (!existsInAuth) {
                        authRepository.signOut(context)
                        _isLoggingIn.value = false
                        _errorEmail.value = email
                        return@launch
                    }
                }
                
                val result = authRepository.signInWithGoogleCredential(idToken)

                if (result.isSuccess) {
                    val userId = authRepository.currentUser()
                    val userExists = userRepository.exists(userId).first()
                    
                    if (!userExists) {
                        synchronizeLocalDataToFirestore(
                            userId ?: "",
                            localUser ?: User(),
                            localTestResults,
                            localAnswers
                        )
                        analyticsTracker.signUp()
                    } else {
                        analyticsTracker.login()
                        // One-shot inicialización (Rachas, energía, migraciones) post-login
                        userId?.let {
                            appInitializationUseCase(it)
                        }
                    }
                    // Navigate back
                    onSuccess()
                } else {
                    val exception = result.exceptionOrNull()
                    if (exception !is kotlinx.coroutines.CancellationException) {
                        _errorMessage.value = "Error al autenticar con Firebase."
                    }
                }
            } else {
                val exception = tokenResult.exceptionOrNull()
                if (exception !is kotlinx.coroutines.CancellationException) {
                    _errorMessage.value = "Error al obtener cuenta de Google."
                }
            }
            
            _isLoggingIn.value = false
        }
    }

    private suspend fun synchronizeLocalDataToFirestore(
        userId: String,
        user: User,
        testResults: List<TestResult>,
        answers: List<Answer>,
    ) {
        val syncResult = userRepository.uploadAll(userId, user, testResults, answers)
        
        if (syncResult.isSuccess) {
            userRepository.clear()
            testResultRepository.clear()
            answerRepository.clear()
        }
    }
}
