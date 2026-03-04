package com.jesuskrastev.bali.domain.path

import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.model.NodeType

/**
 * Template pedagógico completo para el examen teórico de la DGT española.
 * Contiene ~102 nodos organizados en 10 secciones con lecciones, repasos y exámenes.
 */
object DgtLearningPathTemplate {

    private data class NodeDef(
        val title: String,
        val description: String,
        val sectionIndex: Int,
        val sectionTitle: String,
        val unitIndex: Int,
        val nodeType: NodeType,
        val iconResName: String
    )

    fun buildInitialPath(): List<LessonNode> {
        val defs = buildNodeDefinitions()
        return defs.mapIndexed { index, def ->
            LessonNode(
                id = "node_${def.sectionIndex}_${def.unitIndex}_${def.nodeType.name.lowercase()}",
                orderIndex = index,
                title = def.title,
                description = def.description,
                status = if (index == 0) NodeStatus.UNLOCKED else NodeStatus.LOCKED,
                scorePercentage = null,
                sectionIndex = def.sectionIndex,
                sectionTitle = def.sectionTitle,
                unitIndex = def.unitIndex,
                nodeType = def.nodeType,
                iconResName = def.iconResName
            )
        }
    }

    private fun buildNodeDefinitions(): List<NodeDef> {
        val all = mutableListOf<NodeDef>()

        // ── SECCIÓN 1 — El Conductor ──
        all += section(
            sectionIndex = 0,
            sectionTitle = "El Conductor",
            lessons = listOf(
                "Factores físicos y psíquicos" to "Descubre qué te afecta al volante.",
                "Alcohol: efectos, límites y sanciones" to "Conoce los límites legales y sus consecuencias.",
                "Drogas, medicamentos y conducción" to "Entiende cómo afectan las sustancias.",
                "Fatiga, somnolencia y microsueños" to "Aprende a reconocer la fatiga.",
                "Distracciones: móvil, GPS y pasajeros" to "Evita las distracciones más comunes.",
                "Cinturón, airbag y cabecero" to "Uso correcto de los sistemas de seguridad.",
                "Sistemas de Retención Infantil (SRI)" to "Protege a los más pequeños."
            ),
            reviewTitle = "Repaso — El Conductor",
            reviewDesc = "Repasa todo sobre el conductor.",
            examTitle = "Examen — El Conductor",
            examDesc = "Demuestra lo que sabes."
        )

        // ── SECCIÓN 2 — El Vehículo ──
        all += section(
            sectionIndex = 1,
            sectionTitle = "El Vehículo",
            lessons = listOf(
                "Documentación obligatoria" to "Conoce los papeles imprescindibles.",
                "Partes externas del vehículo" to "Carrocería y acristalamientos.",
                "Motor, transmisión y tracción" to "Domina los componentes mecánicos.",
                "Neumáticos: presión y desgaste" to "Cuida tus neumáticos correctamente.",
                "Sistema de iluminación" to "Aprende cuándo usar cada luz.",
                "Frenos: ABS, EBD y distancias" to "Entiende los sistemas de frenado.",
                "Sistemas ADAS" to "Conoce las ayudas electrónicas.",
                "Mantenimiento preventivo e ITV" to "Previene problemas mecánicos.",
                "Carga, remolques y masas máximas" to "Carga de forma legal y segura."
            ),
            reviewTitle = "Repaso — El Vehículo",
            reviewDesc = "Repasa todo lo aprendido sobre el vehículo.",
            examTitle = "Examen — El Vehículo",
            examDesc = "Demuestra lo que sabes sobre el vehículo."
        )

        // ── SECCIÓN 3 — Señales de Tráfico I: Peligro y Prohibición ──
        all += section(
            sectionIndex = 2,
            sectionTitle = "Señales I: Peligro y Prohibición",
            lessons = listOf(
                "Prelación entre señales" to "¿Qué señal manda sobre otra?",
                "Señales de peligro (triángulos)" to "Reconoce las señales de advertencia.",
                "Señales de prohibición de entrada" to "Aprende dónde no puedes entrar.",
                "Señales de prohibición: velocidad" to "Domina los límites y restricciones.",
                "Señales de restricción: peso y altura" to "Limitaciones por dimensiones.",
                "Señales de fin de prohibición" to "Cuándo termina una restricción.",
                "Señales de obligación (azules)" to "Conoce lo que estás obligado a hacer."
            ),
            reviewTitle = "Repaso — Señales I",
            reviewDesc = "Repasa las señales de peligro y prohibición.",
            examTitle = "Examen — Señales I",
            examDesc = "Demuestra tu dominio de señales básicas."
        )

        // ── SECCIÓN 4 — Señales de Tráfico II: Indicación y Marcas ──
        all += section(
            sectionIndex = 3,
            sectionTitle = "Señales II: Indicación y Marcas",
            lessons = listOf(
                "Señales de indicación general" to "Conoce las señales informativas.",
                "Señales de autopistas y autovías" to "Gestiona vías de alta capacidad.",
                "Señales de carriles y servicios" to "Carriles especiales y servicios.",
                "Marcas viales longitudinales" to "Aprende qué significan las líneas.",
                "Marcas viales transversales" to "STOP, ceda el paso, paso peatones.",
                "Otras marcas: flechas y badenes" to "Flechas, chevrones, resaltos.",
                "Semáforos: tipos y fases" to "Interpreta cada tipo de semáforo.",
                "Señales de los agentes" to "Entiende los gestos y posiciones."
            ),
            reviewTitle = "Repaso — Señales II",
            reviewDesc = "Repasa las señales de indicación y marcas.",
            examTitle = "Examen — Señales II",
            examDesc = "Demuestra tu dominio de señales avanzadas."
        )

        // ── SECCIÓN 5 — Normas de Circulación I: Velocidad y Carriles ──
        all += section(
            sectionIndex = 4,
            sectionTitle = "Normas I: Velocidad y Carriles",
            lessons = listOf(
                "Velocidades máximas por tipo de vía" to "Conoce los límites en cada vía.",
                "Velocidades específicas: novatos, lluvia" to "Casos especiales de velocidad.",
                "Velocidades mínimas y circulación" to "Cuándo ir lento es peligroso.",
                "Distancia de seguridad" to "Mantén la distancia correcta.",
                "Carriles en vías de varios sentidos" to "Circula por el carril adecuado.",
                "Carriles en vías de un solo sentido" to "Uso correcto de vías unidireccionales.",
                "Cambio de carril y señalización" to "Cambia de carril de forma segura.",
                "Incorporación a la circulación" to "Incorpórate sin riesgo."
            ),
            reviewTitle = "Repaso — Normas I",
            reviewDesc = "Repasa velocidad y carriles.",
            examTitle = "Examen — Normas I",
            examDesc = "Demuestra tu dominio de normas básicas."
        )

        // ── SECCIÓN 6 — Normas de Circulación II: Prioridad y Maniobras ──
        all += section(
            sectionIndex = 5,
            sectionTitle = "Normas II: Prioridad y Maniobras",
            lessons = listOf(
                "Prioridad en cruces sin señalizar" to "Aprende quién pasa primero.",
                "Prioridad en cruces señalizados" to "Intersecciones con señales.",
                "Glorietas y rotondas" to "Entrada, circulación y salida.",
                "Adelantamiento: cuándo y cómo" to "Adelanta de forma segura.",
                "Prohibiciones de adelantamiento" to "Cuándo no puedes adelantar.",
                "Parada: lugares y prohibiciones" to "Dónde puedes y no puedes parar.",
                "Estacionamiento: zonas reguladas" to "ORA, disco horario y zonas.",
                "Giros y cambio de sentido" to "Gira a derecha, izquierda y sentido contrario.",
                "Marcha atrás: normativa" to "Cuándo y cómo dar marcha atrás."
            ),
            reviewTitle = "Repaso — Normas II",
            reviewDesc = "Repasa prioridad y maniobras.",
            examTitle = "Examen — Normas II",
            examDesc = "Demuestra tu dominio de normas avanzadas."
        )

        // ── SECCIÓN 7 — Vías, Entorno y Conducción Especial ──
        all += section(
            sectionIndex = 6,
            sectionTitle = "Vías, Entorno y Conducción Especial",
            lessons = listOf(
                "Tipos de vía y características" to "Conoce cada tipo de vía española.",
                "Condiciones meteorológicas adversas" to "Lluvia, nieve, niebla y viento.",
                "Conducción nocturna" to "Domina la conducción de noche.",
                "Túneles y pasos subterráneos" to "Conducción segura en túneles.",
                "Conducción eficiente y eco-driving" to "Ahorra combustible.",
                "Emisiones, etiquetas DGT y ZBE" to "Entiende las zonas de bajas emisiones.",
                "Accidentes: conducta y PAS" to "Actúa correctamente ante un accidente.",
                "Señalización de emergencia" to "Triángulos, chaleco y balizas."
            ),
            reviewTitle = "Repaso — Vías y Entorno",
            reviewDesc = "Repasa vías y conducción especial.",
            examTitle = "Examen — Vías y Entorno",
            examDesc = "Demuestra lo que sabes sobre vías y entorno."
        )

        // ── SECCIÓN 8 — Otros Usuarios de la Vía ──
        all += section(
            sectionIndex = 7,
            sectionTitle = "Otros Usuarios de la Vía",
            lessons = listOf(
                "Peatones: derechos y cruces" to "Respeta a los peatones en la vía.",
                "Ciclistas: normas y carril bici" to "Convive con bicicletas.",
                "VMP: patinetes y similares" to "Normas para movilidad personal.",
                "Motocicletas y ciclomotores" to "Comparte vía con las motos.",
                "Vehículos de emergencia" to "Cede el paso a emergencias.",
                "Transporte escolar y de viajeros" to "Normas con autobuses escolares.",
                "Mercancías peligrosas" to "Paneles, etiquetas y distancias."
            ),
            reviewTitle = "Repaso — Otros Usuarios",
            reviewDesc = "Repasa otros usuarios de la vía.",
            examTitle = "Examen — Otros Usuarios",
            examDesc = "Demuestra tu dominio sobre otros usuarios."
        )

        // ── SECCIÓN 9 — Infracciones, Sanciones y Responsabilidad ──
        all += section(
            sectionIndex = 8,
            sectionTitle = "Infracciones y Sanciones",
            lessons = listOf(
                "Clasificación de infracciones" to "Leves, graves y muy graves.",
                "Sanciones: multas y retirada" to "Consecuencias de las infracciones.",
                "Pérdida y recuperación de puntos" to "Sistema de puntos del carné.",
                "Seguro obligatorio: tipos" to "Coberturas del seguro.",
                "Responsabilidad civil y penal" to "Responsabilidad del conductor.",
                "Alcoholemia: pruebas y consecuencias" to "Pruebas, negativa y penas."
            ),
            reviewTitle = "Repaso — Infracciones",
            reviewDesc = "Repasa infracciones y sanciones.",
            examTitle = "Examen — Infracciones",
            examDesc = "Demuestra lo que sabes sobre sanciones."
        )

        // ── SECCIÓN 10 — Simulacros Finales DGT ──
        val simSection = 9
        val simTitle = "Simulacros Finales"
        for (i in 1..5) {
            all += NodeDef(
                title = "Simulacro DGT #$i",
                description = "Examen oficial de 30 preguntas — Simulacro $i.",
                sectionIndex = simSection,
                sectionTitle = simTitle,
                unitIndex = i - 1,
                nodeType = NodeType.EXAM,
                iconResName = "lesson_exam"
            )
        }

        return all
    }

    private fun section(
        sectionIndex: Int,
        sectionTitle: String,
        lessons: List<Pair<String, String>>,
        reviewTitle: String,
        reviewDesc: String,
        examTitle: String,
        examDesc: String
    ): List<NodeDef> {
        val nodes = mutableListOf<NodeDef>()
        var unitIdx = 0

        for ((title, desc) in lessons) {
            nodes += NodeDef(
                title = title,
                description = desc,
                sectionIndex = sectionIndex,
                sectionTitle = sectionTitle,
                unitIndex = unitIdx++,
                nodeType = NodeType.LESSON,
                iconResName = "lesson_test"
            )
        }

        nodes += NodeDef(
            title = reviewTitle,
            description = reviewDesc,
            sectionIndex = sectionIndex,
            sectionTitle = sectionTitle,
            unitIndex = unitIdx++,
            nodeType = NodeType.REVIEW,
            iconResName = "lesson_review"
        )

        nodes += NodeDef(
            title = examTitle,
            description = examDesc,
            sectionIndex = sectionIndex,
            sectionTitle = sectionTitle,
            unitIndex = unitIdx,
            nodeType = NodeType.EXAM,
            iconResName = "lesson_exam"
        )

        return nodes
    }
}
