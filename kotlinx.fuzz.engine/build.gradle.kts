import kotlinx.fuzz.configurePublishing

plugins {
    kotlin("plugin.serialization") version "2.2.0"
    id("kotlinx.fuzz.src-module")
}

dependencies {
    implementation(project(":kotlinx.fuzz.api"))
    implementation(kotlin("reflect"))
    implementation(libs.casr.adapter)
    implementation(libs.slf4j.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.compiler.k2) {
        isTransitive = false
    }
    listOf(
        libs.kotlin.analysis.api.api,
        libs.kotlin.analysis.api.standalone,
    ).forEach {
        implementation(it) {
            isTransitive = false // see KTIJ-19820
        }
    }
    listOf(
        libs.kotlin.analysis.api.impl,
        libs.kotlin.analysis.api.fir,
        libs.kotlin.low.level.api.fir,
        libs.kotlin.analysis.api.platform,
        libs.kotlin.symbol.light.classes,
    ).forEach {
        runtimeOnly(it) {
            isTransitive = false // see KTIJ-19820
        }
    }

    testRuntimeOnly(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}

configurePublishing()
