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
    implementation(kotlin("reflect"))
    implementation(libs.reflections)
}

kotlin {
    jvmToolchain(8)
}
