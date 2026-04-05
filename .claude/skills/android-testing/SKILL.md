---
name: android-testing
description: Comprehensive testing strategy involving Unit, Integration, Hilt, and Screenshot tests.
---

# Android Testing Strategies

This skill provides expert guidance on testing modern Android applications, inspired by "Now in Android". It covers **Unit Tests**, **Hilt Integration Tests**, and **Screenshot Testing**.

## Testing Pyramid

1.  **Unit Tests**: Fast, isolate logic (ViewModels, Repositories).
2.  **Integration Tests**: Test interactions (Room DAOs, Retrofit vs MockWebServer).
3.  **UI/Screenshot Tests**: Verify UI correctness (Compose).

## Dependencies (`libs.versions.toml`)

Ensure you have the right testing dependencies.

```toml
[versions]
kotlinxCoroutines = "1.9.0"
hilt = "2.54"
roborazzi = "1.58.0"
robolectric = "4.14.1"
androidxTestExtJunit = "1.2.1"
espressoCore = "3.6.1"
androidxTestRunner = "1.6.2"

[libraries]
junit4 = { module = "junit:junit", version = "4.13.2" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxTestExtJunit" }
androidx-test-runner = { group = "androidx.test", name = "runner", version.ref = "androidxTestRunner" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
compose-ui-test = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
hilt-android-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hilt" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
roborazzi = { group = "io.github.takahirom.roborazzi", name = "roborazzi", version.ref = "roborazzi" }
roborazzi-compose = { group = "io.github.takahirom.roborazzi", name = "roborazzi-compose", version.ref = "roborazzi" }
roborazzi-rule = { group = "io.github.takahirom.roborazzi", name = "roborazzi-rule", version.ref = "roborazzi" }

[plugins]
roborazzi = { id = "io.github.takahirom.roborazzi", version.ref = "roborazzi" }
```

## `build.gradle.kts` setup

```kotlin
plugins {
    alias(libs.plugins.roborazzi)
}

android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Unit tests
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)

    // Hilt testing
    testImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.hilt.android.testing)
    kspTest(libs.dagger.hilt.android.compiler)
    kspAndroidTest(libs.dagger.hilt.android.compiler)

    // Compose UI tests
    androidTestImplementation(libs.compose.ui.test)
    debugImplementation(libs.compose.ui.test.manifest)

    // Screenshot tests (Roborazzi)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.rule)

    // Instrumented tests
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.espresso.core)
}
```

## Screenshot Testing with Roborazzi

Screenshot tests ensure your UI doesn't regress visually. Roborazzi runs on the JVM — fast, no emulator needed.

### Writing a Screenshot Test

```kotlin
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel6)
class MyScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun captureMyScreen() {
        composeTestRule.setContent {
            MyTheme {
                MyScreen()
            }
        }

        composeTestRule.onRoot()
            .captureRoboImage()
    }
}
```

## Hilt Testing

Use `HiltAndroidRule` to inject dependencies in tests.

```kotlin
@HiltAndroidTest
class MyDaoTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: MyDatabase
    private lateinit var dao: MyDao

    @Before
    fun init() {
        hiltRule.inject()
        dao = database.myDao()
    }

    // ... tests
}
```

### Custom TestRunner for Hilt (required for instrumented tests)

```kotlin
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        name: String?,
        context: Context?
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
```

Register it in `build.gradle.kts`:
```kotlin
defaultConfig {
    testInstrumentationRunner = "com.yourpackage.HiltTestRunner"
}
```

## ViewModel Unit Test (coroutines + Hilt)

```kotlin
class MyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepository = FakeMyRepository()
    private lateinit var viewModel: MyViewModel

    @Before
    fun setup() {
        viewModel = MyViewModel(fakeRepository)
    }

    @Test
    fun `uiState emits Loading then Success`() = runTest {
        val states = viewModel.uiState.take(2).toList()
        assertThat(states[0]).isInstanceOf(UiState.Loading::class.java)
        assertThat(states[1]).isInstanceOf(UiState.Success::class.java)
    }
}
```

## Running Tests

- **Unit**: `./gradlew test`
- **Instrumented**: `./gradlew connectedAndroidTest`
- **Screenshots (record)**: `./gradlew recordRoborazziDebug`
- **Screenshots (verify)**: `./gradlew verifyRoborazziDebug`
