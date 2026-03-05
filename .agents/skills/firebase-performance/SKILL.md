---
name: bali-firebase-performance
description: >
  Use ALWAYS when working on the Bali Android app (Kotlin + Jetpack Compose + Firebase).
  Enforces Firebase Performance monitoring, correct Coroutine dispatchers, Firestore best
  practices, Compose recomposition safety, and known issues specific to this codebase.
  Trigger on any new feature, bug fix, refactor, or ViewModel/Repository/Composable change.
allowed-tools:
  - Read
  - Write
  - Edit
  - Bash
  - Grep
  - Glob
---

# Bali App — Estándares de Código y Performance

Stack: **Kotlin · Jetpack Compose · Firebase (Auth + Firestore + Crashlytics + Performance) · Hilt · Room · Gemini AI**

---

## 🔥 FIREBASE PERFORMANCE — OBLIGATORIO

### SDK ya instalado (verificar en build.gradle.kts)

```kotlin
// plugins
id("com.google.firebase.firebase-perf")

// dependencies
implementation(libs.firebase.perf)
```

Si no está, añádelo antes de continuar.

### Trazar operaciones críticas con @AddTrace

Usa `@AddTrace` en cualquier función suspendida que realice trabajo significativo:

```kotlin
import com.google.firebase.perf.metrics.AddTrace

// ✅ Trazar generación de examen con Gemini
@AddTrace(name = "gemini_generate_exam")
private suspend fun generateExam() { ... }

// ✅ Trazar migración de Firestore
@AddTrace(name = "firestore_migration")
suspend fun executeMigrations(userId: String) { ... }

// ✅ Trazar carga del learning path
@AddTrace(name = "load_learning_path")
fun getPathNodes(userId: String): Flow<List<LessonNode>> { ... }
```

### Trazas manuales para operaciones con Flow o bloques complejos

```kotlin
import com.google.firebase.perf.FirebasePerformance

// Para bloques que no son una sola función
val trace = FirebasePerformance.getInstance().newTrace("upload_all_data")
trace.start()
try {
    firestoreUserDao.uploadAll(...)
} finally {
    trace.stop()
}

// Para la generación de path con Gemini (mide tiempo real de IA)
val trace = FirebasePerformance.getInstance().newTrace("gemini_generate_path")
trace.putAttribute("node_count", count.toString())
trace.start()
try {
    generateNextPathNodesUseCase(count)
} finally {
    trace.stop()
}
```

### Atributos personalizados en trazas

Añade contexto útil para filtrar en la consola:

```kotlin
trace.putAttribute("user_level", user.level.toString())
trace.putAttribute("license_type", user.licenseType)
trace.putAttribute("is_logged_in", (userId != null).toString())
trace.putMetric("questions_count", questions.size.toLong())
```

### Activar en BaliApplication (ya debe estar o añadir)

```kotlin
class BaliApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Solo en debug para no interferir con métricas reales
        if (BuildConfig.DEBUG) {
            FirebasePerformance.getInstance().isPerformanceCollectionEnabled = true
        }
    }
}
```

---

## ⚙️ COROUTINES — REGLAS DE DISPATCHERS

### Regla de oro: NUNCA lanzar trabajo de red/disco en Main

```kotlin
// ❌ PROHIBIDO en este proyecto — Gemini y Firestore en Main
viewModelScope.launch {
    gemini.generateContent(prompt) // red → bloquea UI
    firestoreDao.uploadAll(...)    // red → bloquea UI
}

// ✅ CORRECTO
viewModelScope.launch(Dispatchers.IO) {
    val response = gemini.generateContent(prompt)
    withContext(Dispatchers.Main) {
        _uiState.update { it.copy(questions = parse(response)) }
    }
}
```

### Mapa de dispatchers para este proyecto

| Operación | Dispatcher correcto |
|---|---|
| Gemini `generateContent()` | `Dispatchers.IO` |
| Firestore `.get().await()` | `Dispatchers.IO` |
| Firestore `snapshots()` Flow | No necesita (ya es async) |
| Room DAO queries | `Dispatchers.IO` |
| DataStore reads/writes | `Dispatchers.IO` |
| `_uiState.update {}` | `Dispatchers.Main` (default) |
| Cálculos de XP/streak | `Dispatchers.Default` |
| `calculateResult()` en ExamViewModel | ✅ Ya usa `withContext(Dispatchers.IO)` — mantener |

### Patrón correcto para ViewModels de este proyecto

```kotlin
// Patrón estándar Bali ViewModel
viewModelScope.launch(Dispatchers.IO) {
    try {
        val result = repository.someOperation()
        _uiState.update { it.copy(data = result, isLoading = false) }
    } catch (e: Exception) {
        _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
        FirebaseCrashlytics.getInstance().recordException(e)
    }
}
```

---

## 🏗️ FIRESTORE — PATRONES OBLIGATORIOS

### Listeners: SIEMPRE con awaitClose (ya correcto en Auth — replicar en todo)

```kotlin
// ✅ PATRÓN BALI para cualquier snapshot listener nuevo
fun observeSomething(userId: String): Flow<Something> = callbackFlow {
    val registration = firestore.collection("...")
        .document(userId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            snapshot?.toObject(SomethingFirestore::class.java)?.let { trySend(it) }
        }
    awaitClose { registration.remove() } // ← NUNCA omitir
}
```

### Writes: usa siempre SetOptions.merge() para campos parciales

```kotlin
// ❌ Sobreescribe campos que no enviaste
docRef.set(data).await()

// ✅ Solo actualiza los campos presentes
docRef.set(data, SetOptions.merge()).await()
// o para campos específicos:
docRef.update(mapOf("field" to value)).await()
```

