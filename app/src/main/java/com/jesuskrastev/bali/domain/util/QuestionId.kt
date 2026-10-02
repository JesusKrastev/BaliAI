package com.jesuskrastev.bali.domain.util

import java.security.MessageDigest
import java.text.Normalizer

/**
 * Stable identifier of a question, derived from its text.
 *
 * Neither question source carries an id: the static lesson bank has none and Gemini writes new
 * questions every time. Hashing the text gives both the same kind of id, so an answer can be
 * matched to the same question in a later session (review of mistakes, cached explanations,
 * "how well do I know this one").
 *
 * **Never change [normalize] or the hash.** Ids are stored with every answer; a different
 * recipe would stop matching all the ids already saved.
 */
object QuestionId {

    private const val PREFIX = "q_"
    private const val HEX_LENGTH = 12

    /**
     * Builds the id of a question. Capitals, accents, punctuation and spacing do not matter, so
     * the same question written slightly differently by Gemini still gets the same id.
     *
     * @param text the question as shown to the user
     * @return `q_` followed by 12 hex characters; the same text always gives the same id
     */
    fun of(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(normalize(text).toByteArray(Charsets.UTF_8))
        return PREFIX + digest.joinToString("") { "%02x".format(it) }.take(HEX_LENGTH)
    }

    /**
     * Reduces [text] to its words: lower case, no accents, every run of anything that is not a
     * letter or digit collapsed into one space.
     *
     * @param text the question text
     * @return the normalized text that gets hashed
     */
    private fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
}
