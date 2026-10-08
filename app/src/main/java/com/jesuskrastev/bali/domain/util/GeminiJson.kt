package com.jesuskrastev.bali.domain.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Reads the JSON object out of a Gemini reply.
 *
 * The models are asked for schema-constrained JSON, but the text is still scanned for the
 * outermost object so an older or degraded reply with a preamble around it still parses instead
 * of costing a generation and giving nothing back.
 */
object GeminiJson {

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
    }

    /**
     * Parses the outermost JSON object of [rawText].
     *
     * @param rawText the model's reply; JSON, optionally wrapped in other text.
     * @return the parsed object.
     * @throws IllegalArgumentException when the text holds no JSON object at all; the message is
     *   in Spanish because the screens show it to the student.
     */
    fun parseObject(rawText: String): JsonObject {
        val start = rawText.indexOf('{')
        val end = rawText.lastIndexOf('}')
        require(start != -1 && end > start) { "La IA no devolvió un JSON válido" }
        return json.parseToJsonElement(rawText.substring(start, end + 1)).jsonObject
    }
}
