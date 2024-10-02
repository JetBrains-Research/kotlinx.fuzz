import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

group = GROUP_ID
version = VERSION

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.getByName<KotlinCompile>("compileKotlin") {
    compilerOptions.allWarningsAsErrors = true
}
