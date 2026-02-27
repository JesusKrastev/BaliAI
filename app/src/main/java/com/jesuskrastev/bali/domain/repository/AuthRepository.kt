package com.jesuskrastev.bali.domain.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    suspend fun signInWithGoogle(context: Context): Result<Unit>
    suspend fun signOut(context: Context)
    suspend fun currentUser(): String?
    suspend fun currentUserPhotoUrl(): String?
    suspend fun currentUserEmail(): String?
}
