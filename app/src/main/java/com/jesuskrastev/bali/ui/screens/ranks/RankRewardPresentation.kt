package com.jesuskrastev.bali.ui.screens.ranks

import androidx.annotation.DrawableRes
import com.jesuskrastev.bali.R
import com.jesuskrastev.bali.domain.model.RankReward

/** One guaranteed prize line, with the same illustration used in the shop. */
internal data class RewardLine(@DrawableRes val image: Int, val label: String)

/** Returns every item granted by [this] reward, with quantities and readable Spanish labels. */
internal fun RankReward.lines(): List<RewardLine> = buildList {
    if (coins > 0) add(RewardLine(R.drawable.coin, "+$coins monedas"))
    if (hints > 0) add(RewardLine(R.drawable.shop_hint, "+$hints ${if (hints == 1) "pista" else "pistas"}"))
    if (fiftyFifties > 0) add(RewardLine(R.drawable.shop_fifty_fifty, "+$fiftyFifties 50/50"))
    if (doubleXpBoosts > 0) add(RewardLine(R.drawable.shop_double_xp, "+$doubleXpBoosts doble XP"))
    if (doubleCoinBoosts > 0) add(RewardLine(R.drawable.shop_double_coins, "+$doubleCoinBoosts doble moneda"))
}

/** Returns a concise title for [this] prize; packs show their guaranteed contents separately. */
internal fun RankReward.title(): String = if (itemKinds > 1) "Pack de premios" else lines().single().label

/** Returns all quantities for [this] reward, suitable for a pack description. */
internal fun RankReward.contents(): String = lines().joinToString(" · ") { it.label }

/** Returns the relevant shop illustration for [this] reward, or the chest for a guaranteed pack. */
@DrawableRes
internal fun RankReward.prizeRes(): Int =
    if (itemKinds > 1) R.drawable.shop_surprise_chest else lines().single().image

/** Returns where [this] reward is usable so a claimed boost is not mistaken for an active one. */
internal fun RankReward.usageText(): String = when {
    doubleXpBoosts > 0 || doubleCoinBoosts > 0 ->
        "Guardado en tu inventario. Los multiplicadores se aplican en las próximas sesiones."
    hints > 0 || fiftyFifties > 0 ->
        "Guardado en tu inventario para practicar. Las ayudas no se usan en simulacros."
    else -> "Monedas añadidas a tu saldo para la tienda."
}
