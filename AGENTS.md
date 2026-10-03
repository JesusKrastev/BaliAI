# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

Bali AI is a native Android app for Spanish DGT driving exam preparation, featuring AI-powered explanations (Gemini), gamification (XP, streaks, coins), and cloud sync via Firebase.

## Architecture

Clean Architecture + MVVM. Single-module app, no product flavors (hard paywall — every paid user gets full functionality).

```
app/src/
└── main/java/com/jesuskrastev/bali/
    ├── domain/         # Use cases, domain models, repository interfaces — no Android/framework deps
    ├── data/           # Repository impls, Room DAOs, Firestore DAOs, mappers
    │   ├── local/room/ # Room entities, DAOs, BaliDatabase, Converters
    │   ├── remote/     # Firestore DAOs, FCM service
    │   ├── repository/ # Concrete repository implementations
    │   └── mapper/     # Domain ↔ Entity ↔ Firestore mappers (extension functions)
    ├── di/             # Hilt modules — all @InstallIn(SingletonComponent::class)
    └── ui/
        ├── screens/    # One folder per screen: Screen.kt + ViewModel.kt + Fake*.kt
        ├── components/ # Reusable Composables
        ├── navigation/ # NavHost + @Serializable route objects
        └── theme/      # Material3 theme
```

Data flow: `UI → ViewModel → UseCase → Repository interface → (Room | Firestore)`.  
Repositories transparently sync: local Room for offline, Firestore when authenticated.

## Tech Stack

- **Language**: Kotlin 2.0.21, JVM target 11
- **UI**: Jetpack Compose (BOM 2024.09.00), Material3, Compose Navigation 2.8.9
- **DI**: Hilt 2.52 with KSP (not KAPT)
- **Local DB**: Room 2.6.1 (DB version 10, `exportSchema = false`)
- **Preferences**: DataStore 1.1.2
- **Backend**: Firebase BOM 33.13.0 (Auth, Firestore, Analytics, Crashlytics, Messaging, Remote Config)
- **AI**: Google Generative AI SDK 0.9.0 (Gemini)
- **Payments**: RevenueCat 9.23.1
- **Push**: OneSignal + Firebase Messaging
- **Analytics**: PostHog + Firebase Analytics (both tracked via `AnalyticsTracker`)
- **Images**: Coil 2.7.0 with SVG support
- **Animations**: Lottie 6.4.1
- **Testing**: JUnit4, Truth, Mockito-Kotlin, Robolectric 4.14.1, Roborazzi 1.6.0

## Commands

```bash
# Unit tests (includes screenshot tests on debug only)
./gradlew test

# Instrumented tests — requires connected device/emulator
./gradlew connectedAndroidTest

# Coverage report → app/build/reports/jacoco/testDebugUnitTestCoverage/html/index.html
./gradlew testDebugUnitTestCoverage

# Screenshot tests — record golden images
./gradlew recordRoborazziDebug

# Screenshot tests — verify against golden images
./gradlew verifyRoborazziDebug

# Debug build
./gradlew assembleDebug

# Release bundle
./gradlew bundleRelease

# Run a single test class
./gradlew testDebugUnitTest --tests "com.jesuskrastev.bali.ui.screens.auth.AuthViewModelTest"
```

## Code Style & Conventions

**Navigation routes** use type-safe `@Serializable` objects/data classes, not string routes:
```kotlin
@Serializable data class TestRoute(val category: String)
// navController.navigate(TestRoute("topic_name"))
```

**ViewModel state** always uses a single `UiState` data class exposed as `StateFlow`:
```kotlin
private val _uiState = MutableStateFlow(TestUiState())
val uiState: StateFlow<TestUiState> = _uiState.asStateFlow()
```

**Hilt modules**: use `@Binds` (abstract class) to bind interface → impl; use `@Provides` (object) only for third-party types.

**Mappers**: implemented as extension functions in `data/mapper/`, never as methods on entity/domain classes.

**Coroutines**: data layer operations run on `Dispatchers.IO` via `withContext`. ViewModels use `viewModelScope`.

