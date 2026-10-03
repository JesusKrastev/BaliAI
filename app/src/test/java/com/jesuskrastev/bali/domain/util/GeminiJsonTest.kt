package com.jesuskrastev.bali.domain.util

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertThrows
import org.junit.Test

class GeminiJsonTest {

    @Test
    fun `an object wrapped in prose is found and parsed`() {
        val root = GeminiJson.parseObject("Aquí tienes: {\"selectedCategory\": \"Señales\"} ¡Suerte!")

        assertThat(root["selectedCategory"]?.jsonPrimitive?.content).isEqualTo("Señales")
    }

    @Test
    fun `a trailing comma and unknown keys are tolerated`() {
        val root = GeminiJson.parseObject("{\"nodes\": [], \"extra\": 1,}")

        assertThat(root["nodes"]).isNotNull()
    }

    @Test
    fun `text without an object is rejected with a message for the student`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            GeminiJson.parseObject("Lo siento, no puedo ayudarte con eso.")
        }

        assertThat(error).hasMessageThat().contains("JSON")
    }
}
