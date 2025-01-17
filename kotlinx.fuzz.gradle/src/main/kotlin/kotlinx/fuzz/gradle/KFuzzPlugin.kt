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

        project.tasks.register<ParseLogsToCsvTask>("parse-fuzz-logs") {
            mustRunAfter("fuzz")

            val resultDir = FuzzConfig.fromSystemProperties().resultDir
            inputDirectory.set(resultDir.resolve("logs").toFile())
            statsDirectory.set(resultDir.resolve("stats").toFile())
            crashesDirectory.set(resultDir.resolve("crashes").toFile())
        }
    }
}

abstract class FuzzTask : Test() {
    @get:Internal
    internal lateinit var fuzzConfig: KFuzzConfig

    @TaskAction
    fun action(): Unit = Unit
}

@Suppress("unused")
fun Project.fuzzConfig(block: KFuzzConfigBuilder.() -> Unit) {
    val config = KFuzzConfigBuilder.build(block)
    tasks.withType<FuzzTask>().forEach { task ->
        task.fuzzConfig = config
    }
}
