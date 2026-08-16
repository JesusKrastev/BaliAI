package com.jesuskrastev.bali.data.repository

import com.jesuskrastev.bali.data.local.room.dao.ChatMessageDao
import com.jesuskrastev.bali.data.mapper.toDomain
import com.jesuskrastev.bali.data.mapper.toEntity
import com.jesuskrastev.bali.data.mapper.toFirestore
import com.jesuskrastev.bali.data.remote.firestore.dao.FirestoreChatDao
import com.jesuskrastev.bali.domain.model.ChatMessage
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the AI tutor conversation in Firestore for signed-in users and in Room for
 * everyone else, following the repository auth-routing pattern used across the app.
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val firestoreChatDao: FirestoreChatDao,
    private val authRepository: AuthRepository
) : ChatRepository {

    /**
     * Runs [actionRemote] when a user is authenticated and [actionLocal] otherwise.
     * Both run on [Dispatchers.IO].
     */
    private suspend inline fun <T> withAuthRouting(
        crossinline actionRemote: suspend (String) -> T,
        crossinline actionLocal: suspend () -> T
    ): T = withContext(Dispatchers.IO) {
        val userId = authRepository.currentUser()
        if (userId != null) actionRemote(userId) else actionLocal()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeHistory(): Flow<List<ChatMessage>> =
        authRepository.currentUserFlow.flatMapLatest { userId ->
            if (userId != null) {
                firestoreChatDao.getMessages(userId).map { messages -> messages.map { it.toDomain() } }
            } else {
                chatMessageDao.getAll().map { messages -> messages.map { it.toDomain() } }
            }
        }

    override suspend fun save(message: ChatMessage) = withAuthRouting(
        actionRemote = { userId -> firestoreChatDao.insert(userId, message.toFirestore()) },
        actionLocal = { chatMessageDao.insert(message.toEntity()) }
    )

    override suspend fun clear() = withAuthRouting(
        actionRemote = { userId ->
            firestoreChatDao.clear(userId)
            // The local table is cleared too: it is what this user sees if they sign out.
            chatMessageDao.clear()
        },
        actionLocal = { chatMessageDao.clear() }
    )
}
