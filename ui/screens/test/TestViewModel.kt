package com.jesuskrastev.bali.ui.screens.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.jesuskrastev.bali.data.local.room.dao.AnswerDao
import com.jesuskrastev.bali.data.local.room.dao.TestResultDao
import com.jesuskrastev.bali.data.local.room.entities.AnswerEntity
import com.jesuskrastev.bali.data.local.room.entities.TestResultEntity
import com.jesuskrastev.bali.data.repository.AnswerRepositoryImpl
import com.jesuskrastev.bali.domain.model.Answer
import com.jesuskrastev.bali.domain.model.TestMode
import com.jesuskrastev.bali.domain.usecase.DecrementEnergyUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementCoinsUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementStreakUseCase
import com.jesuskrastev.bali.domain.usecase.IncrementXpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

data class QuestionUiState(
    val text: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String,
    val imageUrl: String? = null
)

data class TestUiState(
    val category: String = "",
    val questions: List<QuestionUiState> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val isAnswerChecked: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val sessionStreak: Int = 0
)

data class TestSummary(
    val score: Int,
    val total: Int,
    val xpGained: Int,
    val durationSeconds: Int,
    val accuracy: Int,
    val baseXp: Int,
    val bonusPerfection: Int?,
    val bonusFast: Int?,
    val bonusStreak: Int?,
    val leveledUp: Boolean,
    val coinsGained: Int
)

