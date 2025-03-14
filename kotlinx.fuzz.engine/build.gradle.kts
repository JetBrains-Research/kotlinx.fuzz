import kotlinx.fuzz.configurePublishing

plugins {
    id("kotlinx.fuzz.src-module")
}

dependencies {
    implementation(project(":kotlinx.fuzz.api"))
    implementation(kotlin("reflect"))
    implementation(libs.rgxgen)
    implementation(libs.slf4j.api)
    implementation(libs.kotlinpoet)
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
    // copy-pasted from Analysis API https://github.com/JetBrains/kotlin/blob/a10042f9099e20a656dec3ecf1665eea340a3633/analysis/low-level-api-fir/build.gradle.kts#L37
    runtimeOnly("com.github.ben-manes.caffeine:caffeine:2.9.3")

    testRuntimeOnly(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}

configurePublishing()
