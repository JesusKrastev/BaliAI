package com.jesuskrastev.bali.domain.path

import com.jesuskrastev.bali.domain.model.AILessonNode
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.model.NodeType
import java.util.UUID

/**
 * Template pedagógico completo para el examen teórico de la DGT española.
 * Contiene ~70 nodos organizados en 9 secciones con lecciones, repasos y exámenes.
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

    fun buildInitialPath(): List<AILessonNode> {
        val defs = buildNodeDefinitions()
        return defs.mapIndexed { index, def ->
            AILessonNode(
                id = UUID.randomUUID().toString(),
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
                "Factores que afectan a la conducción" to "Descubre qué factores influyen al volante.",
                "Alcohol al volante: efectos y límites" to "Conoce los límites legales de alcohol.",
                "Drogas, medicamentos y conducción" to "Entiende cómo afectan las sustancias.",
                "Fatiga y somnolencia" to "Aprende a reconocer la fatiga al conducir.",
                "Distracciones: móvil, GPS, pasajeros" to "Evita las distracciones más comunes.",
                "Cinturón de seguridad y airbag" to "Uso correcto de los sistemas de seguridad.",
                "Sistemas de Retención Infantil (SRI)" to "Protege a los más pequeños en el coche."
            ),
            reviewTitle = "Repaso del Conductor",
            reviewDesc = "Repasa todo lo aprendido sobre el conductor.",
            examTitle = "Examen — El Conductor",
            examDesc = "Demuestra lo que sabes sobre el conductor."
        )

        // ── SECCIÓN 2 — El Vehículo ──
        all += section(
            sectionIndex = 1,
            sectionTitle = "El Vehículo",
            lessons = listOf(
                "Documentación obligatoria del vehículo" to "Conoce los papeles imprescindibles.",
                "Partes del vehículo y funcionamiento" to "Domina los componentes del coche.",
                "Neumáticos: presión, desgaste y norma" to "Cuida tus neumáticos correctamente.",
                "Luces del vehículo: tipos y uso" to "Aprende cuándo usar cada luz.",
                "Frenos: ABS, EBD y distancias" to "Entiende los sistemas de frenado.",
                "Sistemas de asistencia (ADAS)" to "Conoce las ayudas electrónicas.",
                "Mantenimiento preventivo y averías" to "Previene problemas mecánicos.",
                "Carga del vehículo y remolques" to "Aprende a cargar de forma legal y segura."
            ),
            reviewTitle = "Repaso del Vehículo",
            reviewDesc = "Repasa todo lo aprendido sobre el vehículo.",
            examTitle = "Examen — El Vehículo",
            examDesc = "Demuestra lo que sabes sobre el vehículo."
        )

        // ── SECCIÓN 3 — Señales de Tráfico I ──
        all += section(
            sectionIndex = 2,
            sectionTitle = "Señales de Tráfico I",
            lessons = listOf(
                "Prelación entre señales" to "¿Qué señal manda sobre otra?",
                "Señales de peligro (triángulos)" to "Reconoce las señales de advertencia.",
                "Señales de prohibición de entrada" to "Aprende dónde no puedes entrar.",
                "Señales de prohibición y restricción" to "Domina las limitaciones en la vía.",
                "Señales de obligación (círculos azules)" to "Conoce lo que estás obligado a hacer.",
                "Señales de fin de prohibición" to "Identifica cuándo termina una restricción."
            ),
            reviewTitle = "Repaso Señales I",
            reviewDesc = "Repasa las señales de tráfico básicas.",
            examTitle = "Examen — Señales I",
            examDesc = "Demuestra tu dominio de señales básicas."
        )

        // ── SECCIÓN 4 — Señales de Tráfico II ──
        all += section(
            sectionIndex = 3,
            sectionTitle = "Señales de Tráfico II",
            lessons = listOf(
                "Señales de indicación: información" to "Conoce las señales informativas.",
                "Señales de indicación: carriles" to "Gestiona carriles y autopistas.",
                "Marcas viales longitudinales" to "Aprende qué significan las líneas.",
                "Marcas viales transversales y otras" to "Domina el resto de marcas viales.",
                "Semáforos: tipos y significado" to "Interpreta cada tipo de semáforo.",
                "Señales de los agentes" to "Entiende las señales manuales.",
                "Señales circunstanciales y de obras" to "Actúa ante señales temporales."
            ),
            reviewTitle = "Repaso Señales II",
            reviewDesc = "Repasa las señales de tráfico avanzadas.",
            examTitle = "Examen — Señales II",
            examDesc = "Demuestra tu dominio de señales avanzadas."
        )

        // ── SECCIÓN 5 — Normas de Circulación I ──
        all += section(
            sectionIndex = 4,
            sectionTitle = "Normas de Circulación I",
            lessons = listOf(
                "Velocidades máximas por tipo de vía" to "Conoce los límites en cada vía.",
                "Velocidades mínimas y marcha lenta" to "Aprende cuándo ir demasiado lento es peligroso.",
                "Distancia de seguridad" to "Mantén la distancia correcta siempre.",
                "Uso correcto de los carriles" to "Circula por el carril adecuado.",
                "Cambio de carril y señalización" to "Cambia de carril de forma segura.",
                "Incorporación a la circulación" to "Incorpórate sin poner en riesgo a nadie.",
                "Circulación en autopistas y autovías" to "Domina las vías de alta capacidad."
            ),
            reviewTitle = "Repaso Normas I",
            reviewDesc = "Repasa las normas de circulación básicas.",
            examTitle = "Examen — Normas I",
            examDesc = "Demuestra tu dominio de normas básicas."
        )

        // ── SECCIÓN 6 — Normas de Circulación II ──
        all += section(
            sectionIndex = 5,
            sectionTitle = "Normas de Circulación II",
            lessons = listOf(
                "Prioridad de paso en intersecciones" to "Aprende quién pasa primero.",
                "Glorietas y rotondas" to "Circula correctamente en rotondas.",
                "Adelantamiento: cuándo y cómo" to "Adelanta de forma segura y legal.",
                "Prohibiciones de adelantamiento" to "Conoce cuándo no puedes adelantar.",
                "Parada y estacionamiento" to "Aparca cumpliendo la normativa.",
                "Zonas de estacionamiento regulado" to "ORA, disco horario y zonas azules.",
                "Giros y cambio de sentido" to "Gira a izquierda, derecha y sentido contrario.",
                "Marcha atrás: normativa y límites" to "Conoce cuándo y cómo dar marcha atrás."
            ),
            reviewTitle = "Repaso Normas II",
            reviewDesc = "Repasa las normas de circulación avanzadas.",
            examTitle = "Examen — Normas II",
            examDesc = "Demuestra tu dominio de normas avanzadas."
        )

        // ── SECCIÓN 7 — Vías y Medio Ambiente ──
        all += section(
            sectionIndex = 6,
            sectionTitle = "Vías y Medio Ambiente",
            lessons = listOf(
                "Tipos de vías y características" to "Conoce cada tipo de vía española.",
                "Condiciones meteorológicas adversas" to "Conduce seguro con lluvia, nieve o niebla.",
                "Conducción nocturna" to "Domina la conducción de noche.",
                "Conducción eficiente y eco-driving" to "Ahorra combustible y cuida el planeta.",
                "Emisiones y zonas de bajas emisiones" to "Entiende las ZBE y la normativa.",
                "Accidentes: auxilios y conducta" to "Actúa correctamente ante un accidente.",
                "Triángulos de emergencia y seguridad" to "Señaliza correctamente una avería."
            ),
            reviewTitle = "Repaso Vías y Medio Ambiente",
            reviewDesc = "Repasa todo sobre vías y medio ambiente.",
            examTitle = "Examen — Vías y Medio Ambiente",
            examDesc = "Demuestra lo que sabes sobre vías y entorno."
        )

        // ── SECCIÓN 8 — Otros Usuarios y Casos Especiales ──
        all += section(
            sectionIndex = 7,
            sectionTitle = "Otros Usuarios y Casos Especiales",
            lessons = listOf(
                "Peatones: cruce, acera y calzada" to "Respeta a los peatones en la vía.",
                "Ciclistas y VMP en la vía" to "Convive con bicicletas y patinetes.",
                "Motocicletas y ciclomotores" to "Comparte vía con las motos.",
                "Vehículos de emergencia y especiales" to "Cede el paso a emergencias.",
                "Transporte escolar y de viajeros" to "Normas con autobuses escolares.",
                "Mercancías peligrosas (básico)" to "Conoce las reglas para mercancías peligrosas."
            ),
            reviewTitle = "Repaso Casos Especiales",
            reviewDesc = "Repasa los casos especiales de circulación.",
            examTitle = "Examen — Casos Especiales",
            examDesc = "Demuestra tu dominio de casos especiales."
        )

        // ── SECCIÓN 9 — Simulacros Finales ──
        val simSection = 8
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
