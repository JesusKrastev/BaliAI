package com.jesuskrastev.bali.ui.screens.ranks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.domain.model.RankProgression
import com.jesuskrastev.bali.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State for the XP prize path and its current claim operation. */
data class RankRewardsUiState(
    val xp: Int = 0,
    val claimedIds: Set<String> = emptySet(),
    val claimingId: String? = null,
    val error: String? = null
)

@HiltViewModel
class RankRewardsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    private val _claimingId = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RankRewardsUiState> = combine(
        userRepository.get(), _claimingId, _error
    ) { user, claimingId, error ->
        RankRewardsUiState(
            xp = user?.xp ?: 0,
            claimedIds = user?.claimedRankRewards.orEmpty().toSet(),
            claimingId = claimingId,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RankRewardsUiState())

    /** Claims [rewardId] once after confirming its XP requirement; returns immediately. */
    fun claim(rewardId: String) {
        val reward = RankProgression.rewardFor(rewardId) ?: return
        if (_claimingId.value != null || uiState.value.xp < reward.requiredXp || rewardId in uiState.value.claimedIds) return
        viewModelScope.launch {
            _claimingId.value = rewardId
            _error.value = null
            try {
                if (!userRepository.claimRankReward(reward)) _error.value = "Este premio ya no está disponible."
            } catch (error: Exception) {
                _error.value = "No se pudo recoger el premio. Inténtalo de nuevo."
            } finally {
                _claimingId.value = null
            }
        }
    }

    /** Clears the visible error message after it has been shown; returns Unit. */
    fun clearError() { _error.value = null }
}