**Complex screen pattern**: screens with non-trivial interactions add `{Feature}Event.kt` (sealed class for user actions) and `{Feature}Reducer.kt` (pure function mapping state + event → new state). Simple screens skip the reducer and update state directly in the ViewModel.

**Coroutine unit tests**: use `MainDispatcherRule` (`app/src/test/…/util/MainDispatcherRule.kt`) as a `@get:Rule` in every ViewModel test. It sets and resets `Dispatchers.Main` via `UnconfinedTestDispatcher`.

**Fake dependencies for tests**: unit tests for ViewModels and use cases use hand-written `Fake*` classes (e.g., `FakeUserRepository`) co-located in the same test package. Use Mockito only for third-party types that can't be faked cheaply.

**Repository auth-routing**: repositories expose a `withAuthRouting { userId -> … }` helper. When a `userId` is present (user authenticated), operations target Firestore; otherwise they fall back to Room. Writes dual-write to both stores so local state stays consistent offline.

## Workflow Rules

- Run `./gradlew test` before committing changes to `domain/` or `data/`.
- Screenshot tests only run in debug build variants — do not run them against release.
- When adding a new `Room` migration, increment `BaliDatabase.version`, add a `Migration` object to the `BaliDatabase` companion, and test it with an instrumented DAO test.
- When adding a new Hilt module, always specify the component scope explicitly (`@Singleton`, etc.) — never rely on implicit scoping.
- When adding a new analytics event, track it in `AnalyticsTracker` (dual-sends to Firebase + PostHog), never call either SDK directly from a ViewModel.

## Branches and releases

Follow `CLAUDE.md` § "Branches and releases". The three rules that must never break:
every feature branches from the latest `origin/develop` (never from another feature) and returns
to it through a squash-merged PR; a feature branch never goes to Play; `main` only receives
`release/*` and `hotfix/*` through a merge commit.

## Code Quality

- ALWAYS refactor code opportunistically when touching a file — improve naming, reduce duplication, simplify logic, and clean up dead code. Leave every file cleaner than you found it.
- ALWAYS add a brief KDoc comment above every function explaining what it does, its parameters, and its return value. Do this for new functions and for existing ones you modify.

## Firestore Migrations

- IMPORTANT: Any significant change to the Firestore data structure (adding/removing fields, renaming collections, changing data types, restructuring documents) MUST be accompanied by a migration class.
- Migrations live in `app/src/main/java/com/jesuskrastev/bali/data/migration/migrations/` and are named `MigrationV{N}To{N+1}.kt` (e.g., `MigrationV3ToV4.kt`).
- Each migration class must implement `FirestoreMigration` (from `domain/migration/`), providing `targetVersion: Int`, `description: String`, and `suspend fun migrate(userId: String)`.
- Register each new migration in `FirestoreMigrationsModule` using `@IntoSet` so `FirestoreMigrationManager` picks it up automatically. Ensure `provideEmptyMigrations()` remains in the module to satisfy Hilt when no migrations are bound.
- NEVER modify Firestore structure directly without a corresponding migration. Treat schema changes the same way you would a Room database migration.

## Important Rules

- **NEVER commit `local.properties`** — it contains `GEMINI_API_KEY`, `ONE_SIGNAL_APP_ID` and `REVENUECAT_API_KEY`. The build will fail without it; add it locally.
- **NEVER commit `google-services.json` to a public repo** — it contains Firebase project credentials.
- `BaliApplication.isRobolectric()` guards skip SDK initialization (OneSignal, PostHog, RevenueCat) in unit tests. NEVER remove this guard — those SDKs crash under Robolectric.
- `versionCode` format is `YYYYMMDD` (e.g., `20260320`). NEVER use sequential integers.
- The `lintVitalAnalyze/Report/Release` tasks are explicitly disabled in `build.gradle.kts` due to a KSP/Lint bug — do not re-enable them.
- All API keys are injected via `BuildConfig` fields read from `local.properties` at build time. NEVER hardcode keys in source files.
