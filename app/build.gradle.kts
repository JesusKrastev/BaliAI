import java.io.FileInputStream
import java.util.Properties
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.com.google.dagger)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
    alias(libs.plugins.roborazzi)
    id("jacoco")
}

/** Propiedades de local.properties; queda vacío si el fichero no existe (caso CI). */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) FileInputStream(file).use { load(it) }
}

/**
 * Resuelve un secreto de build buscando primero en local.properties (desarrollo local)
 * y después en las variables de entorno (CI).
 *
 * Un valor en blanco cuenta como ausente: GitHub Actions define la variable de entorno
 * igualmente cuando el secret no existe, y sin esto el [default] nunca se aplicaría.
 *
 * @param key clave, con el mismo nombre en local.properties y en el entorno.
 * @param default valor devuelto si la clave no existe en ninguno de los dos orígenes.
 * @return el valor del secreto, o [default] si no está definido en ningún sitio.
 */
fun secret(key: String, default: String = ""): String =
    localProperties.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: System.getenv(key)?.takeIf { it.isNotBlank() }
        ?: default

android {
    namespace = "com.jesuskrastev.bali"
    compileSdk {
        version = release(36)
    }

    signingConfigs {
        // La signingConfig "debug" es la que AGP crea por defecto: misma ruta
        // (~/.android/debug.keystore) y mismas credenciales, pero generándola si no
        // existe. Declararla a mano hacía que validateSigningDebug fallase en
        // cualquier máquina limpia, como los runners de CI.
        create("release") {
            val keystorePath = secret("RELEASE_KEYSTORE_PATH")
            if (keystorePath.isNotBlank()) {
                storeFile = file(keystorePath)
                storePassword = secret("RELEASE_STORE_PASSWORD")
                keyAlias = secret("RELEASE_KEY_ALIAS")
                keyPassword = secret("RELEASE_KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "com.jesuskrastev.bali"
        minSdk = 24
        targetSdk = 36
        // Format   o YYYYMMDDNN: fecha de publicación + nº de build de ese día.
        // En CI lo inyecta el workflow vía CI_VERSION_CODE; en local se usa el valor base.
        versionCode = secret("CI_VERSION_CODE", "2026091700").toInt()
        versionName = "1.1.9"

        testInstrumentationRunner = "com.jesuskrastev.bali.HiltTestRunner"
        buildConfigField("String", "ONE_SIGNAL_APP_ID", "\"${secret("ONE_SIGNAL_APP_ID")}\"")
        buildConfigField("String", "MIXPANEL_TOKEN", "\"${secret("MIXPANEL_TOKEN")}\"")
        buildConfigField("String", "REVENUECAT_API_KEY", "\"${secret("REVENUECAT_API_KEY")}\"")
        buildConfigField("String", "POSTHOG_API_KEY", "\"${secret("POSTHOG_API_KEY")}\"")
        buildConfigField("String", "POSTHOG_HOST", "\"${secret("POSTHOG_HOST", "https://us.i.posthog.com")}\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Con keystore de subida (CI o local.properties) firma de release; si no, debug,
            // para que un bundleRelease local siga funcionando sin credenciales.
            signingConfig = if (secret("RELEASE_KEYSTORE_PATH").isNotBlank()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    lint {
        disable.add("KtLintInternalError")
        abortOnError = false
    }
}

tasks.withType<Test>().configureEach {
    if (name.contains("Release", ignoreCase = true)) {
        exclude("**/*ScreenshotTest*")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.animation.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation
    implementation(libs.compose.navigation)
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    // Icons
    implementation(libs.androidx.compose.material.icons.extended)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Dagger Hilt
    ksp(libs.dagger.hilt.android.compiler)
    implementation(libs.dagger.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)

    // Gemini

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    // Lottie
    implementation(libs.lottie.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.config.ktx)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.ai)
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    // Auth (Credential Manager)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // One signal
    implementation(libs.onesignal)

    // In-App updates
    implementation(libs.app.update)
    implementation(libs.app.update.ktx)

    // In-App reviews
    implementation(libs.app.review)

    // Mixpanel
    implementation(libs.mixpanel.android)

    // PostHog
    implementation(libs.posthog.android)

    // Testing - Unit Tests
    testImplementation(libs.truth)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.junit4)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)

    // Testing - Hilt
    testImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.hilt.android.testing)
    kspTest(libs.dagger.hilt.android.compiler)
    kspAndroidTest(libs.dagger.hilt.android.compiler)

    // Testing - Compose UI Tests
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.truth)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Testing - Screenshot Tests (Roborazzi)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)

    // Testing - Instrumented Tests
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.espresso.core)

    // Revenuecat
    implementation(libs.purchases)
    implementation(libs.purchases.ui)

    // Compose charts
    implementation (libs.compose.charts)
}

// Workaround for Kotlin FIR symbol resolution bug in lint with build scripts
afterEvaluate {
    tasks.matching { it.name == "lintVitalAnalyzeRelease" || it.name == "lintVitalRelease" || it.name == "lintVitalReportRelease" }.all {
        onlyIf { false }
    }
}

// --- Merged from jacoco.gradle.kts ---

val jacocoTestReportExcludes = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "android/**/*.*",
    "**/*_HiltModules*.*",
    "**/*_MembersInjector*.*",
    "**/*_Factory*.*",
    "**/*_ProvideField*.*",
    "**/*_LifecycleAdapter*.*",
    "**/Dagger*.*",
    "**/Hilt*.*",
    "**/*ScreenKt*.*", // Composable screens often have low meaningful coverage
    "**/*ThemeKt*.*",
    "**/*ComposableSingletons*.*"
)

tasks.register<JacocoReport>("testDebugUnitTestCoverage") {
    dependsOn("testDebugUnitTest")
    group = "Reporting"
    description = "Generate Jacoco coverage reports for the debug build."

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val kotlinTree = fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(jacocoTestReportExcludes)
    }
    
    val javaTree = fileTree("${project.layout.buildDirectory.get()}/intermediates/javac/debug/classes") {
        exclude(jacocoTestReportExcludes)
    }

    classDirectories.setFrom(files(kotlinTree, javaTree))
    
    sourceDirectories.setFrom(files("${project.projectDir}/src/main/java"))
    
    executionData.setFrom(fileTree(project.layout.buildDirectory.get()) {
        include("jacoco/testDebugUnitTest.exec")
    })
}

configure<JacocoPluginExtension> {
    toolVersion = "0.8.12"
}
