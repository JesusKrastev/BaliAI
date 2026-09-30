# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 🧠 Brain — long-term memory for this project

Decisions, past mistakes and cross-repo contracts live outside this repo, in the Obsidian vault at
`G:\Mi unidad\Obsidian\Claude`. This file says *what* the project is; the vault says *why it is
that way*, *what already went wrong* and *what is still open*.

**At the start of a work session, read exactly these three things — nothing else:**

1. `G:\Mi unidad\Obsidian\Claude\00-INDEX.md` — the map and the protocol.
2. `G:\Mi unidad\Obsidian\Claude\01-Proyectos\BaliAI\BaliAI.md` — this project's card.
3. `grep -i "BaliAI" "G:\Mi unidad\Obsidian\Claude\03-Sync\SYNC-PENDIENTES.md"` — work queued for
   this repo by changes made in BaliAIPage.

Everything else (`BaliAI-decisiones.md`, `BaliAI-errores.md`, `02-Contratos/`, `04-Ideas/`) is
opened **on demand and with `grep` first** — never read whole. That restraint is the point: the
vault is designed so a session loads ~1.5k tokens of memory, not 50k.

**Before finishing a session with real changes**, write back: one line in `BaliAI-estado.md`, plus a
line in `BaliAI-decisiones.md` or `BaliAI-errores.md` if you decided something or hit a trap worth
remembering. Formats are in `_plantillas\plantilla-linea-log.md`.

