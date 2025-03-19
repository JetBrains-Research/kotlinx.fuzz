import kotlinx.fuzz.configurePublishing

plugins {
    id("kotlinx.fuzz.src-module")
}

dependencies {
    implementation(project(":kotlinx.fuzz.engine"))
    implementation(project(":kotlinx.fuzz.api"))

    implementation(kotlin("reflect"))
    implementation(libs.slf4j.api)
    implementation(libs.junit.platform.engine)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.plan.jazzer.junit)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.testkit)
    testRuntimeOnly(project(":kotlinx.fuzz.jazzer"))
}

tasks.test {
    useJUnitPlatform {
        excludeEngines("kotlinx.fuzz")
    }
}

configurePublishing()