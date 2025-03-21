import kotlinx.fuzz.config.CoverageReportType
import kotlin.time.Duration.Companion.hours

plugins {
    id("kotlinx.fuzz.example-module")
    id("kotlinx.fuzz.gradle")
    kotlin("plugin.serialization") version "2.0.20"
}

dependencies {
    implementation(libs.plan.jazzer.api)
    implementation(libs.plan.jazzer.junit)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.cbor)
    implementation(libs.kotlinx.serialization.properties)
    implementation(libs.kotlinx.serialization.protobuf)

    testRuntimeOnly("org.jetbrains:kotlinx.fuzz.jazzer")
}

fuzzConfig {
    instrument = listOf("kotlinx.serialization.**")
    maxFuzzTimePerTarget = 8.hours
    supportJazzerTargets = true
    logLevel = kotlinx.fuzz.config.LogLevel.DEBUG
    coverage {
        reportTypes = setOf(CoverageReportType.HTML, CoverageReportType.CSV)
        val deps = with(libs.kotlinx.serialization) {
            listOf(json, cbor, properties, protobuf)
        }
            .map { it.get() }
            .map { "${it.group}:${it.name}-jvm" }
            .toSet()

        includeDependencies = deps
    }
}
