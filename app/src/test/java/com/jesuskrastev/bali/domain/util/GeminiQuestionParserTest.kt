package com.jesuskrastev.bali.domain.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

/**
 * The shuffle these tests cover is load-bearing: measured against the real exam prompt,
 * the model returns the correct answer in the first position 29 times out of 30, so
 * without reordering a student could pass a mock exam by always picking A. And because
 * the reordering moves the answer, a bug in the index remap would mark every question
 * wrong — which is why the mapping is pinned here rather than just the distribution.
 */
class GeminiQuestionParserTest {

    @Test
    fun `the correct answer index still points at the correct option after shuffling`() {
        val json = questionsJson(
            options = listOf("Ceda el paso", "Stop", "Prohibido el paso"),
            correctAnswerIndex = 1
        )

        repeat(50) { seed ->
            val question = GeminiQuestionParser.parse(json, Random(seed)).single()

            assertThat(question.options).containsExactly("Ceda el paso", "Stop", "Prohibido el paso")
            assertThat(question.options[question.correctAnswerIndex]).isEqualTo("Stop")
        }
    }

    @Test
    fun `options that read the same do not make the answer point at the wrong one`() {
        // indexOf on the texts would collapse these two; the permutation is built on
        // indexes precisely so the second "50 km h" stays the correct one.
        val json = questionsJson(
            options = listOf("30 km h", "50 km h", "50 km h"),
            correctAnswerIndex = 2
        )

        repeat(50) { seed ->
            val question = GeminiQuestionParser.parse(json, Random(seed)).single()
            assertThat(question.options[question.correctAnswerIndex]).isEqualTo("50 km h")
        }
    }

    @Test
    fun `the answer no longer sits in the same position every time`() {
        val json = questionsJson(options = listOf("A", "B", "C"), correctAnswerIndex = 0)

        val positions = (0 until 100)
            .map { seed -> GeminiQuestionParser.parse(json, Random(seed)).single().correctAnswerIndex }
            .toSet()

        assertThat(positions).containsExactly(0, 1, 2)
    }

    @Test
    fun `an out of range answer index leaves the question untouched instead of dropping it`() {
        val json = questionsJson(options = listOf("A", "B", "C"), correctAnswerIndex = 7)

        val question = GeminiQuestionParser.parse(json, Random(0)).single()

        assertThat(question.options).containsExactly("A", "B", "C").inOrder()
        assertThat(question.correctAnswerIndex).isEqualTo(7)
    }

    @Test
    fun `json wrapped in prose still parses`() {
        val wrapped = "Aqui tienes el examen:\n" +
            questionsJson(listOf("A", "B", "C"), 0) +
            "\nEspero que te sirva."

        assertThat(GeminiQuestionParser.parse(wrapped, Random(0))).hasSize(1)
    }

    @Test
    fun `a malformed question is skipped without losing the rest`() {
        val json = """
            {"questions":[
              {"text":"Buena","options":["A","B","C"],"correctAnswerIndex":0,"explanation":"ok"},
              {"text":"Rota","options":"no es una lista","correctAnswerIndex":0,"explanation":"x"}
            ]}
        """.trimIndent()

        val questions = GeminiQuestionParser.parse(json, Random(0))

        assertThat(questions).hasSize(1)
        assertThat(questions.single().text).isEqualTo("Buena")
    }

    /**
     * Builds a one-question response in the shape the model returns.
     *
     * @param options the answer options, in the order the model produced them.
     * @param correctAnswerIndex the correct option's position before shuffling.
     * @return the JSON payload to feed the parser.
     */
    private fun questionsJson(options: List<String>, correctAnswerIndex: Int): String {
        val rendered = options.joinToString(",") { "\"$it\"" }
        return """
            {"questions":[{
              "text":"Que indica esta senal?",
              "options":[$rendered],
              "correctAnswerIndex":$correctAnswerIndex,
              "explanation":"Justificacion",
              "imageUrl":null
            }]}
        """.trimIndent()
    }
}
