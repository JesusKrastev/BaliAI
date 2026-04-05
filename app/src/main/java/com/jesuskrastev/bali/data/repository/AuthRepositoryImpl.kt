package com.jesuskrastev.bali.data.repository

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.jesuskrastev.bali.R
import com.onesignal.OneSignal
import com.jesuskrastev.bali.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    override val isLoggedIn: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser != null) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun currentUser(): String? {
        return auth.currentUser?.uid
    }

    override val currentUserFlow: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun currentUserPhotoUrl(): String? {
        return auth.currentUser?.photoUrl?.toString()
    }

    override suspend fun currentUserEmail(): String? {
        return auth.currentUser?.email
    }

    override suspend fun getGoogleIdTokenAndEmail(context: Context): Result<Pair<String, String>> {
        val activity = context.findActivity()
            ?: return Result.failure(Exception("Activity no encontrada"))

        val credentialManager = CredentialManager.create(activity)

        // Primero hacemos sign out silencioso para que siempre aparezca el selector
        auth.signOut()
        credentialManager.clearCredentialState(ClearCredentialStateRequest())

        return try {
            // Con filterByAuthorizedAccounts=false siempre muestra todas las cuentas
            val result = getCredential(
                activity = activity,
                credentialManager = credentialManager,
                context = context,
                filterByAuthorized = false
            ) ?: return Result.failure(Exception("No se encontró ninguna cuenta de Google en el dispositivo"))

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Result.success(Pair(googleCredential.idToken, googleCredential.id))
            } else {
                Result.failure(Exception("Tipo de credencial inesperado: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            // El usuario canceló → no es un error, no hacemos nada
            Result.failure(CancellationException("Usuario canceló el inicio de sesión"))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error obteniendo credencial de Google", e)
            Result.failure(e)
        }
    }

    private suspend fun getCredential(
        activity: Activity,
        credentialManager: CredentialManager,
        context: Context,
        filterByAuthorized: Boolean
    ): GetCredentialResponse? {
        return try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(filterByAuthorized)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false) // false = siempre muestra el selector, nunca auto-selecciona
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()

            credentialManager.getCredential(context = activity, request = request)

        } catch (e: GetCredentialCancellationException) {
            throw e // Se captura arriba en signInWithGoogle
        } catch (e: Exception) {
            Log.w("AuthRepository", "getCredential falló: ${e::class.simpleName} - ${e.message}")
            null
        }
    }

    override suspend fun signInWithGoogleCredential(idToken: String): Result<Unit> {
        return try {
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(firebaseCredential).await()
            auth.currentUser?.uid?.let { OneSignal.login(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error autenticando con Firebase", e)
            Result.failure(e)
        }
    }

    override suspend fun existsInAuth(email: String): Boolean {
        return try {
            val result = auth.fetchSignInMethodsForEmail(email).await()
            result.signInMethods?.isNotEmpty() == true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun signOut(context: Context) {
        val activity = context.findActivity() ?: return
        auth.signOut()
        CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest())
    }

    private fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}