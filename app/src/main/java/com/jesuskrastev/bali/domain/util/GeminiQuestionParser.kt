package com.jesuskrastev.bali.domain.util

import com.jesuskrastev.bali.ui.screens.test.QuestionUiState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses Gemini's raw text response into a list of [QuestionUiState].
 * Extracts the first valid JSON object found in the response text,
 * tolerating preamble or trailing text that the model may include.
 */
object GeminiQuestionParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
    }

    fun parse(rawText: String): List<QuestionUiState> {
        val start = rawText.indexOf('{')
        val end = rawText.lastIndexOf('}')
        require(start != -1 && end != -1) { "No valid JSON block found in Gemini response" }

        val jsonString = rawText.substring(start, end + 1)
        val root = json.parseToJsonElement(jsonString).jsonObject

        return root["questions"]?.jsonArray?.mapNotNull { element ->
            runCatching {
                val obj = element.jsonObject
                QuestionUiState(
                    text = obj["text"]?.jsonPrimitive?.content.orEmpty(),
                    options = obj["options"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                    correctAnswerIndex = obj["correctAnswerIndex"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                    explanation = obj["explanation"]?.jsonPrimitive?.content.orEmpty(),
                    imageUrl = obj["imageUrl"]?.jsonPrimitive?.content
                        ?.takeIf { it != "null" && it.startsWith("http") }
                )
            }.getOrNull()
        } ?: emptyList()
    }
}
