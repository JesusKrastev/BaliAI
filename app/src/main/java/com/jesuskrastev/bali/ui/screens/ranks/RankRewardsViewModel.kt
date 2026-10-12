package com.jesuskrastev.bali.ui.screens.ranks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jesuskrastev.bali.domain.model.RankProgression
import com.jesuskrastev.bali.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State for the XP prize path and its current claim operation.
 *
 * @property coins the user's coin balance
 * @property isLoaded false until the user's XP has been read, so the road isn't scrolled to 0 XP
 * @property message one-off error feedback, cleared once shown
 * @property celebration the prize just collected, shown full screen until the user taps it away
 */
data class RankRewardsUiState(
    val xp: Int = 0,
    val coins: Int = 0,
    val claimedIds: Set<String> = emptySet(),
    val claimingId: String? = null,
    val message: String? = null,
    val celebration: PrizeCelebration? = null,
    val isLoaded: Boolean = false
)

@HiltViewModel
class RankRewardsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    private val _claimingId = MutableStateFlow<String?>(null)
    private val _message = MutableStateFlow<String?>(null)
    private val _celebration = MutableStateFlow<PrizeCelebration?>(null)

    val uiState: StateFlow<RankRewardsUiState> = combine(
        userRepository.get(), _claimingId, _message, _celebration
    ) { user, claimingId, message, celebration ->
        RankRewardsUiState(
            xp = user?.xp ?: 0,
            coins = user?.coins ?: 0,
            claimedIds = user?.claimedRankRewards.orEmpty().toSet(),
            claimingId = claimingId,
            message = message,
            celebration = celebration,
            isLoaded = true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RankRewardsUiState())

    /** Claims [rewardId] once after confirming its XP requirement; returns immediately. */
    fun claim(rewardId: String) {
        val reward = RankProgression.rewardFor(rewardId) ?: return
        val before = uiState.value
        if (_claimingId.value != null || before.xp < reward.requiredXp || rewardId in before.claimedIds) return
        _claimingId.value = rewardId
        _message.value = null
        viewModelScope.launch {
            try {
                if (userRepository.claimRankReward(reward)) {
                    _celebration.value = prizeCelebrationOf(
                        reward = reward,
                        xp = before.xp,
                        claimedIds = before.claimedIds + reward.id,
                        coinsAfter = before.coins + reward.coins
                    )
                } else {
                    _message.value = "Este premio ya no está disponible."
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _message.value = "No se pudo recoger el premio. Inténtalo de nuevo."
            } finally {
                _claimingId.value = null
            }
        }
    }

    /** Clears the visible message after it has been shown; returns Unit. */
    fun messageShown() { _message.value = null }

    /** Closes the prize celebration; returns Unit. */
    fun celebrationShown() { _celebration.value = null }
}
