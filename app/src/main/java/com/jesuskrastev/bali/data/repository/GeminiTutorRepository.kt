package com.jesuskrastev.bali.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
 * Every request carries the whole instruction set plus the recent turns, because the
 * model is stateless between calls — there is no server-side session to resume.
 */
@Singleton
class GeminiTutorRepository @Inject constructor(
    private val gemini: GenerativeModel
) : AiTutorRepository {

    override suspend fun ask(
        question: String,
        history: List<ChatMessage>,
        student: User?
    ): String = withContext(Dispatchers.IO) {
        try {
            val response = gemini.generateContent(buildPrompt(question, history, student))
            response.text?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("La IA no devolvió ninguna respuesta")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            FirebaseCrashlytics.getInstance().recordException(e)
            throw e
        }
    }

    /**
     * Assembles the full prompt: the tutor's role and rules, the student's profile, the
     * recent conversation and finally the new question.
     *
     * @param question the student's latest message.
     * @param history previous turns, oldest first.
     * @param student the student's profile, or null when none exists yet.
     * @return the text sent to Gemini.
     */
    private fun buildPrompt(question: String, history: List<ChatMessage>, student: User?): String {
        val license = student?.licenseType?.takeIf { it.isNotBlank() } ?: "B (Coche)"
        val level = student?.level ?: 1
        val name = student?.name?.takeIf { it.isNotBlank() }
        val difficultTopics = student?.difficultTopics?.takeIf { it.isNotBlank() } ?: "Ninguno indicado"

        val conversation = if (history.isEmpty()) {
            "(Es la primera pregunta de la conversación.)"
        } else {
            history.joinToString("\n") { message ->
                val speaker = if (message.role == ChatRole.USER) "ALUMNO" else "BALI"
                "$speaker: ${message.content}"
            }
        }

        return """
            Eres Bali, un profesor experto en el examen teórico de la DGT (Dirección General
            de Tráfico) de España. Resuelves dudas de un alumno que se está preparando el
            teórico: señales, normas de circulación, prioridades, velocidades, documentación,
            seguridad vial, mecánica básica, primeros auxilios y trámites del examen.

            CONTEXTO DEL ALUMNO (para personalizar la respuesta):
            - Nombre: ${name ?: "desconocido"}.
            - Permiso al que aspira: Permiso $license.
            - Nivel en la app: $level (a mayor nivel, puedes usar lenguaje más técnico).
            - Temas que le cuestan: $difficultTopics.

            REGLAS DE RESPUESTA:
            - Responde SIEMPRE en español de España, tuteando al alumno.
            - Ve al grano: empieza por la respuesta directa y después explica el porqué.
            - Máximo 150 palabras salvo que el alumno pida explícitamente más detalle.
            - Cita la norma o el concepto que aplica (ej: "señal R-1 Ceda el paso",
              "art. 21 del Reglamento General de Circulación") cuando aporte valor.
            - Usa SIEMPRE la normativa española vigente (baliza V-16 obligatoria desde 2026,
              límite de 30 km/h en vías urbanas de un solo carril por sentido, normativa de
              patinetes VMP, tasas de alcoholemia actuales).
            - Si la pregunta es ambigua, responde con la interpretación más probable y añade
              una única pregunta de aclaración al final.
            - Si el alumno pregunta algo que NO tiene relación con la conducción, el tráfico
              o el examen de la DGT, dilo con amabilidad en una frase y reconduce la
              conversación al teórico. No respondas al tema ajeno.
            - Si no estás seguro de un dato, dilo abiertamente en lugar de inventarlo, y
              recomienda contrastarlo con la DGT.
            - Nunca des consejos para saltarse la norma ni para hacer trampas en el examen.

            FORMATO:
            - Texto plano con markdown ligero. Puedes usar **negrita** para lo importante y
              listas con "- " cuando enumeres. No uses títulos, tablas ni bloques de código.
            - No empieces con saludos ni con "¡Buena pregunta!". Entra directo en materia.

            CONVERSACIÓN HASTA AHORA:
            $conversation

            NUEVA PREGUNTA DEL ALUMNO:
            $question

            Responde ahora como Bali, siguiendo todas las reglas anteriores.
        """.trimIndent()
    }
}
