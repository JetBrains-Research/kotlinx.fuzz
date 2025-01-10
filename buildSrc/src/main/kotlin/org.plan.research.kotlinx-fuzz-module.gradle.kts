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
    testImplementation("io.github.oshai:kotlin-logging-jvm:$KOTLIN_LOGGING_VERSION")
    testImplementation("ch.qos.logback:logback-classic:$LOGBACK_VERSION")
}

kotlin {
    jvmToolchain(17)
}

tasks.getByName<KotlinCompile>("compileKotlin") {
    compilerOptions {
        allWarningsAsErrors = true
    }
}

tasks.test {
    systemProperties["logback.configurationFile"] = rootProject.file("logback.xml")
}
