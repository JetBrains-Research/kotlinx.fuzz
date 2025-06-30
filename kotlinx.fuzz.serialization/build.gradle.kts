import kotlinx.fuzz.configurePublishing

plugins {
    id("kotlinx.fuzz.src-module")
    alias(libs.plugins.serialization)
}

dependencies {
    implementation(project(":kotlinx.fuzz.api"))
    implementation(kotlin("reflect"))
    implementation(libs.kotlinx.serialization.core)
    testRuntimeOnly(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}

configurePublishing()
