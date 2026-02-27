package com.jesuskrastev.bali.ui.screens.onboarding

/**
 * OnboardingConfig holds all the static data for the onboarding flow.
 * Separating data from logic improves maintainability and testability.
 */
object OnboardingConfig {
    val licenses = listOf(
        "🚗 Coche (B)", "🏍️ Moto (A2)", "\uD83C\uDFCD Moto (A1)", "🛵 Ciclomotor (AM)", "🚛 Camión (C)", "🚌 Autobús (D)", "🚜 Remolque (E)"
    )

    val experiences = listOf(
        "\uD83C\uDF93 Empiezo de cero absoluto",
        "\uD83D\uDCD6 Ya tengo algunas nociones básicas",
        "\uD83D\uDD01 He suspendido y quiero repetirlo",
        "\uD83E\uDEA7 Ya tengo otro carnet"
    )

    val reasons = listOf(
        "💼 Trabajo", "🏠 Independencia", "✈️ Viajes", "👨‍👩‍👧‍👦 Familia", "🛠️ Oportunidad académica", "🏎️ Disfrute personal"
    )

    val dailyGoals = listOf(
        "⚡ 15 min (Modo rápido)",
        "\uD83D\uDD53 30 min (Recomendado)",
        "🔥 1 hora (Intensivo)"
    )

    val learningPreferences = listOf(
        "📝 Solo tests", "🤖 Explicaciones con IA", "📚 Teoría y esquemas", "🎓 Simulacros de examen"
    )

    val difficultTopics = listOf(
        "🛑 Señales y marcas", "🏎️ Velocidades máximas", "🚦 Prioridades de paso",
        "📄 Documentación y multas", "🔧 Mecánica y luces", "🍻 Alcohol, drogas y fatiga"
    )

    val concerns = listOf(
        "\uD83E\uDEA4 Las preguntas trampa", "⏳ Quedarme sin tiempo", "😰 Los nervios del momento", "\uD83D\uDEAB Bloquearme"
    )

    val studyTimes = listOf(
        "🌅 Mañana", "☀️ Mediodía", "🌇 Tarde", "🌙 Noche"
    )
}
