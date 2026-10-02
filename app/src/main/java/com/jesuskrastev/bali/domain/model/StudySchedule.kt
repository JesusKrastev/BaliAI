package com.jesuskrastev.bali.domain.model

/**
 * The part of the day the user said suits them to study, asked in onboarding so the study
 * reminder arrives when they can act on it instead of at an arbitrary hour.
 *
 * @property tag stable value sent to OneSignal as the `study_slot` tag; never rename it, the
 *   segments in the OneSignal dashboard filter on it
 * @property hour local hour (24 h clock) the reminder is sent at
 */
enum class StudySlot(val tag: String, val hour: Int) {
    MORNING("morning", 9),
    NOON("noon", 14),
    AFTERNOON("afternoon", 18),
    NIGHT("night", 21);

    companion object {
        /**
         * Reads a slot back from its [tag].
         *
         * @param tag a value previously stored from [StudySlot.tag], or null
         * @return the matching slot, or null when [tag] is missing or unknown
         */
        fun fromTag(tag: String?): StudySlot? = entries.firstOrNull { it.tag == tag }
    }
}

/**
 * How often the user committed to study in onboarding, which decides on which days a
 * reminder is due.
 *
 * @property tag stable value sent to OneSignal as the `study_rhythm` tag; never rename it
 */
enum class StudyRhythm(val tag: String) {
    DAILY("daily"),
    OFTEN("often"),
    WHENEVER("whenever");

    companion object {
        /**
         * Reads a rhythm back from its [tag].
         *
         * @param tag a value previously stored from [StudyRhythm.tag], or null
         * @return the matching rhythm, or null when [tag] is missing or unknown
         */
        fun fromTag(tag: String?): StudyRhythm? = entries.firstOrNull { it.tag == tag }
    }
}

/**
 * When the user wants to be reminded to study.
 *
 * @property slot the part of the day that suits them
 * @property rhythm how often they said they would study, or null if they skipped it
 */
data class StudySchedule(val slot: StudySlot, val rhythm: StudyRhythm?)
