package com.jesuskrastev.bali.data.repository

import com.jesuskrastev.bali.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The repositories' auth-routing rule: Firestore when a user is signed in, Room otherwise.
 *
 * @param actionRemote runs with the signed-in user's id when there is a session
 * @param actionLocal runs when nobody is signed in
 * @return what the chosen action returned; both run on [Dispatchers.IO]
 */
internal suspend inline fun <T> AuthRepository.withAuthRouting(
    crossinline actionRemote: suspend (userId: String) -> T,
    crossinline actionLocal: suspend () -> T
): T = withContext(Dispatchers.IO) {
    val userId = currentUser()
    if (userId != null) actionRemote(userId) else actionLocal()
}
