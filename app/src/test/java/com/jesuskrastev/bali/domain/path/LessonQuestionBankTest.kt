package com.jesuskrastev.bali.domain.path

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.NodeType
import org.junit.Test

class LessonQuestionBankTest {

    private val lessonNodeIds = DgtLearningPathTemplate.buildInitialPath()
        .filter { it.nodeType == NodeType.LESSON }
        .map { it.id }

    /** Every lesson of the path has enough written questions for a ten-question test. */
    @Test
    fun `every lesson node has at least ten questions`() {
        assertThat(lessonNodeIds).isNotEmpty()
        lessonNodeIds.forEach { nodeId ->
            val questions = LessonQuestionBank.getQuestionsForNode(nodeId, count = Int.MAX_VALUE)
            assertThat(questions.size).isAtLeast(10)
        }
    }

    /** A test takes ten questions, even from nodes that hold many more. */
    @Test
    fun `a test takes ten questions by default`() {
        lessonNodeIds.forEach { nodeId ->
            assertThat(LessonQuestionBank.getQuestionsForNode(nodeId)).hasSize(10)
        }
    }

    /** Every question has two or three options, an answer key that points at one of them and an explanation. */
    @Test
    fun `every question has a valid answer key and an explanation`() {
        lessonNodeIds.forEach { nodeId ->
            LessonQuestionBank.getQuestionsForNode(nodeId, count = Int.MAX_VALUE).forEach { question ->
                assertThat(question.options.size).isIn(2..3)
                assertThat(question.correctAnswerIndex).isIn(question.options.indices)
                assertThat(question.explanation).isNotEmpty()
                assertThat(question.text).isNotEmpty()
            }
        }
    }

    /** Questions of the new bank load their picture from the `questions/` folder of Firebase Storage. */
    @Test
    fun `question pictures point at Firebase Storage`() {
        val withImage = lessonNodeIds
            .flatMap { LessonQuestionBank.getQuestionsForNode(it, count = Int.MAX_VALUE) }
            .filter { it.imageUrl != null }

        assertThat(withImage).isNotEmpty()
        withImage.forEach {
            assertThat(it.imageUrl).startsWith("https://firebasestorage.googleapis.com/")
            assertThat(it.imageUrl).endsWith("?alt=media")
        }
    }

    /** The parser turns image file names into download URLs and keeps legacy URLs as they are. */
    @Test
    fun `parser maps questions and resolves image names`() {
        val json = """
            {"version":1,"nodes":{"node_a":[
              {"text":"T1","options":["a","b"],"correct":1,"explanation":"E1","image":"pic.jpg"},
              {"text":"T2","options":["a","b","c"],"correct":0,"explanation":"E2","legacyImageUrl":"https://x/y.png"},
              {"text":"T3","options":["a","b"],"correct":0,"explanation":"E3"}
            ]}}
        """.trimIndent()

        val nodes = LessonQuestionBankParser.parse(json) { "url/$it" }

        val questions = nodes.getValue("node_a")
        assertThat(questions.map { it.imageUrl }).containsExactly("url/pic.jpg", "https://x/y.png", null).inOrder()
        assertThat(questions[0].correctAnswerIndex).isEqualTo(1)
        assertThat(questions[2].explanation).isEqualTo("E3")
    }
}
