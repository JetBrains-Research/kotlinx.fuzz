package kotlinx.fuzz.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register

abstract class SampleTask : DefaultTask() {
    @TaskAction
    fun run() {
        println("running sample task")
    }
}

class MainPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register<SampleTask>("sample")
    }
}
