package kotlinx.fuzz.gradle

import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test

abstract class FuzzTask : Test() {
    @TaskAction
    fun action() {
        println("Invoking FuzzTask")
    }
}