@HiltViewModel
class TestViewModel @Inject constructor(
    private val testResultDao: TestResultDao,
    private val answerDao: AnswerRepositoryImpl,
    private val gemini: GenerativeModel,
    private val decrementEnergyUseCase: DecrementEnergyUseCase,
    private val incrementStreakUseCase: IncrementStreakUseCase,
    private val incrementXpUseCase: IncrementXpUseCase,
    private val incrementCoinsUseCase: IncrementCoinsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TestUiState())
    val uiState: StateFlow<TestUiState> = _uiState.asStateFlow()
    private var startTime: Long = 0
    private var sessionStreak: Int = 0

    private val jsonContent = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
        allowTrailingCommas = true
    }

    init {
        generateTest()
    }

    fun onEvent(event: TestEvent) {
        when (event) {
            TestEvent.Retry -> retry()
            is TestEvent.SelectOption -> selectOption(event.optionIndex)
            TestEvent.CheckAnswer -> checkAnswer()
            TestEvent.NextQuestion -> nextQuestion()
            is TestEvent.FinishTest -> {
                viewModelScope.launch {
                    val result = calculateResult()
                    event.onResult(result)
                }
            }
        }
    }

    private fun retry() {
        sessionStreak = 0
        _uiState.update { it.copy(isLoading = true, error = null, questions = emptyList(), currentQuestionIndex = 0, selectedAnswers = emptyMap(), isAnswerChecked = false, sessionStreak = 0) }
        generateTest()
    }

    private fun generateTest() {
        viewModelScope.launch {
            try {
                val lastTests = testResultDao.getRecent().first()
                
                val historyContext = if (lastTests.isNotEmpty()) {
                    "Resultados recientes: " + 
                    lastTests.joinToString(", ") { "${it.category}: ${it.score}/${it.total}" }
                } else {
                    "Sin tests previos."
                }

                val prompt = """
                    Eres un Profesor Experto de la DGT (Dirección General de Tráfico) en España. 
                    Tu misión es generar una SESIÓN DE PRÁCTICA de 10 preguntas.

                    1. SELECCIÓN DE CATEGORÍA:
                    Analiza el historial: $historyContext.
                    ELIGE UNA categoría de esta lista (Prioriza las que NO se han practicado recientemente):
                    - Alumbrado y señalización óptica.
                    - Prioridad de paso en intersecciones.
                    - Maniobras (incorporación, giros, adelantamientos, paradas).
                    - Velocidad (límites, distancias de frenado).
                    - El conductor (alcohol, drogas, fatiga, distracciones).
                    - Mecánica básica y mantenimiento.
                    - Documentación, seguro e inspecciones técnica (ITV).
                    - Usuarios de la vía (peatones, ciclistas, animales).
                    - Señales de advertencia de peligro (solo si no se ha hecho señales recientemente).
                    - Marcas viales y señales horizontales.

                    2. REGLAS DE LA PREGUNTA:
                    - Estilo DGT oficial: Preguntas directas, a veces con situaciones hipotéticas.
                    - 3 opciones por pregunta. Solo una es correcta.
                    - Las respuestas incorrectas deben ser creíbles.
                    - EXPLICACIÓN: Máximo 20 palabras. Debe ser clara y lógica.

                    3. REGLAS DE IMÁGENES (SISTEMA FILEPATH):
                    - Usa imágenes SOLO si la pregunta describe una situación visual o una señal física.
                    - Formato obligatorio: https://commons.wikimedia.org/wiki/Special:FilePath/Spain_traffic_signal_[codigo].svg
                    - Codigos ejemplo: r1 (ceda), r2 (stop), p1 (peligro), r301 (velocidad 40), s1 (autopista).
                    - Si la pregunta es puramente teórica (ej: tasa de alcohol), usa null.

                    4. FORMATO DE RESPUESTA (JSON PURO):
                    {
                      "selectedCategory": "Nombre exacto de la categoría elegida",
                      "questions": [
                        {
                          "text": "¿Pregunta?",
                          "options": ["Opción 1", "Opción 2", "Opción 3"],
                          "correctAnswerIndex": 0,
                          "explanation": "Breve explicación lógica...",
                          "imageUrl": "URL_O_NULL"
                        }
                      ]
                    }
                    Responde SOLO el JSON.
                """.trimIndent()

                val response = gemini.generateContent(prompt)
                val rawText = response.text ?: throw Exception("Sin respuesta")
                
                val jsonStartIndex = rawText.indexOf('{')
                val jsonEndIndex = rawText.lastIndexOf('}')
                if (jsonStartIndex == -1 || jsonEndIndex == -1) throw Exception("Formato inválido")
                
                val jsonString = rawText.substring(jsonStartIndex, jsonEndIndex + 1)
                val root = jsonContent.parseToJsonElement(jsonString).jsonObject
                
                val category = root["selectedCategory"]?.jsonPrimitive?.content ?: "Práctica General"
                val questionUiStates = root["questions"]?.jsonArray?.map { element ->
                    val obj = element.jsonObject
                    QuestionUiState(
                        text = obj["text"]?.jsonPrimitive?.content ?: "",
                        options = obj["options"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                        correctAnswerIndex = obj["correctAnswerIndex"]?.jsonPrimitive?.content?.toInt() ?: 0,
                        explanation = obj["explanation"]?.jsonPrimitive?.content ?: "",
                        imageUrl = obj["imageUrl"]?.jsonPrimitive?.content.takeIf { it != "null" && it != null && it.startsWith("http") }
                    )
                } ?: emptyList()
                
                startTime = System.currentTimeMillis()
                _uiState.update { it.copy(category = category, questions = questionUiStates, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    private fun selectOption(optionIndex: Int) {
        if (_uiState.value.isAnswerChecked) return
        _uiState.update { 
            val newAnswers = it.selectedAnswers.toMutableMap()
            newAnswers[it.currentQuestionIndex] = optionIndex
            it.copy(selectedAnswers = newAnswers)
        }
    }

    private fun checkAnswer() {
        val currentState = _uiState.value
        val selectedOption = currentState.selectedAnswers[currentState.currentQuestionIndex]
        if (selectedOption != null) {
            val isCorrect = selectedOption == currentState.questions[currentState.currentQuestionIndex].correctAnswerIndex
            if (isCorrect) sessionStreak++ else sessionStreak = 0
            _uiState.update { it.copy(isAnswerChecked = true, sessionStreak = sessionStreak) }
        }
    }

    private fun nextQuestion() {
        val currentState = _uiState.value
        if (currentState.currentQuestionIndex < currentState.questions.size - 1) {
            _uiState.update { it.copy(currentQuestionIndex = it.currentQuestionIndex + 1, isAnswerChecked = false) }
        }
    }

    private suspend fun calculateResult(): TestSummary {
        val state = _uiState.value
        val correct = state.questions.indices.count { index ->
            state.selectedAnswers[index] == state.questions[index].correctAnswerIndex
        }
        val durationSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        val accuracy = if (state.questions.isNotEmpty()) ((correct.toFloat() / state.questions.size) * 100).toInt() else 0
        
        val xpEarned = incrementXpUseCase(
            mode = TestMode.PRACTICE,
            correctAnswers = correct,
            totalQuestions = state.questions.size,
            durationSeconds = durationSeconds
        )

        val coinsGained = incrementCoinsUseCase()
        
        val testId = testResultDao.insert(
            TestResultEntity(
                category = state.category,
                score = correct,
                total = state.questions.size,
                isPassed = accuracy >= 90
            )
        )

        state.questions.forEachIndexed { index, question ->
            val selectedOption = state.selectedAnswers[index]
            if (selectedOption != null) {
                answerDao.insert(
                    Answer(
                        testId = testId,
                        questionText = question.text,
                        selectedOption = selectedOption,
                        isCorrect = selectedOption == question.correctAnswerIndex
                    )
                )
            }
        }

        decrementEnergyUseCase()
        incrementStreakUseCase()

        return TestSummary(
            score = correct,
            total = state.questions.size,
            xpGained = xpEarned.xpGained,
            durationSeconds = durationSeconds,
            accuracy = accuracy,
            baseXp = xpEarned.baseXp,
            bonusPerfection = xpEarned.bonusPerfection,
            bonusFast = xpEarned.bonusFast,
            bonusStreak = xpEarned.bonusStreak,
            leveledUp = xpEarned.levelUp,
            coinsGained = coinsGained
        )
    }
}
