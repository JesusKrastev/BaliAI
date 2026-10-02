package com.jesuskrastev.bali.ui.screens.test

import kotlin.random.Random

/**
 * One motivational message of the result screen.
 *
 * @property title the headline, short, may end in one emoji
 * @property subtitle one or two sentences under it
 */
data class ResultMessage(val title: String, val subtitle: String)

private fun m(title: String, subtitle: String) = ResultMessage(title, subtitle)

/**
 * The bank of messages for the result screen, by [ResultTier]. Plain Kotlin, apart from the UI, so
 * it can be tested and grown without touching a composable.
 *
 * Rules every message follows:
 * - Warm and close, in Bali's voice, never patronising.
 * - Below the bar (failed exam, low score) it encourages and names a next step.
 * - It never promises the real DGT exam will be passed: the app cannot guarantee that. "Con este
 *   resultado aprobarías el teórico" is a fact about the mock exam, so it is allowed there.
 * - A failed exam never gets a message that celebrates (see [ResultTier.isCelebrated]).
 */
object ResultMessages {

    /** Smallest number of messages every tier must have, so the variety is never lost silently. */
    const val MIN_MESSAGES_PER_TIER = 10

    /**
     * Picks a message for a result, at random.
     *
     * @param tier how the result is read
     * @param pace how fast the questions were answered; [ResultPace.FAST] adds the speed messages
     *   to the pool
     * @param random source of randomness, replaceable in tests
     * @param avoid title of the message shown last time, skipped when the pool has another, so the
     *   same words do not come twice in a row
     * @return the chosen message
     */
    fun pick(
        tier: ResultTier,
        pace: ResultPace = ResultPace.NORMAL,
        random: Random = Random.Default,
        avoid: String? = null
    ): ResultMessage {
        val pool = poolFor(tier, pace)
        val candidates = pool.filter { it.title != avoid }.ifEmpty { pool }
        return candidates[random.nextInt(candidates.size)]
    }

    /**
     * Lists every message a result can get.
     *
     * @param tier how the result is read
     * @param pace how fast the questions were answered
     * @return the tier's messages, plus its speed messages when [pace] is [ResultPace.FAST]
     */
    fun poolFor(tier: ResultTier, pace: ResultPace): List<ResultMessage> =
        bank.getValue(tier) + if (pace == ResultPace.FAST) fast[tier].orEmpty() else emptyList()

