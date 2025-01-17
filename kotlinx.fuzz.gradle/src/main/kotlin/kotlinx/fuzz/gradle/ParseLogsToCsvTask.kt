package kotlinx.fuzz.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.writeText
import kotlin.time.Duration

abstract class ParseLogsToCsvTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val statsDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val crashesDirectory: DirectoryProperty

    @get:Input
    abstract val duration: Property<Duration>

    @TaskAction
    fun parseLogs() {
        val path = inputDirectory.get().asFile.toPath()
        val statsDir = statsDirectory.get().asFile.toPath()
        val crashDir = crashesDirectory.get().asFile.toPath()

        val files = path.listDirectoryEntries().filter { it.name.endsWith(".exec") }
        for (file in files) {
            val (stats, crashes) = libfuzzerOutputToCsv(file, duration.get())
            val newName = file.name.removeSuffix(".exec") + ".csv"
            statsDir.resolve(newName).writeText(stats)
            crashDir.resolve(newName).writeText(crashes)
        }
    }
}