package com.jesuskrastev.bali.domain.model

/**
 * The streak bet sold in the coin shop: the user pays [COST_COINS] and, if the streak survives
 * [DAYS] more study days, gets [PAYOUT_COINS] back; if the streak is lost first, the stake is gone.
 *
 * The bet is stored as a single number, [User.streakBetTarget]: the streak length that wins it
 * (the streak at purchase plus [DAYS]). 0 means there is no bet. Only a running streak can be
 * bet on (at least 1 day), so the starting point of the bet is never 0 and a lost streak is
 * always detectable. A day covered by a streak freeze keeps the streak but is not a study day,
 * so it does not move the bet forward.
 */
object StreakBet {
    /** Coins paid to place the bet. */
    const val COST_COINS = 50

    /** Coins received when the bet is won. */
    const val PAYOUT_COINS = 100

    /** Study days the streak has to grow by to win the bet. */
    const val DAYS = 7

    /**
     * Tells whether a bet can be placed with the streak the user has today.
     *
     * @param settledStreak the streak as it stands today ([DailyStreak.settledAt])
     * @return true when there is a running streak to bet on
     */
    fun canBetOn(settledStreak: Int): Boolean = settledStreak >= 1

    /**
     * The streak length that wins a bet placed today.
     *
     * @param settledStreak the streak as it stands today
     * @return the value to store in [User.streakBetTarget]
     */
    fun targetFor(settledStreak: Int): Int = settledStreak + DAYS

    /**
     * Tells whether the streak was lost after the bet was placed. A streak below the one at
     * purchase can only mean it went back to 0 and started again.
     *
     * @param target the stored bet target, 0 for no bet
     * @param settledStreak the streak as it stands today, before any study today is counted
     * @return true for an active bet whose streak was lost
     */
    fun isLost(target: Int, settledStreak: Int): Boolean =
        target > 0 && settledStreak < target - DAYS

    /**
     * Tells whether the streak has reached the bet's target.
     *
     * @param target the stored bet target, 0 for no bet
     * @param streak the streak after counting today's study
     * @return true for an active bet that is won
     */
    fun isWon(target: Int, streak: Int): Boolean = target > 0 && streak >= target

    /**
     * Study days already completed since the bet was placed.
     *
     * @param target the stored bet target
     * @param streak the streak as it stands now
     * @return a number from 0 to [DAYS]
     */
    fun daysDone(target: Int, streak: Int): Int = (streak - (target - DAYS)).coerceIn(0, DAYS)
}
