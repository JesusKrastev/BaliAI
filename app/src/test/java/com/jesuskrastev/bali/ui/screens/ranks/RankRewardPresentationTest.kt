package com.jesuskrastev.bali.ui.screens.ranks

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.model.RankReward
import org.junit.Test

class RankRewardPresentationTest {
    /** Consumable-only prizes name their item instead of advertising zero coins. */
    @Test
    fun consumableNamesItsRealQuantityAndIllustration() {
        val hint = RankReward("h", 40, hints = 1)
        assertThat(hint.title()).isEqualTo("+1 pista")
        assertThat(hint.prizeRes()).isEqualTo(R.drawable.shop_hint)
        assertThat(hint.usageText()).contains("no se usan en simulacros")
        assertThat(RankReward("xp", 250, doubleXpBoosts = 2).title()).isEqualTo("+2 doble XP")
    }

    /** A pack describes all guaranteed items so the chest illustration is never misleading. */
    @Test
    fun guaranteedPackListsAllContents() {
        val reward = RankReward("pack", 600, hints = 2, fiftyFifties = 1)
        assertThat(reward.title()).isEqualTo("Pack de premios")
        assertThat(reward.contents()).isEqualTo("+2 pistas · +1 50/50")
        assertThat(reward.prizeRes()).isEqualTo(R.drawable.shop_surprise_chest)
        assertThat(reward.lines()).hasSize(2)
    }
}
