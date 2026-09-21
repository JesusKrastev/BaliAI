package com.jesuskrastev.bali.domain.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing user authentication state.
 *
 * Interfaces with Firebase Auth and the Credential Manager (Google Sign-In) to
 * track the user's active session and basic profile data.
 */
interface AuthRepository {
    /** Emits true when the user is logged in, false otherwise. */
    val isLoggedIn: Flow<Boolean>
    
    /** Emits the logged-in user's UID, or null if not authenticated. */
    val currentUserFlow: Flow<String?>

    /** Initiates the Google Sign-In flow and retrieves the ID token and email if successful. */
    suspend fun getGoogleIdTokenAndEmail(context: Context): Result<Pair<String, String>>
    
    /** Authenticates the Firebase user using the provided Google ID token. */
    suspend fun signInWithGoogleCredential(idToken: String): Result<Unit>
    
    /** Checks if an account already exists for the given email address. */
    suspend fun existsInAuth(email: String): Boolean
    
    /** Signs the user out of Firebase and clears local credential states. */
    suspend fun signOut(context: Context)
    
    /** Imperatively returns the current user's UID or null. */
    suspend fun currentUser(): String?

    /**
     * Emits the current user's profile photo URL, re-emitting on every sign-in/sign-out
     * so reactive screens (e.g. a `combine()`-built UI state) stay in sync. Null when
     * signed out or when the account has no photo.
     */
    val currentUserPhotoUrlFlow: Flow<String?>

    /**
     * Emits the current user's email address, re-emitting on every sign-in/sign-out so
     * reactive screens (e.g. a `combine()`-built UI state) stay in sync. Null when
     * signed out.
     */
    val currentUserEmailFlow: Flow<String?>
}