### Batches: respetar el límite de 500 ops (ya implementado en FirestoreUserDao)

```kotlin
// ✅ Patrón ya existente — mantener en nuevos DAOs
operations.chunked(BATCH_LIMIT).forEach { chunk ->
    val batch = firestore.batch()
    chunk.forEach { it(batch) }
    batch.commit().await()
}
```

### Error handling en todas las operaciones suspend de Firestore

```kotlin
// ✅ Toda operación Firestore debe tener try/catch
suspend fun updateUser(userId: String, user: User) {
    try {
        collection.document(userId).set(user.toFirestore(), SetOptions.merge()).await()
    } catch (e: FirebaseFirestoreException) {
        FirebaseCrashlytics.getInstance().recordException(e)
        throw e // re-throw para que el ViewModel lo maneje
    }
}
```

---

## 🎨 COMPOSE — REGLAS DE RECOMPOSICIÓN

### items() SIEMPRE con key

```kotlin
// ❌ PROHIBIDO — recompone toda la lista
items(list) { item -> ItemRow(item) }

// ✅ OBLIGATORIO en este proyecto
items(list, key = { it.id }) { item -> ItemRow(item) }

// Para índices (ExamScreen grid):
items(questionsCount, key = { it }) { index -> QuestionCell(index) }

// Para strings únicos (TopicsScreen, OnboardingScreen):
items(options, key = { it.title }) { option -> OptionCard(option) }
```

### LaunchedEffect para cargas iniciales

```kotlin
// ❌ PROHIBIDO — se ejecuta en cada recomposición
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    viewModel.loadSomething() // ← ejecutado en CADA recomposición
}

// ✅ CORRECTO
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    LaunchedEffect(Unit) {
        viewModel.loadSomething()
    }
}
```

### collectAsStateWithLifecycle (no collectAsState) para Flows

```kotlin
// ❌ No respeta el lifecycle — sigue activo en background
val uiState by viewModel.uiState.collectAsState()

// ✅ Se cancela cuando la pantalla va a background
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

### derivedStateOf para cálculos sobre estado

```kotlin
// ❌ Recalcula en cada recomposición
val completedNodes = uiState.pathNodes.filter { it.status == "COMPLETED" }

// ✅ Solo recalcula cuando pathNodes cambia
val completedNodes by remember(uiState.pathNodes) {
    derivedStateOf { uiState.pathNodes.filter { it.status == "COMPLETED" } }
}
```

---

## 🔐 AUTENTICACIÓN — REGLAS ESPECÍFICAS DE BALI

### NUNCA acceder a `currentUser` directo fuera del repo de Auth

```kotlin
// ❌ PROHIBIDO en ViewModels o UseCases
val uid = FirebaseAuth.getInstance().currentUser?.uid

// ✅ SIEMPRE a través del AuthRepository inyectado
val uid = authRepository.currentUser()
// o reactivo:
authRepository.currentUserFlow.collect { uid -> ... }
```

### El patrón `withAuthRouting` ya existe en UserRepositoryImpl — replicarlo en nuevos repos

```kotlin
private suspend inline fun <T> withAuthRouting(
    actionRemote: suspend (String) -> T,
    actionLocal: suspend () -> T
): T {
    val userId = authRepository.currentUser()
    return if (userId != null) actionRemote(userId) else actionLocal()
}
```

### Exponer datos de usuario como Flow, no como suspend fun

```kotlin
// ❌ Causa el bug actual en HomeViewModel (suspend en combine)
suspend fun currentUserPhotoUrl(): String?

// ✅ Como Flow para poder combinarlo correctamente
val currentUserPhotoFlow: Flow<String?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener {
        trySend(it.currentUser?.photoUrl?.toString())
    }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
}
```

---

## 📦 BUILD — CHECKLIST OBLIGATORIA

### Antes de cualquier release, verificar:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true       // ✅ obligatorio
        isShrinkResources = true     // ✅ obligatorio
        // ❌ signingConfig = signingConfigs.getByName("debug") ← NUNCA en release
        signingConfig = signingConfigs.getByName("release") // ✅
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### Reglas ProGuard para las libs de Bali (proguard-rules.pro)

```proguard
# Firebase Firestore entities — no ofuscar los data class de Firestore
-keep class com.jesuskrastev.bali.data.remote.firestore.entities.** { *; }
-keep class com.jesuskrastev.bali.data.local.room.entities.** { *; }

# Gemini / Generative AI
-keep class com.google.ai.client.generativeai.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
```

---

## 🏥 CHECKLIST ANTES DE HACER COMMIT

Antes de dar por terminado cualquier cambio en Bali, verifica:

- [ ] ¿Toda llamada a Gemini o Firestore está en `Dispatchers.IO`?
- [ ] ¿Cualquier `addSnapshotListener` nuevo tiene `awaitClose { registration.remove() }`?
- [ ] ¿Los nuevos `LazyColumn`/`LazyRow` usan `key =` en sus `items()`?
- [ ] ¿Las nuevas operaciones críticas tienen traza de Firebase Performance (`@AddTrace` o manual)?
- [ ] ¿Los nuevos campos de `AuthRepository` que se usan en `combine()` son `Flow`, no `suspend fun`?
- [ ] ¿El build de release usa `signingConfig` de release, no de debug?
- [ ] ¿Los errores de Firestore se reportan con `FirebaseCrashlytics.recordException(e)`?
- [ ] ¿Los nuevos `StateFlow` se exponen con `.asStateFlow()`?
- [ ] ¿Se usa `collectAsStateWithLifecycle()` en lugar de `collectAsState()` en Composables?
