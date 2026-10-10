package com.jesuskrastev.bali.domain.util

import com.jesuskrastev.bali.ui.screens.test.QuestionUiState
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random

/**
 * Parses Gemini's response into a list of [QuestionUiState]; see [GeminiJson] for how the JSON is
 * found in the reply.
 *
 * It also shuffles each question's options. Asking the model for a random answer position
 * does not work — measured over a 30-question exam, the correct answer landed on option B
 * 17 times on both `gemini-2.5-flash` and `gemini-3.5-flash-lite`, enough that a student
 * could pass a mock exam by always picking B. Shuffling client-side is exact, free, and
 * lets the prompts drop the randomness rules they were never able to honour.
 */
object GeminiQuestionParser {

    /**
     * Reads the category the model says it picked for the session.
     *
     * @param rawText the model's reply; JSON, optionally wrapped in other text.
     * @return the `selectedCategory` the model reported, or null when it left it out.
     * @throws IllegalArgumentException when the text holds no JSON object at all.
     */
    fun selectedCategory(rawText: String): String? =
        GeminiJson.parseObject(rawText)["selectedCategory"]?.jsonPrimitive?.content

    /**
     * Reads the questions out of a model response, shuffling the options of each one.
     *
     * @param rawText the model's reply; JSON, optionally wrapped in other text.
     * @param random source of shuffling, overridable so tests can pin the order.
     * @return the parsed questions, skipping any entry that is malformed.
     * @throws IllegalArgumentException when the text holds no JSON object at all.
     */
    fun parse(rawText: String, random: Random = Random.Default): List<QuestionUiState> =
        parseWithSource(rawText, random).map { it.question }

    /** A parsed question with the number of the study-material question it was written from, if the model said. */
    data class SourcedQuestion(val sourceIndex: Int?, val question: QuestionUiState)

    /**
     * Like [parse], but keeps the `sourceIndex` the model attached to each question, which a review
     * uses to hand the picture of the original question back to its rewrite.
     *
     * @param rawText the model's reply; JSON, optionally wrapped in other text.
     * @param random source of shuffling, overridable so tests can pin the order.
     * @return the parsed questions with their source numbers, skipping any malformed entry.
     * @throws IllegalArgumentException when the text holds no JSON object at all.
     */
    fun parseWithSource(rawText: String, random: Random = Random.Default): List<SourcedQuestion> {
        val root = GeminiJson.parseObject(rawText)

        return root["questions"]?.jsonArray?.mapNotNull { element ->
            runCatching {
                val obj = element.jsonObject
                val options = obj["options"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                val correctIndex = obj["correctAnswerIndex"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val shuffled = options.shuffledKeepingAnswer(correctIndex, random)

                SourcedQuestion(
                    sourceIndex = obj["sourceIndex"]?.jsonPrimitive?.content?.toIntOrNull(),
                    question = QuestionUiState(
                        text = obj["text"]?.jsonPrimitive?.content.orEmpty(),
                        options = shuffled.options,
                        correctAnswerIndex = shuffled.correctAnswerIndex,
                        explanation = obj["explanation"]?.jsonPrimitive?.content.orEmpty(),
                        imageUrl = obj["imageUrl"]?.jsonPrimitive?.content
                            ?.takeIf { it != "null" && it.startsWith("http") }
                    )
                )
            }.getOrNull()
        } ?: emptyList()
    }

    /**
     * Reorders one question's options at random, keeping the correct answer pointing at the right text.
     *
     * @param question the question to shuffle; its options and `correctAnswerIndex` are replaced.
     * @param random source of shuffling, overridable so tests can pin the order.
     * @return a copy of [question] with its options in a new order.
     */
    fun shuffleOptions(question: QuestionUiState, random: Random = Random.Default): QuestionUiState {
        val shuffled = question.options.shuffledKeepingAnswer(question.correctAnswerIndex, random)
        return question.copy(options = shuffled.options, correctAnswerIndex = shuffled.correctAnswerIndex)
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