**If you touch data collection, SDKs, permissions, pricing, the paywall or any user-facing feature**,
it probably has to reach the landing site (BaliAIPage): update the matching file in
`02-Contratos\` and queue a line in `03-Sync\SYNC-PENDIENTES.md`. Do **not** edit the other repo
from here — see `03-Sync\SYNC-reglas.md`.

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
- **Local DB**: Room 2.6.1 (DB version 13, `exportSchema = false`)
- **Preferences**: DataStore 1.1.2
- **Backend**: Firebase BOM 33.16.0 (Auth, Firestore, Analytics, Crashlytics, Messaging, Remote Config, App Check)
- **AI**: Firebase AI Logic (`firebase-ai`) against the Gemini Developer API backend. No API key ships
  in the app: Firebase proxies the call and App Check attests the caller. Models are configured in
  `di/GeminiModule.kt`, one qualifier per use (`@TutorModel`, `@QuestionsModel`, `@PathNodesModel`).
  Note the project is on Kotlin 2.0.21, so Firebase BOM 34.x will not compile against it.
- **Payments**: RevenueCat 10.16.0 (`purchases` + `purchases-ui`: paywall and Customer Center)
- **Push**: OneSignal + Firebase Messaging
- **Analytics**: Mixpanel + PostHog + Firebase Analytics (all three tracked via `AnalyticsTracker`)
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

**System bar insets**: `AppNavigation`'s `Scaffold` pads the whole `NavHost` for the status bar, the bottom navigation bar (or the nav-bar inset when there is none) and *consumes* those insets (`consumeWindowInsets`). Screens therefore must NOT add their own `statusBarsPadding()` / `navigationBarsPadding()` for those bars, nor override `contentWindowInsets` / `TopAppBar(windowInsets = …)` to zero — inner `Scaffold`s, `TopAppBar`s and bottom bars already see them as consumed. Only `imePadding()` is still the screen's job. Applying insets in both places is what caused the doubled padding above every `TopAppBar`.

## Workflow Rules

- Run `./gradlew test` before committing changes to `domain/` or `data/`.
- Screenshot tests only run in debug build variants — do not run them against release.
- When adding a new `Room` migration, increment `BaliDatabase.version`, add a `Migration` object to the `BaliDatabase` companion, and test it with an instrumented DAO test.
- When adding a new Hilt module, always specify the component scope explicitly (`@Singleton`, etc.) — never rely on implicit scoping.
- When adding a new analytics event, track it in `AnalyticsTracker` (dual-sends to Firebase + Mixpanel + PostHog), never call any of those SDKs directly from a ViewModel.

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

- **NEVER commit `local.properties`** — it contains `ONE_SIGNAL_APP_ID`, `MIXPANEL_TOKEN`, `REVENUECAT_API_KEY`, and `POSTHOG_API_KEY`. Add it locally; without it the app builds but ships empty SDK keys.
- **NEVER put the Gemini API key back into `BuildConfig`.** A `buildConfigField` is a plain string in the shipped APK; that is why the app moved to Firebase AI Logic. Gemini credentials belong in the Firebase project only.
- Debug builds need their App Check debug token registered once per machine (Firebase console -> App Check -> Apps -> Debug tokens), otherwise every AI request is rejected. The token is printed to Logcat on first run.
- **NEVER commit `google-services.json` to a public repo** — it contains Firebase project credentials.
- `RobolectricDetector.isRobolectric()` (root package) guards skip SDK initialization (OneSignal, Mixpanel, PostHog, RevenueCat) in unit tests — called from `BaliApplication.onCreate()` and from the Mixpanel/PostHog Hilt modules. NEVER remove this guard, and never reimplement the check inline (e.g. `Build.FINGERPRINT == "robolectric"`) instead of calling it — those SDKs crash under Robolectric.
- `versionCode` format is `YYYYMMDDNN` (e.g., `2026032007`): publish date plus a two-digit counter for that day's builds, so several builds can be uploaded per day. NEVER use sequential integers. CI injects it via the `CI_VERSION_CODE` env var; the literal in `defaultConfig` is only the local-dev fallback. Play requires codes to increase monotonically — never go back to the old 8-digit form.
- The `lintVitalAnalyze/Report/Release` tasks are explicitly disabled in `build.gradle.kts` due to a KSP/Lint bug — do not re-enable them.
- All API keys are injected via `BuildConfig` fields resolved by the `secret(key, default)` helper in `app/build.gradle.kts`, which reads `local.properties` first and falls back to environment variables (that is how CI supplies them). NEVER hardcode keys in source files.
- Release signing is driven by `RELEASE_KEYSTORE_PATH` / `RELEASE_STORE_PASSWORD` / `RELEASE_KEY_ALIAS` / `RELEASE_KEY_PASSWORD` through the same `secret()` helper. When `RELEASE_KEYSTORE_PATH` is absent the `release` build type falls back to the debug keystore so local `bundleRelease` still works — that fallback artifact is NOT uploadable to Play.

## Open product decisions (from the 2026-09 code review sweep)

The sweep of the whole codebase is finished (migrations were intentionally excluded). These findings were **not** fixed because they need a product/design decision, not a guess. Delete each line once decided.

- **Free practice is unreachable.** The Topics and Mistakes screens were deleted (nothing linked to them), which leaves `TestRoute()` without a node (free practice) and `TestRoute.topic` / `TestViewModel.setTopic` as dead paths: `TestRoute` is only navigated to from Home's learning-path nodes. Remove them (plus `TestViewModelTest`'s `setTopic` test and `ScreenNameTest`'s route string) or give free practice a new entry point.
- Streak freezes have no visual feedback: `StreakStatus.FROZEN` is implemented in `StreakScreen` / `LessonStreakScreen` but never produced — `StreakUiHelper.generateWeeklyStreak()` has no data source for "which day was frozen" (no `frozenDays`-style field on `User`). Streak evaluation runs server-side in a Cloud Function this repo doesn't contain.
- `SectionHeaderCard` prints "SECCIÓN n, UNIDAD n" using the same index for both numbers.
- `SenalRelampagoGame`'s `SIGN_POOL` has two entries for sign R-102 with different Spanish names — possibly a duplicate, unverified against the official DGT catalogue.
- `TestResultScreen`'s `XpRow(isBonus: Boolean)` parameter is passed by every caller but never read by the composable — bonus and base XP rows render identically.
