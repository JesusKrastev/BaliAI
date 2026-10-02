package com.jesuskrastev.bali.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.domain.repository.AuthRepository
import com.jesuskrastev.bali.domain.repository.PathRepository
import com.jesuskrastev.bali.domain.repository.PathSeenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What the path animation needs to know.
 *
 * @property isReady false until the path has been compared with what Home showed last time;
 *   Home holds the path back until then, so a node about to animate is never drawn open first
 * @property unlock the stretch to animate, null when there is none
 */
data class PathUnlockUiState(
    val isReady: Boolean = false,
    val unlock: PathUnlock? = null
)

/**
 * Detects nodes that were unlocked since Home last showed the path. It watches the path on its
 * own, so it works whether Home was destroyed while the user did a lesson or kept in the back
 * stack. What Home has shown is only remembered once the animation has played ([onUnlockPlayed]),
 * so leaving before it plays, or a Home that is rebuilt after the lesson, still plays it.
 */
@HiltViewModel
class PathUnlockViewModel @Inject constructor(
    authRepository: AuthRepository,
    pathRepository: PathRepository,
    private val seenRepository: PathSeenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PathUnlockUiState())
    val uiState: StateFlow<PathUnlockUiState> = _uiState.asStateFlow()

    private var userId: String? = null

    init {
        viewModelScope.launch {
            authRepository.currentUserFlow
                .filterNotNull()
                .flatMapLatest { id -> pathRepository.getPathNodes(id).map { nodes -> id to nodes } }
                .collect { (id, nodes) ->
                    userId = id
                    pathFrontierOrder(nodes)?.let { evaluate(id, it) }
                }
        }
    }

    /**
     * Compares the path with what was shown last time. Stays silent, and just remembers the
     * position, the first time (new install or account) and whenever the path did not advance.
     *
     * @param id the signed-in user
     * @param frontier the furthest unlocked node now
     */
    private suspend fun evaluate(id: String, frontier: Int) {
        if (_uiState.value.unlock != null) return
        val lastSeen = runCatching { seenRepository.lastSeenFrontier(id) }.getOrNull()
        val unlock = detectPathUnlock(frontier, lastSeen)
        if (unlock == null && lastSeen != frontier) {
            runCatching { seenRepository.markSeen(id, frontier) }
        }
        _uiState.value = PathUnlockUiState(isReady = true, unlock = unlock)
    }

    /** Called when the animation has finished: remembers the new position so it plays only once. */
    fun onUnlockPlayed() {
        val played = _uiState.value.unlock ?: return
        val id = userId ?: return
        _uiState.value = PathUnlockUiState(isReady = true, unlock = null)
        viewModelScope.launch { runCatching { seenRepository.markSeen(id, played.toOrder) } }
    }
}