    /** Every tier's base messages. Exposed to tests through [poolFor]. */
    internal val bank: Map<ResultTier, List<ResultMessage>> = mapOf(
        ResultTier.LESSON_PERFECT to listOf(
            m("¡Perfecto, sin un solo fallo! 🎯", "Esta lección ya no tiene secretos para ti."),
            m("¡Pleno total! 🏆", "Todo acertado. Bali está dando saltitos de alegría."),
            m("¡Impecable! ✨", "Ni una duda, ni un fallo. Así se estudia."),
            m("¡Cien por cien! 💯", "Lo has clavado de principio a fin."),
            m("¡Esto es dominar un tema! 🔥", "Si todos los tests salieran así, el teórico sería un paseo."),
            m("¡Sin fallos, sin piedad! 😎", "La lección se ha rendido ante ti."),
            m("¡Redondo, de verdad! 🌟", "Hoy has hecho el test perfecto. Apúntatelo."),
            m("¡Qué nivelazo! 🚀", "Todas bien. Esta lección ya la tienes en el bolsillo."),
            m("¡Matrícula de honor! 🎓", "Cero fallos. Bali te daría un abrazo si pudiera."),
            m("¡Así se hace! 👏", "Perfecto de arriba abajo. Sigue con esa cabeza."),
            m("¡Memoria de elefante! 🐘", "No se te ha escapado ni una. Eso es repasar bien."),
            m("¡Fallo cero! 🙌", "Cuando un tema sale así, toca pasar al siguiente con la cabeza alta.")
        ),
        ResultTier.LESSON_EXCELLENT to listOf(
            m("¡Casi perfecto! 🔥", "Un fallito de nada. Míralo y la próxima será redonda."),
            m("¡Qué buen test! ⭐", "Estás muy cerca de dominar este tema."),
            m("¡Excelente resultado! 💪", "Casi todo bien. Se nota que has estudiado."),
            m("¡A un paso del pleno! 🎯", "Repasa lo que has fallado y lo tendrás al cien por cien."),
            m("¡Muy muy bien! 👏", "Tienes el tema dominado, solo queda pulir algún detalle."),
            m("¡Vas como un tiro! 🚀", "Un resultado así da confianza. Aprovecha el impulso."),
            m("¡Nota de sobresaliente! 🌟", "Esa pregunta que se escapó es la que más te va a enseñar."),
            m("¡Gran trabajo! 😄", "Casi sin fallos. Bali está orgulloso de ti."),
            m("¡Qué manera de rendir! 🏅", "Con este nivel, el siguiente tema debería caer sin problema."),
            m("¡Casi, casi, casi! 😏", "El pleno estaba ahí al lado. Repasa el fallo y vuelve a por él."),
            m("¡Esto sí que es constancia! 🌱", "Los días de estudio se notan en resultados como este."),
            m("¡Muy sólido! 🧱", "Tienes base de sobra. Un repaso rápido al fallo y listo.")
        ),
        ResultTier.LESSON_GOOD to listOf(
            m("¡Buen trabajo! 💪", "Lección superada. Repasa los fallos y la próxima saldrá aún mejor."),
            m("¡Lo has sacado adelante! ✅", "Vas sumando. Cada test deja menos huecos."),
            m("¡Bien hecho! 🙂", "Más aciertos que fallos de sobra. Mira los errores y afina."),
            m("¡Vas por muy buen camino! 🛣️", "Los fallos de hoy son los aciertos de mañana."),
            m("¡Paso a paso se llega! 👣", "Has superado la lección. Revisa lo que falló y sigue."),
            m("¡Sólido! 🧱", "Buena base. Con un repaso a los fallos la subes de nivel."),
            m("¡Tema en marcha! 🚗", "Ya controlas casi todo. Pulir los fallos es lo que te falta."),
            m("¡Muy buen ritmo! 📈", "Así, test a test, el temario se va quedando."),
            m("¡Eso es constancia! 🌱", "Resultado de los que suman. Sigue con la racha."),
            m("¡Lección en el bolsillo! 🎒", "Y el siguiente paso del camino te espera."),
            m("¡Otro test para el saco! 🙌", "Un buen resultado. ¿Y si repasas los fallos antes de salir?"),
            m("¡Cada vez más cerca! 🎯", "Los errores que te quedan son pocos y se pueden arreglar.")
        ),
        ResultTier.LESSON_FAIR to listOf(
            m("Vas por buen camino 📈", "Más de la mitad bien. Repasa los fallos y repite: notarás la diferencia."),
            m("A medias, pero avanzando 🧩", "Lo que has acertado es base firme. Los fallos, tu próximo repaso."),
            m("Esto se puede mejorar 💡", "Lee la explicación de cada fallo y vuelve a intentarlo."),
            m("Ya tienes la mitad 🌓", "Falta la otra mitad: repasa las preguntas que fallaste."),
            m("Un test más y mejora 🔁", "La segunda vuelta siempre sale mejor. Repite la lección."),
            m("Aún hay hueco para crecer 🌱", "Pregúntale al Chat lo que no te quedó claro y repite."),
            m("No está mal para empezar 🙂", "Hay temas que cuestan más. Insiste con este y se va a dar."),
            m("Tranquilo, esto es practicar 🚦", "Para eso están los tests: falla aquí y repasa lo que no sabías."),
            m("Paso a paso 👣", "Revisa los fallos con calma. Con eso ya habrás aprendido más que haciendo otro test."),
            m("Casi, pero no del todo 🎯", "Con un repaso de los errores subes de golpe."),
            m("Hoy ha costado un poco 😮‍💨", "Pasa. Descansa un momento y vuelve a intentarlo, que mejora."),
            m("Base hay, falta afinar 🛠️", "Repasa lo fallado y repite la lección: te sorprenderá lo que cambia.")
        ),
        ResultTier.LESSON_LOW to listOf(
            m("Aquí empieza el aprendizaje 🌱", "Nadie nace sabiendo el reglamento. Lee las explicaciones y repite."),
            m("No te rindas 🧠", "Cada fallo cuenta como lección. Repasa y vuelve a intentarlo."),
            m("Este tema se resiste, de momento 💪", "Léete las explicaciones de los fallos y repite el test."),
            m("Hoy no ha salido, y no pasa nada 🫶", "Lo importante es volver. Mañana lo ves con otros ojos."),
            m("Un mal día lo tiene cualquiera ☁️", "Repasa los fallos con calma: es la forma más rápida de mejorar."),
            m("Empezar cuesta 🏔️", "Los temas nuevos son así. Con un par de vueltas cambia todo."),
            m("Primero entender, luego acertar 📖", "Pídele al Chat que te explique lo que no ha quedado claro."),
            m("Sigue, que merece la pena 🚗", "Fallar aquí no cuenta para nada. Lee la explicación de cada fallo y repite la lección."),
            m("Todavía no, pero casi 🔁", "Repite la lección: la segunda vez suele ir mucho mejor."),
            m("Bali cree en ti 💛", "Respira, repasa los fallos y vuelve a darle."),
            m("Esto es solo la primera vuelta 🌀", "Haz un repaso de lo fallado y verás cómo sube la nota."),
            m("Un paso atrás para coger carrerilla 🏃", "Repasa la teoría de este tema y repite el test.")
        ),
        ResultTier.GAME_PERFECT to listOf(
            m("¡Todas las rondas ganadas! 🏆", "Cinco de cinco. Reflejos de piloto."),
            m("¡Partida perfecta! 🎯", "Ni un fallo en todo el juego. Increíble."),
            m("¡Imparable! 🔥", "Has arrasado con las cinco rondas."),
            m("¡Qué ojo! 👀", "Cero fallos. Esto sí que es saberse el reglamento."),
            m("¡Pleno! 💯", "Cinco aciertos de cinco. Bali aplaude."),
            m("¡Máxima puntuación! ⭐", "Así se juega: rápido y sin equivocarse."),
            m("¡Nivel experto! 😎", "Ni un solo fallo. ¿Repetimos para ver si se puede otra vez?"),
            m("¡Redondo! ✨", "Una partida de manual."),
            m("¡Pura precisión! 🎯", "Todas bien. Se nota que lo tienes aprendido."),
            m("¡Campeón de la carretera! 🛣️", "Ronda tras ronda, sin fallar ninguna."),
            m("¡Sobresaliente! 🎓", "Has demostrado que dominas esto."),
            m("¡Así da gusto jugar! 🙌", "Perfecto de principio a fin.")
        ),
        ResultTier.GAME_HIGH to listOf(
            m("¡Muy buena partida! 🔥", "Casi todas bien. A por el pleno en la siguiente."),
            m("¡Buen ojo! 👀", "Solo una se te escapó. ¿Juegas otra y la clavas?"),
            m("¡Qué buen nivel! 💪", "Lo tienes bastante dominado, solo falta afinar."),
            m("¡Casi pleno! 🎯", "Una ronda de nada. La revancha te espera."),
            m("¡Vas muy fino! ⭐", "Buen resultado. Cada partida te deja más cerca de no fallar ninguna."),
            m("¡Gran partida! 😄", "Más aciertos que fallos de largo. Bien jugado."),
            m("¡Reflejos de conductor! 🚗", "Respuestas rápidas y casi todas correctas."),
            m("¡Eso es saber! 🧠", "Se nota que has estudiado. Repite y busca el pleno."),
            m("¡Cerquita de la perfección! ✨", "Un pequeño fallo no empaña una partida así."),
            m("¡Muy bien jugado! 👏", "Te falta muy poco para las cinco de cinco."),
            m("¡Sólido! 🧱", "Partida de las buenas. Bali está contento."),
            m("¡Vas lanzado! 🚀", "Juega otra vez mientras estás en racha.")
        ),
        ResultTier.GAME_MID to listOf(
            m("¡No ha ido mal! 🙂", "Has ganado más de la mitad. Juega otra y busca una ronda más."),
            m("Buen intento 💡", "Juega otra vez: la segunda partida suele salir mejor."),
            m("Vas cogiendo el truco 🧩", "Con un par de partidas más le pillas el punto. Juega otra."),
            m("A medio camino 🌓", "Algunas rondas fueron tuyas. Repite para ganar el resto."),
            m("Esto mejora jugando 🎮", "Juega una partida más y notarás la diferencia."),
            m("Casi, casi 🎯", "Las que fallaste son las que más aprenderás. Repite y gánalas."),
            m("Con calma 🚦", "Es un juego: equivócate aquí para acertar luego. Juega otra partida."),
            m("Sigue practicando 📈", "Repite: es la forma más rápida de mejorar."),
            m("Un poquito más 💪", "Estás en la zona media. Repite y empuja hasta la zona alta."),
            m("Se está calentando el motor 🔧", "Otra partida y verás cómo suben los aciertos."),
            m("Ni tan mal, ni tan bien 🙃", "Hay base. Repite y afina las respuestas."),
            m("Vamos a por la revancha 🔁", "Bali te espera para otra partida.")
        ),
        ResultTier.GAME_LOW to listOf(
            m("Esta vez no ha salido 🌱", "Es un juego: sin presión. Vuelve a jugar y mejora."),
            m("No te rindas 🧠", "Con cada partida se aprende algo nuevo. Juega otra."),
            m("Primera vuelta, de calentamiento 🔥", "La segunda siempre va mejor. Repite."),
            m("A todos nos pasa 🫶", "Respira y vuelve a intentarlo cuando quieras."),
            m("Hoy ha costado ☁️", "Repasa el tema en una lección y juega otra vez."),
            m("Esto se aprende jugando 🎮", "Cada ronda fallada te enseña algo. Juega otra vez."),
            m("Aún no lo tienes, pero llegará 💛", "Un poco de teoría y otra partida bastan para empezar a subir."),
            m("Empieza el entrenamiento 🏃", "Nadie gana a la primera. Pruébalo de nuevo."),
            m("Un tropiezo no es una caída 🚦", "Juega otra vez y verás cómo mejora."),
            m("Bali sigue contigo 🐾", "Repasa lo que no te sonaba y vuelve a darle."),
            m("Ánimo, que se puede 💪", "Esto es práctica pura. Repite y ve subiendo."),
            m("Vamos a por otra 🔁", "La siguiente partida es una oportunidad nueva.")
        ),
        ResultTier.EXAM_PERFECT to listOf(
            m("¡Simulacro perfecto! 🏆", "30 de 30. Ni un solo fallo en un examen completo."),
            m("¡Pleno en el simulacro! 🎯", "Sin fallos, de principio a fin. Con este resultado aprobarías el teórico."),
            m("¡Qué examen! 🔥", "Cero fallos. Pocos simulacros salen así."),
            m("¡Perfecto de verdad! ✨", "Las 30 bien. Bali no se lo cree."),
            m("¡Un examen de diez! 💯", "Treinta preguntas, treinta aciertos."),
            m("¡Sin una sola falta! 🙌", "Con este resultado aprobarías el teórico de sobra."),
            m("¡Nivel máximo! 🚀", "Un simulacro impecable. Apúntalo, es tu mejor marca posible."),
            m("¡Inmejorable! 🌟", "No se puede hacer mejor. Enhorabuena."),
            m("¡Examen redondo! 🎓", "Tres fallos permitidos y no has usado ninguno."),
            m("¡Has bordado el simulacro! 🪡", "Todo acertado. Orgullo de mascota."),
            m("¡Matrícula de honor! 🏅", "Un simulacro así demuestra mucho trabajo."),
            m("¡Cero fallos, cero dudas! 😎", "Con este resultado aprobarías el teórico, y con nota.")
        ),
        ResultTier.EXAM_PASSED to listOf(
            m("¡Simulacro aprobado! 🎉", "Dentro del margen de fallos de la DGT. Con este resultado aprobarías el teórico."),
            m("¡Examen superado! ✅", "Has estado dentro de los tres fallos permitidos. Bien hecho."),
            m("¡Lo has conseguido! 🙌", "Así se aprueba un simulacro. Repasa los fallos y afina."),
            m("¡Enhorabuena! 🥳", "Con este resultado aprobarías el teórico. Ahora afina los fallos."),
            m("¡Aprobado con todas las de la ley! 📜", "Mira los fallos que tuviste: son los que te separan del pleno."),
            m("¡Gran simulacro! 💪", "Has cumplido de sobra. Un repaso a los fallos y a seguir."),
            m("¡Examen en el bolsillo! 🎒", "Dentro del margen. Mantén el ritmo de estudio."),
            m("¡Muy bien! 👏", "Con este resultado aprobarías el teórico. Repite para asentarlo."),
            m("¡Has pasado el corte! ✂️", "Tres fallos o menos: lo que pide la DGT, cumplido."),
            m("¡Buenísimo! 🌟", "Un simulacro aprobado es un gran paso. Sigue practicando."),
            m("¡A este ritmo, vas fenomenal! 🚗", "Aprobar un simulacro cuesta, y tú lo has hecho."),
            m("¡Así se hace! 😄", "Ahora repasa los fallos para que no se repitan.")
        ),
        ResultTier.EXAM_CLOSE to listOf(
            m("Muy cerca del aprobado 🎯", "Te han faltado pocas preguntas. Repasa los fallos y repite el simulacro."),
            m("Casi lo tienes 💪", "Estás a muy poco del margen de la DGT. Revisa dónde fallaste."),
            m("Por poco, pero no ha sido 🌓", "Estás a un repaso de entrar en el margen. Revisa los fallos."),
            m("Has rozado el aprobado 🤏", "Haz un test de los temas donde más fallaste y vuelve a intentarlo."),
            m("Estás casi ahí 🚦", "Pocas preguntas de diferencia. Con un buen repaso, se arregla."),
            m("Un último empujón 🏁", "Repasa los fallos de este examen y haz otro simulacro."),
            m("Muy buen punto de partida 🌱", "Estás cerca. Revisa los fallos de hoy: te dicen qué estudiar."),
            m("No ha sido por mucho 🙂", "Repasa lo fallado, que es poco, y vuelve a probar."),
            m("Se te ha escapado por poco 😮‍💨", "Respira. Un par de repasos y ese margen se cierra."),
            m("Estás en la buena dirección 🧭", "Revisa los temas de tus fallos y refuérzalos antes del siguiente."),
            m("A un paso de aprobar 👣", "Pregúntale al Chat lo que no te quedó claro y repite."),
            m("Este simulacro te enseña mucho 📖", "Revisa los fallos uno a uno. Son tu mejor guía.")
        ),
        ResultTier.EXAM_FAILED to listOf(
            m("Hoy no ha sido, pero sirve 🧠", "Revisa los fallos: te dicen justo qué temas reforzar."),
            m("Simulacro suspendido, aprendizaje ganado 📖", "Repasa los fallos y haz tests del tema que más te cuesta."),
            m("Aún falta un trecho 🛣️", "Normal: un examen completo es mucho. Repasa y vuelve a intentarlo."),
            m("Esto es para lo que sirve un simulacro 🚦", "Fallar aquí te enseña a no fallar en el examen de verdad. Revisa los fallos."),
            m("Toca repasar 🔁", "Empieza por los temas donde más fallaste y repite el simulacro después."),
            m("No es el resultado que querías 🫶", "Pero ya sabes qué reforzar. Haz una lección de tu tema más flojo."),
            m("Hay trabajo por delante 🛠️", "Un buen plan: una lección al día de tus temas flojos."),
            m("Con calma y a repasar 🌱", "Mira las explicaciones de los fallos antes del siguiente intento."),
            m("Todavía no estás en el margen ⏳", "Con práctica constante se llega. Haz un test al día de tus temas flojos."),
            m("Un simulacro no define nada 💛", "Es una foto de hoy. Repasa y saca otra mañana."),
            m("A seguir practicando 🚗", "Repasa los fallos y pregúntale al Chat lo que no entiendas."),
            m("Esto se va a mover 📈", "Cada repaso acerca la nota al margen. Vuelve a por otro simulacro.")
        ),
        ResultTier.EXAM_FAR to listOf(
            m("Primer paso dado 🌱", "El simulacro enseña dónde estás. Empieza por las lecciones de los temas más flojos."),
            m("No te desanimes 💛", "Un examen completo es duro. Ve lección a lección y vuelve cuando quieras."),
            m("Aún queda camino 🛣️", "Mejor saberlo ahora que el día del examen. Repasa con calma."),
            m("Hoy ha costado mucho ☁️", "Sin prisa: haz lecciones sueltas y vuelve al simulacro más adelante."),
            m("Esto es el punto de partida 🏁", "Toda mejora empieza aquí. Mira los fallos y elige un tema."),
            m("El teórico se aprende 📖", "La nota sube con práctica diaria, aunque sea poca. Haz una lección hoy."),
            m("Respira, esto es práctica 🫶", "Un simulacro así no cuenta para nada fuera de la app. Haz una lección y vuelve."),
            m("A empezar por lo básico 🧩", "Haz las lecciones del camino y repasa los fallos de hoy."),
            m("No pasa nada 🙂", "Vuelve con una lección al día y verás cómo cambia."),
            m("Mejor ahora que en el examen 🚦", "Para eso está el simulacro. Anota los temas donde más fallaste."),
            m("Bali te acompaña 🐾", "Vamos paso a paso: un tema, un test, un repaso."),
            m("El camino empieza con un test 🌱", "Haz una lección corta ahora y deja el simulacro para otro día.")
        )
    )

