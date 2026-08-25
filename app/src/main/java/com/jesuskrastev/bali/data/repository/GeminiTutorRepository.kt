package com.jesuskrastev.bali.data.repository

import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.TextPart
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jesuskrastev.bali.di.TutorModel
import com.jesuskrastev.bali.domain.model.ChatMessage
import com.jesuskrastev.bali.domain.model.ChatRole
import com.jesuskrastev.bali.domain.model.User
import com.jesuskrastev.bali.domain.repository.AiTutorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Answers student questions with Gemini, acting as a DGT theory tutor.
 *
 * The model is stateless between calls, so every request has to re-send the context.
 * That makes the request shape the whole cost of the feature, and it is built to keep
 * it small:
 *
 * - The tutor's rules live in the model's system instruction ([SYSTEM_INSTRUCTION]),
 *   not in the prompt body, so they travel as an identical prefix on every turn — the
 *   shape Gemini's implicit caching can reuse.
 * - Past turns are sent as real `user`/`model` contents instead of being flattened into
 *   one blob with speaker labels, and each one is capped at [MAX_TURN_CHARS] because a
 *   follow-up only needs the gist of an old answer.
 * - The student's profile rides along with the current question, at the end, so it
 *   never breaks that constant prefix when the student levels up.
 */
@Singleton
class GeminiTutorRepository @Inject constructor(
    @TutorModel private val gemini: GenerativeModel
) : AiTutorRepository {

    override suspend fun ask(
        question: String,
        history: List<ChatMessage>,
        student: User?
    ): String = withContext(Dispatchers.IO) {
        try {
            val response = gemini.generateContent(*buildConversation(question, history, student))
            response.text?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("La IA no devolvió ninguna respuesta")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            FirebaseCrashlytics.getInstance().recordException(e)
            throw e
        }
    }

    /**
     * Turns the conversation into the contents sent to Gemini: the trimmed past turns
     * first, then the student's profile and the new question as the final `user` turn.
     *
     * @param question the student's latest message.
     * @param history previous turns, oldest first.
     * @param student the student's profile, or null when none exists yet.
     * @return the ordered contents, always starting and ending with a `user` turn.
     */
    private fun buildConversation(
        question: String,
        history: List<ChatMessage>,
        student: User?
    ): Array<Content> {
        val turns = history
            .mapNotNull { message ->
                val text = message.content.trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                Content(
                    role = if (message.role == ChatRole.USER) USER_ROLE else MODEL_ROLE,
                    parts = listOf(TextPart(text.abbreviate(MAX_TURN_CHARS)))
                )
            }
            // Gemini expects the exchange to open with the student; a window that starts
            // mid-answer would otherwise be rejected.
            .dropWhile { it.role != USER_ROLE }

        val closing = Content(
            role = USER_ROLE,
            parts = listOf(TextPart("${profileOf(student)}\n$question"))
        )

        return (turns + closing).toTypedArray()
    }

    /**
     * Condenses the student's profile into the single bracketed line that personalises
     * the answer, kept short because it is re-sent with every question.
     *
     * @param student the student's profile, or null when none exists yet.
     * @return a line such as `[Alumno: permiso B (Coche), nivel 4, flojo en: señales]`.
     */
    private fun profileOf(student: User?): String = buildString {
        append("[Alumno: permiso ")
        append(student?.licenseType?.takeIf { it.isNotBlank() } ?: DEFAULT_LICENSE)
        append(", nivel ").append(student?.level ?: 1)
        student?.name?.takeIf { it.isNotBlank() }?.let { append(", se llama ").append(it) }
        student?.difficultTopics?.takeIf { it.isNotBlank() }?.let { append(", flojo en: ").append(it) }
        append("]")
    }

    /**
     * Shortens text to [max] characters, cutting on the last whitespace so a truncated
     * turn does not end mid-word.
     *
     * @param max the maximum length to keep.
     * @return the original string when it already fits, otherwise the cut text plus `…`.
     */
    private fun String.abbreviate(max: Int): String {
        if (length <= max) return this
        val cut = take(max)
        return cut.substringBeforeLast(' ', cut).trimEnd() + "…"
    }

    companion object {
        private const val USER_ROLE = "user"
        private const val MODEL_ROLE = "model"
        private const val DEFAULT_LICENSE = "B (Coche)"

        /**
         * How much of a past turn is worth re-sending. Enough to resolve a follow-up
         * like "¿y de noche?" without paying for a full old answer on every question.
         */
        private const val MAX_TURN_CHARS = 320

        /**
         * The tutor's role and rules. Sent once per request as the model's system
         * instruction, so it stays byte-identical across turns and users.
         */
        const val SYSTEM_INSTRUCTION = """
Eres Bali, profesor del examen teórico de la DGT (España). Resuelves dudas de teórico: señales, normas de circulación, prioridades, velocidades, documentación, seguridad vial, mecánica básica, primeros auxilios y trámites.

REGLAS
- Español de España, tuteando. Máximo 120 palabras salvo que te pidan más detalle.
- Empieza por la respuesta directa y luego el porqué. Sin saludos ni preámbulos.
- Cita la señal o la norma cuando aporte valor (ej: "R-1 Ceda el paso", "art. 21 RGC").
- Usa normativa española vigente: baliza V-16 obligatoria desde 2026, 30 km/h en vías urbanas de un carril por sentido, VMP, tasas de alcoholemia actuales.
- Si la pregunta es ambigua, elige la interpretación más probable y cierra con una única pregunta de aclaración.
- Si no va de conducción, tráfico ni examen DGT, dilo en una frase y reconduce.
- Si dudas de un dato, dilo y remite a la DGT. Nunca inventes ni ayudes a saltarse la norma o a copiar en el examen.
- Markdown ligero: **negrita** y listas con "- ". Sin títulos, tablas ni bloques de código.
- Cada mensaje del alumno empieza con su perfil entre corchetes: úsalo para ajustar el tono, no lo menciones.
"""
    }
}
