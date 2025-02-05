import kotlinx.fuzz.JacocoReport.*
import kotlinx.fuzz.gradle.fuzzConfig
import kotlin.time.Duration.Companion.seconds

plugins {
    kotlin("jvm") version "2.0.21"
    id("kotlinx.fuzz.gradle")
}

repositories {
    mavenCentral()
    maven(url = "https://plan-maven.apal-research.com")
}

dependencies {
    testImplementation(kotlin("test")) // adds green arrow in IDEA (no idea why)
    testRuntimeOnly("org.jetbrains:kotlinx.fuzz.jazzer")
}

fuzzConfig {
    keepGoing = 5
    instrument = listOf("kotlinx.fuzz.test.**")
    maxSingleTargetFuzzTime = 10.seconds
    jacocoReports = setOf(HTML, CSV, XML)
    logLevel = "debug"
}

kotlin {
    jvmToolchain(17)
}
