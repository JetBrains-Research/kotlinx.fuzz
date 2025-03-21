import kotlinx.fuzz.config.CoverageReportType
import kotlin.time.Duration.Companion.hours

plugins {
    id("kotlinx.fuzz.example-module")
    id("kotlinx.fuzz.gradle")
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(libs.plan.jazzer.api)
    implementation(libs.plan.jazzer.junit)
    implementation(libs.reflections)
    implementation(libs.kotlinx.io.core)

    testRuntimeOnly("org.jetbrains:kotlinx.fuzz.jazzer")
}

fuzzConfig {
    instrument = listOf("kotlinx.io.**")
    maxFuzzTimePerTarget = 30.hours
    supportJazzerTargets = true
    logLevel = kotlinx.fuzz.config.LogLevel.DEBUG
    coverage {
        reportTypes = setOf(CoverageReportType.HTML, CoverageReportType.CSV)
        val immutable = libs.kotlinx.io.core.get()
        includeDependencies = setOf("${immutable.group}:${immutable.name}-jvm")
    }
}
