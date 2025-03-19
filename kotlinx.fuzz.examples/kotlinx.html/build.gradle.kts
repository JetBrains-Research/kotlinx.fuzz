import kotlinx.fuzz.config.CoverageReportType
import kotlin.time.Duration.Companion.seconds

plugins {
    id("kotlinx.fuzz.example-module")
    id("kotlinx.fuzz.gradle")
}


dependencies {
    testImplementation(kotlin("reflect"))
    implementation(libs.plan.jazzer.api)
    implementation(libs.plan.jazzer.junit)
    implementation(libs.reflections)
    implementation(libs.jsoup)
    implementation(libs.kotlinx.html)
    implementation(libs.kotlinx.html.jvm)

    testRuntimeOnly("org.jetbrains:kotlinx.fuzz.jazzer")

}

fuzzConfig {
    instrument = listOf("kotlinx.html.**")
    maxFuzzTimePerTarget = 10.seconds // TODO
    supportJazzerTargets = true
    logLevel = kotlinx.fuzz.config.LogLevel.DEBUG
//    detailedLogging = true
    coverage {
        reportTypes = setOf(CoverageReportType.HTML, CoverageReportType.CSV)
        val immutable = libs.kotlinx.html.jvm.get()
        includeDependencies = setOf("${immutable.group}:${immutable.name}-jvm")
    }
}
