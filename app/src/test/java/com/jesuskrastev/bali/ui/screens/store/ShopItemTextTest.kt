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

    /** Verifies the freezer copy states the automatic protection, limit and price concisely. */
    @Test
    fun `the freezer explanation states its limit and price`() {
        val info = shopItemInfo(ShopItem.StreakFreezer)

        assertThat(info).contains("Protege automáticamente")
        assertThat(info).contains("hasta ${DailyStreak.MAX_FREEZES}")
        assertThat(info).contains("Cuesta 120 monedas")
    }

    /** Verifies the recovery copy makes its short availability window obvious. */
    @Test
    fun `the recovery explanation states when it works and its price`() {
        val info = shopItemInfo(ShopItem.StreakRecovery)

        assertThat(info).contains("perdiste ayer")
        assertThat(info).contains("hasta terminar hoy")
        assertThat(info).contains("Cuesta ${DailyStreak.RECOVERY_COST_COINS} monedas")
    }

    /** Verifies the bet copy preserves its stake, duration, payout and frozen-day rule. */
    @Test
    fun `the streak bet explanation states the stake, the days, the payout and the risk`() {
        val info = shopItemInfo(ShopItem.StreakBet)

        assertThat(info).contains("Apuesta ${StreakBet.COST_COINS} monedas")
        assertThat(info).contains("${StreakBet.DAYS} días más")
        assertThat(info).contains("gana ${StreakBet.PAYOUT_COINS}")
        assertThat(info).contains("pierdes la apuesta")
        assertThat(info).contains("días congelados no avanzan")
    }

    /** Verifies practice-aid copy says what happens and that one unit is consumed. */
    @Test
    fun `practice aids explain their immediate effect and consumption`() {
        assertThat(shopItemInfo(ShopItem.Hint)).contains("Muestra la explicación")
        assertThat(shopItemInfo(ShopItem.Hint)).contains("Usa una pista")
        assertThat(shopItemInfo(ShopItem.Hint)).contains("solo en práctica")
        assertThat(shopItemInfo(ShopItem.FiftyFifty)).contains("Descarta dos respuestas")
        assertThat(shopItemInfo(ShopItem.FiftyFifty)).contains("Usa un 50/50")
    }

    /** Verifies the chest explains every reward family and the rarest probability. */
    @Test
    fun `the surprise chest explains coins items and their probabilities`() {
        val info = shopItemInfo(ShopItem.SurpriseChest)

        assertThat(info).contains("monedas (65 %)")
        assertThat(info).contains("pista (14 %)")
        assertThat(info).contains("doble moneda (5 %)")
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
