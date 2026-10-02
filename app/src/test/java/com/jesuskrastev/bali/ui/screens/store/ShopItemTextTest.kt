package com.jesuskrastev.bali.ui.screens.store

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.DailyStreak
import org.junit.Test

class ShopItemTextTest {

    @Test
    fun `every product has its own name`() {
        assertThat(shopItemTitle(ShopItem.StreakFreezer)).isEqualTo("Congelador de racha")
        assertThat(shopItemTitle(ShopItem.StreakRecovery)).isEqualTo("Recuperador de racha")
    }

    @Test
    fun `the freezer explanation states its limit and price`() {
        val info = shopItemInfo(ShopItem.StreakFreezer)

        assertThat(info).contains("hasta ${DailyStreak.MAX_FREEZES} a la vez")
        assertThat(info).contains("Cuesta 120 monedas")
    }

    @Test
    fun `the recovery explanation states when it works and its price`() {
        val info = shopItemInfo(ShopItem.StreakRecovery)

        assertThat(info).contains("hasta el final del día siguiente")
        assertThat(info).contains("Cuesta ${DailyStreak.RECOVERY_COST_COINS} monedas")
    }
}
