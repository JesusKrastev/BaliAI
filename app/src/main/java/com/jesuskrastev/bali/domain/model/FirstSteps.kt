package com.jesuskrastev.bali.domain.model

/** Coins granted on top of the task rewards when the student completes every first step. */
const val FIRST_STEPS_BONUS_COINS = 30

/**
 * One of the actions the "Tus primeros pasos" card asks a new student to do on day 0–1.
 *
 * @property id stable identifier persisted in the user's `firstStepsDone` Firestore field. Never
 *   rename one: doing so would make every account that already completed it look unfinished.
 * @property coins reward paid once, the first time the task is completed.
 */
enum class FirstStepTask(val id: String, val coins: Int) {
    /** Finish a practice test from the learning path. */
    FIRST_TEST("first_test", 30),

    /** Get an answer from the AI tutor chat. */
    ASK_BALI("ask_bali", 20),

    /** Finish a mini-game session. */
    PLAY_GAME("play_game", 20);

    companion object {
        /**
         * Finds the task stored under [id].
         *
         * @param id identifier read from persistence.
         * @return the matching task, or null for an unknown id (e.g. written by a newer app version).
         */
        fun fromId(id: String): FirstStepTask? = entries.firstOrNull { it.id == id }
    }
}

/**
 * How far a student is through the first-steps card. Lives on the account (not the install), so
 * reinstalling or changing phone neither brings the card back nor lets the coins be collected twice.
 *
 * @property startedAtMillis when the account was enrolled in the card, or 0 when it never was.
 *   Accounts that predate the feature stay at 0 and therefore never see the card.
 * @property completed tasks already done — and already paid.
 * @property dismissed true once the student hid the card, which also stops any pending reward.
 */
data class FirstStepsProgress(
    val startedAtMillis: Long = 0L,
    val completed: Set<FirstStepTask> = emptySet(),
    val dismissed: Boolean = false
) {
    /** True when the account was enrolled in the first-steps card at sign-up. */
    val isEnrolled: Boolean get() = startedAtMillis > 0L

    /** True while tasks can still be completed and paid: enrolled and not dismissed. */
    val isActive: Boolean get() = isEnrolled && !dismissed

    /** True once every task in [FirstStepTask] is done. */
    val isComplete: Boolean get() = completed.containsAll(FirstStepTask.entries)

    /** Number of tasks already done. */
    val doneCount: Int get() = completed.size

    /** Coins still up for grabs: the unfinished tasks plus the completion bonus if it is unpaid. */
    val pendingCoins: Int
        get() = (FirstStepTask.entries - completed).sumOf { it.coins } +
            if (isComplete) 0 else FIRST_STEPS_BONUS_COINS

    /**
     * @param task the task to look up.
     * @return true if [task] was already completed and paid.
     */
    fun isDone(task: FirstStepTask): Boolean = task in completed

    companion object {
        /**
         * Builds the progress of a freshly enrolled account.
         *
         * @param millis enrollment timestamp; must be positive or the account would count as not enrolled.
         */
        fun startingAt(millis: Long): FirstStepsProgress = FirstStepsProgress(startedAtMillis = millis)
    }
}

/**
 * Coins just earned by completing a first step, waiting to be celebrated on Home.
 *
 * @property task the task that was completed.
 * @property coins everything paid for it, completion bonus included.
 * @property completedAll true when this was the last task, so [coins] includes [FIRST_STEPS_BONUS_COINS].
 */
data class FirstStepReward(
    val task: FirstStepTask,
    val coins: Int,
    val completedAll: Boolean
)
