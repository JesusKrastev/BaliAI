package com.jesuskrastev.bali.domain.model

import java.text.Normalizer

/**
 * The ten topics the app teaches, as the AI is told to name them when it builds a practice
 * session.
 *
 * Practice results only store a free-text category (a topic name or the title of a learning-path
 * node), so [fromCategory] matches that text against [keywords] instead of reading a stored topic.
 *
 * @property displayName name shown to the user
 * @property keywords accent-free, lowercase fragments that identify the topic inside a category
 */
enum class DrivingTopic(val displayName: String, private val keywords: List<String>) {
    LIGHTING("Alumbrado", listOf("alumbrado", "luces", "faros", "alumbr")),
    RIGHT_OF_WAY("Prioridad", listOf("prioridad", "interseccion", "cruce", "ceda", "glorieta", "rotonda")),
    MANEUVERS("Maniobras", listOf("maniobra", "adelant", "giro", "cambio de sentido", "marcha atras", "estacion")),
    SPEED("Velocidad", listOf("velocidad", "distancia de seguridad", "frenado", "limite")),
    DRIVER("El conductor", listOf("conductor", "alcohol", "droga", "fatiga", "cansancio", "cinturon", "sueno", "distraccion")),
    MECHANICS("Mecánica", listOf("mecanic", "neumatic", "motor", "averia", "frenos", "mantenimiento", "bateria")),
    DOCUMENTS("Documentación", listOf("document", "permiso", "carnet", "seguro", "itv", "licencia", "puntos")),
    ROAD_USERS("Usuarios de la vía", listOf("usuarios", "peaton", "ciclista", "moto", "vulnerable", "autobus", "transporte")),
    SIGNS("Señales", listOf("senal", "semaforo", "agente")),
    ROAD_MARKINGS("Marcas viales", listOf("marca", "linea", "continua", "discontinua", "pintura"));

    companion object {
        /**
         * Reads a stored topic back.
         *
         * @param tag the enum name saved with an answer, null for answers without a topic
         * @return the topic, or null when [tag] is null or names a topic this version does not know
         */
        fun fromTag(tag: String?): DrivingTopic? = entries.firstOrNull { it.name == tag }

        /**
         * Finds the topic a practice category belongs to.
         *
         * The category is compared without accents or case. "Marcas viales" is checked before
         * "Señales" because a signs lesson about road markings must count as markings, which is
         * why the lookup walks [entries] in a fixed priority order rather than declaration order.
         *
         * @param category the [TestResult.category] of a practice session
         * @return the matching topic, or null when the category is a lesson title that names none
         */
        fun fromCategory(category: String): DrivingTopic? {
            val text = category.normalized()
            if (text.isBlank()) return null
            return MATCH_ORDER.firstOrNull { topic -> topic.keywords.any { it in text } }
        }

        /** Markings and mechanics first: their words ("señal", "motor") appear in longer titles. */
        private val MATCH_ORDER = listOf(
            ROAD_MARKINGS, SIGNS, LIGHTING, RIGHT_OF_WAY, SPEED, MANEUVERS,
            MECHANICS, DOCUMENTS, ROAD_USERS, DRIVER
        )

        /** Lowercases [this] and strips accents and the ñ so keywords match any spelling. */
        private fun String.normalized(): String =
            Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }
}
