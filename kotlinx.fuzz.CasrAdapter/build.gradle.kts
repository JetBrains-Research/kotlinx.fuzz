import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("kotlinx.fuzz.src-module")
}

dependencies {
    implementation(project(":kotlinx.fuzz.api"))
    implementation(libs.rgxgen)
    implementation(libs.slf4j.api)

    testRuntimeOnly(libs.junit.jupiter)
}

tasks.register<Exec>("buildRustLib") {
    workingDir = file("$projectDir/CasrAdapter")
    commandLine = listOf("/usr/bin/env", "cargo", "build", "--release")
}

fun File.listDLLs(): Array<File>? = listFiles { file ->
    file.name.endsWith(".dylib") || file.name.endsWith(".so") || file.name.endsWith(".dll")
}

tasks.register("linkRustLib") {
    dependsOn("buildRustLib")
    doLast {
        val sourceDir = file("$projectDir/CasrAdapter/target/release")
        val targetDir = file(layout.buildDirectory.dir("libs"))

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        if (!sourceDir.exists()) {
            throw GradleException("Source directory $sourceDir does not exist")
        }

        sourceDir.listDLLs()?.forEach { file ->
            val targetLink = targetDir.resolve(file.name)
            if (targetLink.exists()) {
                targetLink.delete()
            }

            try {
                Files.createSymbolicLink(targetLink.toPath(), file.toPath())
            } catch (e: UnsupportedOperationException) {
                println("Warning: Symbolic links are not supported or failed to create. Falling back to file copy.")
                Files.copy(file.toPath(), targetLink.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } catch (e: Exception) {
                throw GradleException("Failed to handle file ${file.name}: ${e.message}")
            }
        }
    }
}


tasks.named("compileKotlin") {
    dependsOn("linkRustLib")
}

tasks.register<Exec>("cleanCargo") {
    workingDir = file("$projectDir/CasrAdapter")
    commandLine = listOf("/usr/bin/env", "cargo", "clean")
}

tasks.named("clean") {
    dependsOn("cleanCargo")
    doLast {
        val targetDir = file(layout.buildDirectory.dir("libs"))
        if (targetDir.exists()) {
            targetDir.listDLLs()?.forEach { it.delete() }
        }
    }
}