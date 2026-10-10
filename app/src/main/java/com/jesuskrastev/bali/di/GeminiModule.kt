package com.jesuskrastev.bali.di

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.jesuskrastev.bali.data.repository.GeminiTutorRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/** The model tuned for the AI tutor chat: plain prose, short answers. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TutorModel

/** The model that generates test questions as schema-constrained JSON. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class QuestionsModel

/** The model that generates learning-path nodes as schema-constrained JSON. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PathNodesModel

/**
 * Wires the Gemini models through Firebase AI Logic.
 *
 * Nothing here carries an API key. Firebase AI Logic proxies the request through Google's
 * backend using credentials that live in the Firebase project, which is the whole point of
 * the move: the previous SDK needed `BuildConfig.GEMINI_API_KEY`, and a `BuildConfig`
 * string is a plain constant sitting in the shipped APK for anyone to extract. App Check
 * (set up in `BaliApplication`) is what stops a third party from calling that proxy from
 * outside the app.
 */
@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {

    /**
     * The Gemini Developer API backend — the same service the app already used, minus the
     * embedded key. Vertex AI is the alternative and would need its own GCP billing setup.
     *
     * @return the Firebase AI entry point for the default app.
     */
    private fun ai() = Firebase.ai(backend = GenerativeBackend.googleAI())

    /**
     * The model behind the tutor chat: a hard output cap, and the tutor's rules pinned as
     * a system instruction so they travel as a constant prefix instead of being rebuilt
     * into the prompt body on every turn.
     *
     * @return a shared, stateless [GenerativeModel] for [GeminiTutorRepository].
     */
    @Provides
    @Singleton
    @TutorModel
    fun provideTutorGemini(): GenerativeModel =
        ai().generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.4f
                topP = 0.9f
                maxOutputTokens = TUTOR_MAX_OUTPUT_TOKENS
            },
            systemInstruction = content { text(GeminiTutorRepository.SYSTEM_INSTRUCTION) }
        )

    /**
     * The model behind the practice, exam and mistake-review screens. Their response shape
     * is enforced by [questionsSchema] rather than asked for in prose, so the reply is
     * always parseable JSON — a malformed answer used to cost a full-price generation that
     * produced nothing and sent the student straight to the retry button.
     *
     * @return a shared, stateless [GenerativeModel] answering with a questions object.
     */
    @Provides
    @Singleton
    @QuestionsModel
    fun provideQuestionsGemini(): GenerativeModel =
        ai().generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = JSON_MIME_TYPE
                responseSchema = questionsSchema()
                maxOutputTokens = QUESTIONS_MAX_OUTPUT_TOKENS
            }
        )

    /**
     * The model that extends the learning path. Same JSON guarantee, far smaller budget:
     * a handful of titles and one-line descriptions.
     *
     * @return a shared, stateless [GenerativeModel] answering with a nodes object.
     */
    @Provides
    @Singleton
    @PathNodesModel
    fun providePathNodesGemini(): GenerativeModel =
        ai().generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = JSON_MIME_TYPE
                responseSchema = pathNodesSchema()
                maxOutputTokens = PATH_NODES_MAX_OUTPUT_TOKENS
            }
        )

    /**
     * Describes the questions payload the three test screens parse.
     *
     * `imageUrl` and `selectedCategory` are optional: most questions are purely
     * theoretical, and only the practice screen picks a category of its own.
     *
     * @return the response schema shared by the practice, exam and review screens.
     */
    private fun questionsSchema(): Schema {
        val question = Schema.obj(
            properties = mapOf(
                "text" to Schema.string("Enunciado de la pregunta"),
                "options" to Schema.array(
                    items = Schema.string("Texto real de una respuesta"),
                    description = "Las tres opciones",
                    minItems = OPTIONS_PER_QUESTION,
                    maxItems = OPTIONS_PER_QUESTION
                ),
                "correctAnswerIndex" to Schema.integer(
                    description = "Posicion (0, 1 o 2) de la opcion correcta dentro de options",
                    minimum = 0.0,
                    maximum = (OPTIONS_PER_QUESTION - 1).toDouble()
                ),
                "explanation" to Schema.string("Justificacion breve de la norma"),
                "imageUrl" to Schema.string(
                    description = "URL de la senal ilustrada, ausente si la pregunta es teorica",
                    nullable = true
                ),
                "sourceIndex" to Schema.integer(
                    description = "Numero de la pregunta del material de estudio de la que parte, solo en repasos",
                    nullable = true
                )
            ),
            optionalProperties = listOf("imageUrl", "sourceIndex"),
            description = "Una pregunta tipo test al estilo del examen oficial de la DGT"
        )

        return Schema.obj(
            properties = mapOf(
                "questions" to Schema.array(items = question, description = "Las preguntas generadas"),
                "selectedCategory" to Schema.string(
                    description = "Categoria elegida para la sesion, solo en practica libre",
                    nullable = true
                )
            ),
            optionalProperties = listOf("selectedCategory"),
            description = "El conjunto de preguntas generadas para la sesion"
        )
    }

    /**
     * Describes the nodes payload the learning path parses.
     *
     * @return the response schema for generated lesson nodes.
     */
    private fun pathNodesSchema(): Schema =
        Schema.obj(
            properties = mapOf(
                "nodes" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "title" to Schema.string("Titulo muy corto, dos o tres palabras"),
                            "description" to Schema.string("Descripcion motivadora de 10 a 15 palabras")
                        ),
                        description = "Una leccion del itinerario de aprendizaje"
                    ),
                    description = "Los nodos generados"
                )
            ),
            description = "Los nodos generados para el itinerario"
        )

    /**
     * The lite model does no thinking, so a call is billed for its answer alone. Measured
     * on a 30-question exam, `gemini-2.5-flash` burned 12.236 thinking tokens on top of its
     * answer — some 70% of the cost. Should this ever move to a model that reasons, cap
     * it with `thinkingConfig`, available from firebase-ai 17.x onwards.
     */
    private const val MODEL_NAME = "gemini-3.1-flash-lite"

    private const val JSON_MIME_TYPE = "application/json"

    /** The DGT exam always offers three answers. */
    private const val OPTIONS_PER_QUESTION = 3

    /**
     * The tutor's system instruction asks for ~120 words, which in Spanish runs close to
     * 200 tokens. This leaves headroom for the "más detalle" exception without leaving
     * the ceiling loose enough to let a single reply run away in cost.
     */
    private const val TUTOR_MAX_OUTPUT_TOKENS = 300

    /** A measured 30-question exam lands near 4.100 tokens; this is the circuit breaker. */
    private const val QUESTIONS_MAX_OUTPUT_TOKENS = 8_000

    /** Five short titles plus one-line descriptions. */
    private const val PATH_NODES_MAX_OUTPUT_TOKENS = 1_200
}
