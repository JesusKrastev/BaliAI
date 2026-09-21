package com.jesuskrastev.bali.domain.usecase

import com.google.firebase.ai.GenerativeModel
import com.jesuskrastev.bali.di.PathNodesModel
import com.jesuskrastev.bali.domain.repository.UserRepository
import com.jesuskrastev.bali.domain.model.LessonNode
import com.jesuskrastev.bali.domain.model.NodeStatus
import com.jesuskrastev.bali.domain.repository.PathRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import javax.inject.Inject

/**
 * Use case responsible for dynamically generating the next segment of the learning path
 * using the Gemini AI model.
 * 
 * It analyzes the user's current level and reported difficulties to create
 * a personalized curriculum of lesson nodes, which are then saved via [PathRepository].
 */
open class GenerateNextPathNodesUseCase @Inject constructor(
    @PathNodesModel private val gemini: GenerativeModel,
    private val userRepository: UserRepository,
    private val pathRepository: PathRepository,
) {
    private val jsonContent = Json { ignoreUnknownKeys = true }

    open suspend operator fun invoke(count: Int = 5): List<LessonNode> {
        val user = userRepository.get().first() ?: throw Exception("Usuario no encontrado")
        val userId = user.id

        // Gather context
        val studentLevel = user.level
        val difficultTopics = user.difficultTopics.takeIf { it.isNotBlank() } ?: "Ninguno"
        
        // Find where we are currently at in the path
        val lastOrderIndex = pathRepository.getLastUnlockedNodeOrder(userId)
        val startOrderIndex = lastOrderIndex + 1

        val prompt = """
            Eres un Sistema Experto de Creación de Curriculums para la DGT en España.
            Tu misión es generar los próximos $count Nodos (Lecciones) para un alumno.
            
            CONTEXTO DEL ALUMNO:
            - Nivel actual: $studentLevel
            - Temas más difíciles reportados: $difficultTopics
            
            REGLAS:
            - Estos nodos deben ser progresivos. Si su nivel es bajo (1-3), enfócate en lo básico (Documentación, Señales, Velocidad). Si es alto, mete temas más complejos.
            - Intercala temas donde tiene más fallos.
            - Los títulos deben ser MUY cortos (ej: "Prioridades", "Velocidad", "Señales de Peligro").
            - Las descripciones deben motivar y explicar brevemente de qué tratará (max 10-15 palabras).
            
            Devuelve exactamente $count nodos.
        """.trimIndent()

        val response = gemini.generateContent(prompt)
        val rawText = response.text ?: throw Exception("Sin respuesta de Gemini")

        val jsonStartIndex = rawText.indexOf('{')
        val jsonEndIndex = rawText.lastIndexOf('}')
        if (jsonStartIndex == -1 || jsonEndIndex == -1) throw Exception("Formato JSON inválido desde AI")

        val jsonString = rawText.substring(jsonStartIndex, jsonEndIndex + 1)
        val root = jsonContent.parseToJsonElement(jsonString).jsonObject
        
        val nodesArray = root["nodes"]?.jsonArray ?: throw Exception("No se encontró el array nodes")

        val generatedNodes = nodesArray.mapIndexed { index, element ->
            val obj = element.jsonObject
            LessonNode(
                id = UUID.randomUUID().toString(),
                orderIndex = startOrderIndex + index,
                title = obj["title"]?.jsonPrimitive?.content ?: "Práctica DGT",
                description = obj["description"]?.jsonPrimitive?.content ?: "Sesión generada automáticamente",
                status = if (index == 0) NodeStatus.UNLOCKED else NodeStatus.LOCKED,
                scorePercentage = null
            )
        }

        // Save generated nodes to DB
        pathRepository.saveGeneratedNodes(userId, generatedNodes)

        return generatedNodes
    }
}
