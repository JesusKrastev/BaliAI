package com.jesuskrastev.bali.ui.screens.games

/**
 * Identifies the rapid-learning mini-games available from the arcade.
 *
 * @param lengthLabel how long a session is, shown on the catalogue card
 */
enum class GameType(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val lengthLabel: String = "$ROUNDS_PER_SESSION RONDAS",
) {
    DRIVE("bali_drive", "Bali Drive", "Conduce con un dedo: esquiva, frena y cede el paso", "🚗", "1 MIN"),
    PUNTOS_CARNE("puntos_carne", "Puntos del Carné", "¿Sabes cuántos puntos te quita cada infracción?", "🪪"),
    SENAL("senal", "Señal Relámpago", "Clasifica la señal antes de que se agote el tiempo", "🚸"),
    LEGAL_O_MULTA("legal_o_multa", "¿Legal o Multa?", "50 situaciones: decide rápido", "🅿️"),
    PELIGRO("peligro", "Encuentra el Peligro", "50 escenas: detecta la amenaza", "⚠️"),
    PRIORIDAD_CRUCE("prioridad_cruce", "Prioridad en el Cruce", "¿Quién pasa primero en cada cruce?", "🔀");

    companion object {
        /** Returns the game matching [id], falling back to the first game for an invalid route. */
        fun fromId(id: String): GameType = entries.firstOrNull { it.id == id } ?: PUNTOS_CARNE
    }
}
