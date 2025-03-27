import kotlinx.fuzz.configurePublishing

plugins {
    id("kotlinx.fuzz.src-module")
}

dependencies {
    implementation(project(":kotlinx.fuzz.engine"))
    implementation(project(":kotlinx.fuzz.api"))
    implementation(kotlin("reflect"))
    implementation(libs.plan.jazzer)
    implementation(libs.slf4j.api)
    implementation(libs.reflections)
}

// Provide plan-jazzer-api for custom hooks
configurations.create("exposedApi") {
    dependencies {
        api(libs.plan.jazzer.api)
    }
}

configurePublishing()