    /** Extra messages mixed in when the test was answered quickly; only for the tiers that suit them. */
    internal val fast: Map<ResultTier, List<ResultMessage>> = mapOf(
        ResultTier.LESSON_PERFECT to listOf(
            m("¡Perfecto y a toda velocidad! ⚡", "Sin fallos y en un suspiro. Menuda combinación."),
            m("¡Rápido y sin fallar! 🏎️", "Esto es saber de verdad: ni dudas ni errores."),
            m("¡Fulminante! 💥", "Todo bien y en tiempo récord.")
        ),
        ResultTier.LESSON_EXCELLENT to listOf(
            m("¡Rápido y casi perfecto! ⚡", "Volando, y con un fallo de nada."),
            m("¡Qué velocidad! 🏎️", "Casi sin errores y sin pestañear."),
            m("¡Con prisa pero con cabeza! 🧠", "Rapidez y acierto: así da gusto.")
        ),
        ResultTier.LESSON_GOOD to listOf(
            m("¡Rapidito! ⚡", "Lo has hecho bien y en poco tiempo."),
            m("¡Qué ritmo! 🏎️", "Buen resultado, y sin pensártelo demasiado."),
            m("¡Con soltura! 😎", "Se nota que conoces el tema: contestas rápido y bien.")
        ),
        ResultTier.GAME_PERFECT to listOf(
            m("¡Reflejos de rayo! ⚡", "Cinco de cinco y a toda velocidad."),
            m("¡Rapidez y puntería! 🎯", "Ni un fallo y sin perder un segundo.")
        ),
        ResultTier.GAME_HIGH to listOf(
            m("¡Qué reflejos! ⚡", "Rápido y casi perfecto."),
            m("¡A toda pastilla! 🏎️", "Buen ritmo y buenos resultados.")
        ),
        ResultTier.EXAM_PERFECT to listOf(
            m("¡Perfecto y con tiempo de sobra! ⏱️", "Un simulacro sin fallos y rapidísimo."),
            m("¡Qué manera de volar! 🚀", "Treinta de treinta sin agobios.")
        ),
        ResultTier.EXAM_PASSED to listOf(
            m("¡Aprobado y sin agobios! ⏱️", "Has terminado con tiempo de sobra. Eso da mucha tranquilidad."),
            m("¡Rápido y dentro del margen! ⚡", "Buen ritmo: justo lo que se necesita en el examen.")
        )
    )
}
