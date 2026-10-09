package com.jesuskrastev.bali.domain.path

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Reads the question bank JSON (`question_bank/lesson_questions.json`) into the static questions
 * that [LessonQuestionBank] serves. Kept apart from the bank so it can be tested with small inputs.
 */
internal object LessonQuestionBankParser {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class BankFile(val version: Int = 1, val nodes: Map<String, List<QuestionDto>> = emptyMap())

    @Serializable
    private data class QuestionDto(
        val text: String,
        val options: List<String>,
        @SerialName("correct") val correctAnswerIndex: Int,
        val explanation: String,
        val image: String? = null,
        val legacyImageUrl: String? = null
    )

    /**
     * Parses the bank file.
     *
     * @param source the JSON text of the bank.
     * @param imageUrl turns an image file name of the new bank into its public download URL.
     * @return the questions of every node, keyed by node id, in file order.
     */
    fun parse(
        source: String,
        imageUrl: (String) -> String
    ): Map<String, List<LessonQuestionBank.StaticQuestion>> =
        json.decodeFromString<BankFile>(source).nodes.mapValues { (_, questions) ->
            questions.map { dto ->
                LessonQuestionBank.StaticQuestion(
                    text = dto.text,
                    options = dto.options,
                    correctAnswerIndex = dto.correctAnswerIndex,
                    explanation = dto.explanation,
                    imageUrl = dto.image?.let(imageUrl) ?: dto.legacyImageUrl
                )
            }
        }
}
