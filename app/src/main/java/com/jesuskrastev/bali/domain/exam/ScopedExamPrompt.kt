package com.jesuskrastev.bali.domain.exam

/**
 * Writes the prompt that asks Gemini for new exam questions out of the lessons' own questions.
 *
 * The prompt hands over the study material and forbids anything outside it, and forbids pictures
 * altogether: a question with an image only works with the image it was written for, and Gemini
 * cannot supply one that matches.
 */
object ScopedExamPrompt {

    /**
     * What the prompt knows about the student, for tuning difficulty.
     *
     * @property license the permit the student is going for, e.g. "B (Coche)"
     * @property level the student's level in the app
     * @property experience the student's previous driving experience
     * @property difficultTopics the topics the student says they struggle with
     * @property totalTests how many tests the student has taken
     * @property daysToExam days until the real exam, or null when there is no date
     */
    data class Student(
        val license: String,
        val level: Int,
        val experience: String,
        val difficultTopics: String,
        val totalTests: Int,
        val daysToExam: Long?
    )

    /**
     * Builds the prompt.
     *
     * @param scopeTitle what the exam covers, e.g. the unit's title
     * @param sources the study material, grouped by lesson
     * @param student what is known about the student
     * @param questionCount how many questions to ask Gemini for
     * @return the prompt text
     */
    fun build(scopeTitle: String, sources: List<ExamLesson>, student: Student, questionCount: Int): String {
        val urgency = if (student.daysToExam != null && student.daysToExam in 1..15) {
            "¡El examen real es en ${student.daysToExam} días! Sé exigente."
        } else {
            "Simulacro estándar."
        }

        return """
            Eres el Examinador Jefe de la DGT en España. Vas a escribir un examen de EXACTAMENTE $questionCount preguntas nuevas sobre: $scopeTitle.

            El alumno SOLO ha estudiado el MATERIAL DE ESTUDIO de abajo. Es lo único de lo que puede preguntarse.

            CONTEXTO DEL ALUMNO:
            - Permiso al que aspira: Permiso ${student.license}.
            - Nivel en la app: ${student.level} (a mayor nivel, distractores más sutiles).
            - Tests realizados: ${student.totalTests}.
            - Experiencia previa: ${student.experience}.
            - Temas que más le cuestan: ${student.difficultTopics}.
            - Urgencia: $urgency

            MATERIAL DE ESTUDIO (preguntas que el alumno ya ha visto, con su respuesta correcta):
            ${material(sources)}

            REGLAS (ESTRICTAS):
            - Cada pregunta nueva evalúa un concepto del material, planteado de OTRA manera: otro enunciado, otra situación, otras opciones. No copies ninguna pregunta del material.
            - No introduzcas normas, cifras ni conceptos que no estén en el material. Si lo que tú sabes contradice al material, manda el material.
            - Reparte las preguntas entre las lecciones del material; no dediques todas a la misma.
            - NO USES IMÁGENES. Nunca escribas "en la imagen", "esta señal", "la figura" ni nada que suponga ver algo. La pregunta debe poder contestarse solo leyendo. Para hablar de una señal, nómbrala con su código o su descripción ("la señal R-2 de STOP").
            - Si una pregunta del material depende de una imagen y su explicación no basta para plantearla sin ella, descártala y usa otra.
            - Estilo DGT oficial: lenguaje técnico y situaciones hipotéticas ("Circula por una vía...", "Como norma general...").
            - 3 opciones con el TEXTO REAL de cada respuesta, nunca solo "A", "B" o "C". Una sola es correcta. Las otras deben ser creíbles y usar las trampas típicas de la DGT ("siempre", "nunca", "solo").
            - EXPLICACIÓN: máximo 20 palabras, clara, y que justifique la norma.
            - Deja imageUrl a null en todas.

            Genera EXACTAMENTE $questionCount preguntas.
        """.trimIndent()
    }

    /** Lists the material lesson by lesson, with each question's right answer and explanation. */
    private fun material(sources: List<ExamLesson>): String =
        sources.joinToString("\n\n") { lesson ->
            val questions = lesson.questions.joinToString("\n") { question ->
                val picture = if (question.imageUrl != null) " [en el original acompañaba una imagen]" else ""
                "- Pregunta: ${question.text}$picture\n" +
                    "  Respuesta correcta: ${question.options.getOrElse(question.correctAnswerIndex) { "" }}\n" +
                    "  Explicación: ${question.explanation}"
            }
            "### ${lesson.title}\n$questions"
        }
}
