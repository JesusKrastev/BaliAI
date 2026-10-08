package com.jesuskrastev.bali.ui.screens.onboarding

/**
 * One question of the onboarding mini-test.
 *
 * The questions are written here rather than drawn from the lesson bank because this is the screen
 * that sells the app: each one is hand-picked for its trap, has a longer explanation than the bank
 * (the rule, the trap and a way to remember it) and was checked against the Reglamento General de
 * Circulación. They are text-only on purpose: images load from the network, and the first launch
 * may have none.
 *
 * @property id stable identifier reported to analytics, independent of the wording
 * @property text the question, worded the way the DGT asks
 * @property options the three answers, as in the real exam
 * @property correctIndex position of the right answer in [options]
 * @property topic short name of the subject, shown on the result and in the plan
 * @property sectionIndex the learning-path section that teaches it, so the plan can flag that week
 * @property explanation shown after answering: the rule, the trap and a trick to remember it
 */
data class QuizQuestion(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val topic: String,
    val sectionIndex: Int,
    val explanation: String
)

/**
 * The answer the user gave to one question of the mini-test.
 *
 * @property questionId [QuizQuestion.id] of the question answered
 * @property selectedIndex the option tapped
 * @property isCorrect whether it was the right one
 */
data class QuizAnswer(
    val questionId: String,
    val selectedIndex: Int,
    val isCorrect: Boolean
)

/**
 * The onboarding mini-test: three questions chosen by what the user fears most about the exam, so
 * the first contact with the product tests exactly that.
 */
object OnboardingQuiz {

    /** Questions in each run, as many as the result screen and the plan are designed for. */
    const val QUESTION_COUNT = 3

    private val stopAlways = QuizQuestion(
        id = "stop_always",
        text = "Llegas a una señal de STOP y no viene ningún vehículo. ¿Qué debes hacer?",
        options = listOf(
            "Detenerme por completo antes de seguir",
            "Reducir la velocidad y pasar sin parar",
            "Parar solo si hay una línea de detención pintada"
        ),
        correctIndex = 0,
        topic = "STOP",
        sectionIndex = SECTION_PRIORITY,
        explanation = "En un STOP hay que detenerse siempre, venga alguien o no. Es justo lo que " +
            "lo diferencia del ceda el paso, donde solo paras si hace falta. " +
            "Truco: STOP = parar siempre; ceda = parar si viene alguien."
    )

    private val alcoholNovice = QuizQuestion(
        id = "alcohol_novice",
        text = "¿Cuál es la tasa máxima de alcohol en sangre para un conductor novel?",
        options = listOf("0,5 gramos por litro", "0,3 gramos por litro", "0,8 gramos por litro"),
        correctIndex = 1,
        topic = "Alcohol",
        sectionIndex = SECTION_DRIVER,
        explanation = "Los noveles (los dos primeros años de carné) y los profesionales tienen un " +
            "límite más bajo: 0,3 g/l en sangre (0,15 mg/l en aire). El 0,5 es el general, y es " +
            "la trampa típica. Truco: novel = 0,3."
    )

    private val urbanSingleLane = QuizQuestion(
        id = "urban_single_lane",
        text = "¿Cuál es la velocidad máxima en una vía urbana con un único carril por sentido?",
        options = listOf("50 km/h", "40 km/h", "30 km/h"),
        correctIndex = 2,
        topic = "Velocidad",
        sectionIndex = SECTION_SPEED,
        explanation = "Desde mayo de 2021, en calles con un solo carril por sentido el límite es " +
            "30 km/h. El 50 se queda para las vías con dos o más carriles por sentido. " +
            "Truco: un carril, 30; dos o más, 50."
    )

    private val officerOverLight = QuizQuestion(
        id = "officer_over_light",
        text = "El semáforo está en rojo, pero un agente de tráfico te indica que avances. ¿Qué haces?",
        options = listOf(
            "Espero a que el semáforo se ponga en verde",
            "Avanzo, obedeciendo al agente",
            "Me detengo y le pito para avisarle"
        ),
        correctIndex = 1,
        topic = "Agentes",
        sectionIndex = SECTION_SIGNS,
        explanation = "Las indicaciones de los agentes mandan sobre todo lo demás. El orden es: " +
            "agentes, señales circunstanciales (como las de obras), semáforos, señales verticales " +
            "y marcas viales. Truco: la persona manda sobre la máquina."
    )

