import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension

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
