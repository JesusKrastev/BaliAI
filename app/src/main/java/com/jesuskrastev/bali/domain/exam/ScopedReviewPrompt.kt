package com.jesuskrastev.bali.domain.exam

/**
 * Writes the prompt that asks Gemini to reword the questions of a review.
 *
 * Gemini never produces or chooses a picture here: a question that came with one keeps it, and
 * the model only has to word the question so it still fits that picture.
 */
object ScopedReviewPrompt {

    /**
     * Builds the prompt.
     *
     * @param scopeTitle what the review covers, e.g. the unit's title
     * @param sources the numbered questions to rewrite
     * @param student what is known about the student
     * @return the prompt text
     */
    fun build(scopeTitle: String, sources: List<ScopedReviewComposer.Source>, student: ScopedExamPrompt.Student): String =
        """
            Eres un Profesor Experto de la DGT en España. Vas a preparar un REPASO sobre: $scopeTitle.

            Te doy ${sources.size} preguntas que el alumno falla o necesita repasar. Escribe EXACTAMENTE ${sources.size} preguntas, UNA por cada una, para comprobar si de verdad ha aprendido el concepto.

            CONTEXTO DEL ALUMNO:
            - Permiso al que aspira: Permiso ${student.license}.
            - Nivel en la app: ${student.level} (a mayor nivel, distractores más sutiles).
            - Experiencia previa: ${student.experience}.
            - Temas que más le cuestan: ${student.difficultTopics}.

            PREGUNTAS A REESCRIBIR:
            ${material(sources)}

            REGLAS (ESTRICTAS):
            - Cada pregunta nueva trata EXACTAMENTE del mismo concepto que la original y tiene el MISMO significado de respuesta correcta. Cambia la redacción y hazla algo más enrevesada: otro enunciado, otra situación, distractores más tentadores. No copies el texto original.
            - En "sourceIndex" pon el número de la pregunta original de la que parte.
            - No añadas normas, cifras ni conceptos que no estén en la pregunta original y su explicación.
            - Si la original lleva una imagen, esa MISMA imagen se mostrará con tu pregunta. Puedes referirte a ella ("la señal de la imagen", "la situación de la imagen"), pero no cambies lo que se pregunta sobre ella ni qué respuesta es la correcta. No describas la imagen con detalles que no estén en la original.
            - Si la original NO lleva imagen, no hagas referencia a ninguna imagen ni figura: la pregunta debe poder contestarse leyendo.
            - Deja imageUrl a null siempre. Nunca escribas URLs.
            - 3 opciones con el TEXTO REAL de cada respuesta, nunca solo "A", "B" o "C". Una sola es correcta. Las otras, creíbles, con las trampas típicas de la DGT ("siempre", "nunca", "solo").
            - EXPLICACIÓN: máximo 20 palabras, clara, y que justifique la norma.

            Genera EXACTAMENTE ${sources.size} preguntas.
        """.trimIndent()

    /** Lists the sources by number, with the right answer and explanation, and a note when a picture goes with them. */
    private fun material(sources: List<ScopedReviewComposer.Source>): String =
        sources.joinToString("\n\n") { source ->
            val question = source.question
            val picture = if (question.imageUrl != null) " [lleva imagen]" else ""
            "[${source.index}] (${source.lessonTitle})$picture\n" +
                "Pregunta: ${question.text}\n" +
                "Respuesta correcta: ${question.options.getOrElse(question.correctAnswerIndex) { "" }}\n" +
                "Explicación: ${question.explanation}"
        }
}