    private val flashingAmber = QuizQuestion(
        id = "flashing_amber_signs",
        text = "En un cruce, el semáforo está en ámbar intermitente y en tu calle hay una señal de " +
            "STOP. ¿Qué debes hacer?",
        options = listOf(
            "Detenerme en el STOP",
            "Pasar con precaución, sin parar",
            "Ceder el paso solo a quien venga por la derecha"
        ),
        correctIndex = 0,
        topic = "Semáforos",
        sectionIndex = SECTION_LIGHTS,
        explanation = "El ámbar intermitente no da paso: solo pide precaución. Mientras parpadea, " +
            "mandan las señales del cruce, así que si hay un STOP, hay que parar. " +
            "Truco: ámbar que parpadea, mira las señales."
    )

    private val widerRoad = QuizQuestion(
        id = "wider_road",
        text = "En un cruce sin señalizar, ¿tiene prioridad quien circula por la vía más ancha?",
        options = listOf(
            "Sí, la vía más ancha siempre tiene preferencia",
            "Sí, pero solo fuera de poblado",
            "No, cedes el paso a quien se acerca por tu derecha"
        ),
        correctIndex = 2,
        topic = "Prioridad",
        sectionIndex = SECTION_PRIORITY,
        explanation = "Sin señales, la anchura no da prioridad: se cede el paso a quien viene por " +
            "la derecha. Las excepciones son otras, como la vía pavimentada frente a un camino de " +
            "tierra o quien ya circula dentro de una glorieta. Truco: sin señales, mira a tu derecha."
    )

    private val rightPriority = QuizQuestion(
        id = "right_priority",
        text = "En un cruce sin ninguna señal, ¿a quién debes ceder el paso?",
        options = listOf(
            "A quien se acerca por mi izquierda",
            "A quien se acerca por mi derecha",
            "Al vehículo más grande"
        ),
        correctIndex = 1,
        topic = "Prioridad",
        sectionIndex = SECTION_PRIORITY,
        explanation = "En los cruces sin señalizar se cede el paso a quien viene por tu derecha. " +
            "Tiene excepciones: dentro de una glorieta pasa primero quien ya circula por ella, y la " +
            "vía pavimentada va antes que el camino de tierra. Truco: sin señales, mira a tu derecha."
    )

    /**
     * Picks the three questions for the user's main worry: traps for "detalles tontos", rules that
     * collide for "que el examen no se parezca", and an easy-to-hard level check otherwise.
     *
     * @param concern one of [OnboardingConfig.concerns], or null when unanswered
     * @return the questions to ask, in order
     */
    fun questionsFor(concern: String?): List<QuizQuestion> = when (concern) {
        OnboardingConfig.CONCERN_SILLY_MISTAKES -> listOf(stopAlways, alcoholNovice, urbanSingleLane)
        OnboardingConfig.CONCERN_EXAM_MISMATCH -> listOf(officerOverLight, flashingAmber, widerRoad)
        else -> listOf(rightPriority, urbanSingleLane, flashingAmber)
    }

    /**
     * Topics of the questions answered wrong, without repeats and in the order they were asked.
     *
     * @param concern the worry that chose the questions
     * @param answers the answers given
     * @return the topics to reinforce
     */
    fun failedTopics(concern: String?, answers: List<QuizAnswer>): List<String> =
        failedQuestions(concern, answers).map { it.topic }.distinct()

    /**
     * Questions answered wrong, in the order they were asked.
     *
     * @param concern the worry that chose the questions
     * @param answers the answers given
     * @return the questions to reinforce
     */
    fun failedQuestions(concern: String?, answers: List<QuizAnswer>): List<QuizQuestion> {
        val wrong = answers.filterNot { it.isCorrect }.map { it.questionId }.toSet()
        return questionsFor(concern).filter { it.id in wrong }
    }
}

// Learning-path sections (DgtLearningPathTemplate) that teach each topic of the mini-test.
private const val SECTION_DRIVER = 0
private const val SECTION_SIGNS = 2
private const val SECTION_LIGHTS = 3
private const val SECTION_SPEED = 4
private const val SECTION_PRIORITY = 5
