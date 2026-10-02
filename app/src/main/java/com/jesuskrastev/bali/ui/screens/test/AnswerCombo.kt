package com.jesuskrastev.bali.ui.screens.test

/**
 * How loudly the quiz top bar shows a run of correct answers in one session. It grows in steps,
 * so ten in a row does not look the same as two.
 *
 * @property minRun the shortest run that reaches this tier
 */
enum class ComboTier(val minRun: Int) {
    /** Fewer than two in a row: no label. */
    HIDDEN(0),

    /** Two to four: the plain "N SEGUIDAS" label. */
    WARM(2),

    /** Five to nine: bigger, in the flame's orange, with the flame. */
    HOT(5),

    /** Ten or more: the biggest, in the flame's gradient. */
    ON_FIRE(10);

    companion object {
        /**
         * The tier of a run.
         *
         * @param run consecutive correct answers in the session
         * @return the highest tier the run reaches; [HIDDEN] for anything under two
         */
        fun of(run: Int): ComboTier = entries.lastOrNull { run >= it.minRun } ?: HIDDEN
    }
}

/**
 * Whether this answer is the one that takes the run into a louder tier ([ComboTier.HOT] or
 * [ComboTier.ON_FIRE]). The label pulses once on that answer, and not on the ones after it.
 *
 * @param run consecutive correct answers in the session, this one included
 * @return true exactly when [run] is the first of the hot or on-fire tier
 */
fun isComboThreshold(run: Int): Boolean = run == ComboTier.HOT.minRun || run == ComboTier.ON_FIRE.minRun
