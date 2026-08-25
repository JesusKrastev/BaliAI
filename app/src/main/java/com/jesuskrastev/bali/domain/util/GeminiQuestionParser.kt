package com.jesuskrastev.bali.domain.util

import com.jesuskrastev.bali.ui.screens.test.QuestionUiState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random

/**
 * Parses Gemini's response into a list of [QuestionUiState].
 *
 * The model is asked for schema-constrained JSON, but the text is still scanned for the
 * outermost JSON object so an older or degraded reply with preamble around it still
 * parses rather than costing a generation and giving nothing back.
 *
 * It also shuffles each question's options. Asking the model for a random answer position
 * does not work — measured over a 30-question exam, the correct answer landed on option B
 * 17 times on both `gemini-2.5-flash` and `gemini-3.5-flash-lite`, enough that a student
 * could pass a mock exam by always picking B. Shuffling client-side is exact, free, and
 * lets the prompts drop the randomness rules they were never able to honour.
 */
object GeminiQuestionParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
    }

    /**
     * Reads the questions out of a model response, shuffling the options of each one.
     *
     * @param rawText the model's reply; JSON, optionally wrapped in other text.
     * @param random source of shuffling, overridable so tests can pin the order.
     * @return the parsed questions, skipping any entry that is malformed.
     * @throws IllegalArgumentException when the text holds no JSON object at all.
     */
    fun parse(rawText: String, random: Random = Random.Default): List<QuestionUiState> {
        val start = rawText.indexOf('{')
        val end = rawText.lastIndexOf('}')
        require(start != -1 && end != -1) { "No valid JSON block found in Gemini response" }

        val jsonString = rawText.substring(start, end + 1)
        val root = json.parseToJsonElement(jsonString).jsonObject

        return root["questions"]?.jsonArray?.mapNotNull { element ->
            runCatching {
                val obj = element.jsonObject
                val options = obj["options"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                val correctIndex = obj["correctAnswerIndex"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val shuffled = options.shuffledKeepingAnswer(correctIndex, random)

                QuestionUiState(
                    text = obj["text"]?.jsonPrimitive?.content.orEmpty(),
                    options = shuffled.options,
                    correctAnswerIndex = shuffled.correctAnswerIndex,
                    explanation = obj["explanation"]?.jsonPrimitive?.content.orEmpty(),
                    imageUrl = obj["imageUrl"]?.jsonPrimitive?.content
                        ?.takeIf { it != "null" && it.startsWith("http") }
                )
            }.getOrNull()
        } ?: emptyList()
    }

    /** A question's options together with the position its correct answer ended up in. */
    private data class ShuffledOptions(val options: List<String>, val correctAnswerIndex: Int)

    /**
     * Reorders the options at random and reports where the correct one moved to.
     *
     * The permutation is built over indexes rather than over the texts themselves, so two
     * options that happen to read the same cannot make the answer point at the wrong one.
     *
     * @param correctAnswerIndex the correct option's position before shuffling.
     * @param random source of the permutation.
     * @return the reordered options and the new index, or the list untouched when
     *   [correctAnswerIndex] falls outside it.
     */
    private fun List<String>.shuffledKeepingAnswer(
        correctAnswerIndex: Int,
        random: Random
    ): ShuffledOptions {
        if (correctAnswerIndex !in indices) return ShuffledOptions(this, correctAnswerIndex)

        val order = indices.shuffled(random)
        return ShuffledOptions(
            options = order.map { this[it] },
            correctAnswerIndex = order.indexOf(correctAnswerIndex)
        )
    }
}
