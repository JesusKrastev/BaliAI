package com.jesuskrastev.bali.domain.usecase

import com.google.ai.client.generativeai.GenerativeModel
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
    private val gemini: GenerativeModel,
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
            Eres un Sistema Experto de CreaciÃ³n de Curriculums para la DGT en EspaÃ±a.
            Tu misiÃ³n es generar los prÃ³ximos $count Nodos (Lecciones) para un alumno.
            
            CONTEXTO DEL ALUMNO:
            - Nivel actual: $studentLevel
            - Temas mÃ¡s difÃ­ciles reportados: $difficultTopics
            
            REGLAS:
            - Estos nodos deben ser progresivos. Si su nivel es bajo (1-3), enfÃ³cate en lo bÃ¡sico (DocumentaciÃ³n, SeÃ±ales, Velocidad). Si es alto, mete temas mÃ¡s complejos.
            - Intercala temas donde tiene mÃ¡s fallos.
            - Los tÃ­tulos deben ser MUY cortos (ej: "Prioridades", "Velocidad", "SeÃ±ales de Peligro").
            - Las descripciones deben motivar y explicar brevemente de quÃ© tratarÃ¡ (max 10-15 palabras).
            
            FORMATO ESPERADO (JSON PURO):
            {
              "nodes": [
                {
                  "title": "TÃ­tulo Corto",
                  "description": "DescripciÃ³n breve y motivadora"
                }
              ]
            }
            Devuelve SOLO EL JSON y asegÃºrate de parsearlo bien.
        """.trimIndent()

        val response = gemini.generateContent(prompt)
        val rawText = response.text ?: throw Exception("Sin respuesta de Gemini")

        val jsonStartIndex = rawText.indexOf('{')
        val jsonEndIndex = rawText.lastIndexOf('}')
        if (jsonStartIndex == -1 || jsonEndIndex == -1) throw Exception("Formato JSON invÃ¡lido desde AI")

        val jsonString = rawText.substring(jsonStartIndex, jsonEndIndex + 1)
        val root = jsonContent.parseToJsonElement(jsonString).jsonObject
        
        val nodesArray = root["nodes"]?.jsonArray ?: throw Exception("No se encontrÃ³ el array nodes")

        val generatedNodes = nodesArray.mapIndexed { index, element ->
            val obj = element.jsonObject
            LessonNode(
                id = UUID.randomUUID().toString(),
                orderIndex = startOrderIndex + index,
                title = obj["title"]?.jsonPrimitive?.content ?: "PrÃ¡ctica DGT",
                description = obj["description"]?.jsonPrimitive?.content ?: "SesiÃ³n generada automÃ¡ticamente",
                status = if (index == 0) NodeStatus.UNLOCKED else NodeStatus.LOCKED,
                scorePercentage = null
            )
        }

        // Save generated nodes to DB
        pathRepository.saveGeneratedNodes(userId, generatedNodes)

        return generatedNodes
    }
}
