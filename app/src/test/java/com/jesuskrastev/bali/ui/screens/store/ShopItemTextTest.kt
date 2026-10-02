package com.jesuskrastev.bali.ui.screens.store

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.DailyStreak
import com.jesuskrastev.bali.domain.model.StreakBet
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

    @Test
    fun `the streak bet explanation states the stake, the days, the payout and the risk`() {
        val info = shopItemInfo(ShopItem.StreakBet)

        assertThat(info).contains("Pagas ${StreakBet.COST_COINS} monedas")
        assertThat(info).contains("${StreakBet.DAYS} días más")
        assertThat(info).contains("recibes ${StreakBet.PAYOUT_COINS}")
        assertThat(info).contains("pierdes las ${StreakBet.COST_COINS}")
    }

    @Test
    fun `practice aids say they do not work in the mock exam`() {
        assertThat(shopItemInfo(ShopItem.Hint)).contains("no se puede usar en el simulacro")
        assertThat(shopItemInfo(ShopItem.FiftyFifty)).contains("no se puede usar en el simulacro")
    }

    @Test
    fun `the surprise chest admits that it can lose coins`() {
        val info = shopItemInfo(ShopItem.SurpriseChest)

        assertThat(info).contains("entre ${ShopCatalog.CHEST_MIN_REWARD} y ${ShopCatalog.CHEST_MAX_REWARD}")
        assertThat(info).contains("ganando o perdiendo")
    }

    @Test
    fun `every product has a name, a picture and the price it is charged`() {
        val items = listOf(
            ShopItem.StreakFreezer, ShopItem.StreakRecovery, ShopItem.StreakBet, ShopItem.Hint,
            ShopItem.FiftyFifty, ShopItem.SurpriseChest, ShopItem.DoubleXp, ShopItem.DoubleCoins
        )

        items.forEach { item ->
            assertThat(shopItemTitle(item)).isNotEmpty()
            assertThat(shopItemShortTitle(item)).isNotEmpty()
            assertThat(shopItemImage(item)).isNotEqualTo(0)
            assertThat(shopItemInfo(item)).contains("Cuesta ${shopItemPrice(item)} monedas")
        }
        assertThat(items.map(::shopItemTitle).toSet()).hasSize(items.size)
    }
}
