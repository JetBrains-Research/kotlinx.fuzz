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

    implementation(libs.kotlinx.datetime)
    testRuntimeOnly("org.jetbrains:kotlinx.fuzz.jazzer")
}


fuzzConfig {
    instrument = listOf("kotlinx.datetime.**")
    maxFuzzTimePerTarget = 2.hours
    supportJazzerTargets = true
    logLevel = kotlinx.fuzz.config.LogLevel.DEBUG
    coverage {
        reportTypes = setOf(CoverageReportType.HTML, CoverageReportType.CSV)
        val datetime = libs.kotlinx.datetime.get()
        includeDependencies = setOf("${datetime.group}:${datetime.name}-jvm")
    }
}
