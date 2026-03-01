package com.jesuskrastev.bali.domain.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    suspend fun getGoogleIdTokenAndEmail(context: Context): Result<Pair<String, String>>
    suspend fun signInWithGoogleCredential(idToken: String): Result<Unit>
    suspend fun existsInAuth(email: String): Boolean
    suspend fun signOut(context: Context)
    suspend fun currentUser(): String?
    suspend fun currentUserPhotoUrl(): String?
    suspend fun currentUserEmail(): String?
}
