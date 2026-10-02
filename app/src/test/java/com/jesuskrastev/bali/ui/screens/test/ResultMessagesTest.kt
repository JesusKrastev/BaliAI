package com.jesuskrastev.bali.ui.screens.test

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

/** The bank of result messages: enough variety, the right tone and the right tier for each result. */
class ResultMessagesTest {

    private fun everyMessage(tier: ResultTier) = ResultMessages.poolFor(tier, ResultPace.FAST)

    @Test
    fun `every tier has at least the minimum number of messages`() {
        for (tier in ResultTier.entries) {
            assertThat(ResultMessages.bank.getValue(tier).size)
                .isAtLeast(ResultMessages.MIN_MESSAGES_PER_TIER)
        }
    }

    @Test
    fun `no tier repeats a title and none is blank`() {
        for (tier in ResultTier.entries) {
            val messages = everyMessage(tier)
            assertThat(messages.map { it.title }.toSet()).hasSize(messages.size)
            messages.forEach {
                assertThat(it.title).isNotEmpty()
                assertThat(it.subtitle).isNotEmpty()
            }
        }
    }

    @Test
    fun `titles stay short enough for the headline`() {
        for (tier in ResultTier.entries) {
            everyMessage(tier).forEach { assertThat(it.title.length).isAtMost(48) }
        }
    }

    @Test
    fun `no message promises the real exam will be passed`() {
        val promises = listOf("garantiz", "seguro que apru", "aprobarás", "aprobaras", "aprobarás seguro", "vas a aprobar")
        for (tier in ResultTier.entries) {
            everyMessage(tier).forEach { message ->
                val text = (message.title + " " + message.subtitle).lowercase()
                promises.forEach { assertThat(text).doesNotContain(it) }
            }
        }
    }

    @Test
    fun `a failed exam never gets a message from a celebrated tier`() {
        val failed = listOf(ResultTier.EXAM_CLOSE, ResultTier.EXAM_FAILED, ResultTier.EXAM_FAR)
        val celebrated = ResultTier.entries.filter { it.isCelebrated }
            .flatMap { everyMessage(it) }.toSet()
        for (tier in failed) {
            repeat(200) { seed ->
                val message = ResultMessages.pick(tier, ResultPace.FAST, Random(seed))
                assertThat(celebrated).doesNotContain(message)
            }
        }
    }

    @Test
    fun `a failed exam never says it passed`() {
        val failedTiers = listOf(ResultTier.EXAM_CLOSE, ResultTier.EXAM_FAILED, ResultTier.EXAM_FAR)
        for (tier in failedTiers) {
            everyMessage(tier).forEach { message ->
                val text = (message.title + " " + message.subtitle).lowercase()
                assertThat(text).doesNotContain("aprobarías")
                assertThat(text).doesNotContain("simulacro aprobado")
                assertThat(text).doesNotContain("enhorabuena")
            }
        }
    }

    @Test
    fun `failing tiers point at something to do next`() {
        // A loose check by verbs and nouns of action: it catches a message that only consoles.
        val steps = listOf(
            "repas", "repite", "revisa", "lee ", "haz ", "juega", "vuelve", "volver", "vuelta", "empieza",
            "chat", "partida", "lección", "anota", "insiste", "pídele"
        )
        for (tier in ResultTier.entries.filter { !it.isCelebrated }) {
            ResultMessages.bank.getValue(tier).forEach { message ->
                val text = (message.title + " " + message.subtitle).lowercase()
                assertThat(steps.any { text.contains(it) }).isTrue()
            }
        }
    }

    @Test
    fun `fast pace adds messages and normal pace does not`() {
        val tier = ResultTier.LESSON_PERFECT
        assertThat(ResultMessages.poolFor(tier, ResultPace.FAST).size)
            .isGreaterThan(ResultMessages.poolFor(tier, ResultPace.NORMAL).size)
        assertThat(ResultMessages.poolFor(tier, ResultPace.NORMAL)).isEqualTo(ResultMessages.bank.getValue(tier))
    }

    @Test
    fun `the message just shown is skipped`() {
        val tier = ResultTier.LESSON_GOOD
        val shown = ResultMessages.bank.getValue(tier).first().title
        repeat(300) { seed ->
            assertThat(ResultMessages.pick(tier, ResultPace.NORMAL, Random(seed), avoid = shown).title)
                .isNotEqualTo(shown)
        }
    }

    @Test
    fun `picking is random over the whole pool`() {
        val tier = ResultTier.GAME_HIGH
        val picked = (0 until 500).map { ResultMessages.pick(tier, ResultPace.NORMAL, Random(it)).title }.toSet()
        assertThat(picked).hasSize(ResultMessages.bank.getValue(tier).size)
    }

    @Test
    fun `the same seed gives the same message`() {
        val a = ResultMessages.pick(ResultTier.EXAM_PASSED, ResultPace.NORMAL, Random(7))
        val b = ResultMessages.pick(ResultTier.EXAM_PASSED, ResultPace.NORMAL, Random(7))
        assertThat(a).isEqualTo(b)
    }
}
