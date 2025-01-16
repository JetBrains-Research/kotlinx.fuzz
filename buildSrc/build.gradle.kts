import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

val properties = loadProperties(rootDir.parentFile.resolve("gradle.properties").absolutePath)

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${properties["kotlin.version"]}")
    implementation("de.undercouch.download:de.undercouch.download.gradle.plugin:5.1.0")

    implementation(kotlin("reflect"))
    implementation("org.reflections:reflections:0.10.2")
}

kotlin {
    jvmToolchain(17)
}
