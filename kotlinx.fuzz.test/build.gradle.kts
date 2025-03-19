import kotlinx.fuzz.config.CoverageReportType
import kotlinx.fuzz.config.ReproducerType
import kotlin.time.Duration.Companion.seconds

plugins {
    kotlin("jvm") version "2.0.21"
    id("kotlinx.fuzz.gradle")
    kotlin("plugin.serialization") version "2.0.20"
}

repositories {
    mavenCentral()
    maven(url = "https://plan-maven.apal-research.com")
    maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
}

dependencies {
    testImplementation(kotlin("test")) // adds green arrow in IDEA (no idea why)
    testRuntimeOnly("org.jetbrains:kotlinx.fuzz.jazzer")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.7.3")

    testImplementation("org.jetbrains:jazzer-junit:0.0.3")
}

fuzzConfig {
    keepGoing = 3
    instrument = listOf(
        "kotlinx.fuzz.test.**",
        "kotlinx.collections.immutable.**",
        "kotlinx.serialization.**",
    )
    maxFuzzTimePerTarget = 10.seconds
    coverage {
        reportTypes = setOf(CoverageReportType.HTML, CoverageReportType.CSV)
        includeDependencies = setOf(
            "org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm",
            "org.jetbrains.kotlinx:kotlinx-serialization-protobuf-jvm",
        )
    }
    supportJazzerTargets = true
    reproducerType = ReproducerType.LIST_BASED_INLINE
}

kotlin {
    jvmToolchain(8)
}
