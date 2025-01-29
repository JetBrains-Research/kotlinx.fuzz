package kotlinx.fuzz.gradle

import kotlinx.fuzz.KFuzzConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*

@Suppress("unused")
abstract class KFuzzPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.dependencies {
            add("testImplementation", "kotlinx.fuzz:kotlinx.fuzz.api")
            add("testRuntimeOnly", "kotlinx.fuzz:kotlinx.fuzz.gradle")
        }

        project.tasks.withType<Test>().configureEach {
            if (this is FuzzTask) {
                return@configureEach
            }

            useJUnitPlatform {
                excludeEngines("kotlinx.fuzz")
            }
        }

        project.tasks.register<FuzzTask>("fuzz") {
            outputs.upToDateWhen { false }  // so the task will run on every invocation
            doFirst {
                systemProperties(fuzzConfig.toPropertiesMap())
            }
            useJUnitPlatform {
                includeEngines("kotlinx.fuzz")
            }
        }
    }
}

abstract class FuzzTask : Test() {
    @get:Internal
    internal lateinit var fuzzConfig: KFuzzConfig

    @TaskAction
    fun action() {
        overallStats()
    }

    private fun overallStats() {
        val workDir = fuzzConfig.workDir
        overallStats(workDir.resolve("stats"), workDir.resolve("overall-stats.csv"))
    }
}

@Suppress("unused")
fun Project.fuzzConfig(block: KFuzzConfigBuilder.() -> Unit) {
    val buildDir = layout.buildDirectory.get()
    val defaultWorkDir = buildDir.dir("fuzz").asFile.toPath()
    val config = KFuzzConfigBuilder.build {
        workDir = defaultWorkDir
        reproducerPath = defaultWorkDir.resolve("reproducers")
        block()
    }

    tasks.withType<FuzzTask>().forEach { task ->
        task.fuzzConfig = config
    }
}
