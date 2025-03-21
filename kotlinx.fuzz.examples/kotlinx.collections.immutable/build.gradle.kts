import kotlinx.fuzz.config.CoverageReportType
import kotlin.time.Duration.Companion.hours


plugins {
    id("kotlinx.fuzz.example-module")
    id("kotlinx.fuzz.gradle")
}

dependencies {
    testImplementation(kotlin("reflect"))
    implementation(libs.plan.jazzer.api)
    implementation(libs.plan.jazzer.junit)
    implementation(libs.reflections)
    implementation(libs.kotlinx.collections.immutable)

    testRuntimeOnly("org.jetbrains:kotlinx.fuzz.jazzer")
}

fuzzConfig {
    instrument = listOf("kotlinx.collections.**")
    maxFuzzTimePerTarget = 2.hours
    supportJazzerTargets = true
    logLevel = kotlinx.fuzz.config.LogLevel.DEBUG
    coverage {
        reportTypes = setOf(CoverageReportType.HTML, CoverageReportType.CSV)
        val immutable = libs.kotlinx.collections.immutable.get()
        includeDependencies = setOf("${immutable.group}:${immutable.name}-jvm")
    }
}
