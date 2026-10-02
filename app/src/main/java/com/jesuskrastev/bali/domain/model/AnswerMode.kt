package com.jesuskrastev.bali.domain.model

/**
 * Where an answer was given, so statistics can tell a lesson from a mock exam.
 *
 * The first three mirror [NodeType], the kind of learning-path node the session came from.
 *
 * @property tag text stored with the answer; never rename it, saved answers keep the old one
 */
enum class AnswerMode(val tag: String) {
    LESSON("LESSON"),
    REVIEW("REVIEW"),
    EXAM("EXAM"),

    /** A session that did not start from a learning-path node. */
    PRACTICE("PRACTICE"),

    /** The 30-minute official exam, with its own screen. */
    OFFICIAL_EXAM("OFFICIAL_EXAM");

    companion object {
        /**
         * Reads a stored tag back.
         *
         * @param tag the text saved with the answer; blank for answers from before modes existed
         * @return the mode, or null when the tag is blank or unknown (an old or newer answer)
         */
        fun fromTag(tag: String?): AnswerMode? = entries.firstOrNull { it.tag == tag }

        /**
         * Picks the mode of a session from the learning-path node it came from.
         *
         * @param nodeType the [NodeType] name of the node, or null when there was none
         * @return the matching mode; [PRACTICE] for no node or a type this app version does not know
         */
        fun fromNodeType(nodeType: String?): AnswerMode =
            fromTag(nodeType)?.takeIf { it != OFFICIAL_EXAM && it != PRACTICE } ?: PRACTICE
    }
}
