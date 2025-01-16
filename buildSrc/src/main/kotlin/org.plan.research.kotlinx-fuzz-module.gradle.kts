import org.gradle.api.tasks.testing.logging.TestExceptionFormat
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
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.register("setLoggingProperties") {
    val logback = rootProject.file("logback.xml")
    val backupFile = rootProject.file("logback.xml.bak")

    doFirst {
        if (!backupFile.exists()) {
            logback.copyTo(backupFile, overwrite = true)
        }

        val envValue = System.getenv("KOTLINX_FUZZ_LOGGING_LEVEL") ?: "OFF"
        val oldContent = logback.readText().split("<configuration>\n")
        logback.writeText(
            "${oldContent[0]}<configuration>\n<property name=\"KOTLINX_FUZZ_LOGGING_LEVEL\" value=\"$envValue\"/>\n${
                oldContent.stream().skip(1).toList().joinToString("<configuration>\n")
            }"
        )
    }

    doLast {
        if (backupFile.exists()) {
            backupFile.copyTo(logback, overwrite = true)
            backupFile.delete()
        }
    }
}

tasks.named("processResources").configure {
    dependsOn("setLoggingProperties")
}

