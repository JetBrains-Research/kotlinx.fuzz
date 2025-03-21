plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.diktat.gradle.plugin)
    implementation(kotlin("reflect"))
}

kotlin {
    jvmToolchain(8)
}
