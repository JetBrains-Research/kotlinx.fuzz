package kotlinx.fuzz.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.nio.file.Path
import kotlin.io.path.*

abstract class OverallStatsTask : DefaultTask() {

    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: Property<Path>

    @TaskAction
    fun calculateOverallStats() {
        processCsvFiles(inputDir.get().asFile.toPath(), outputFile.get())
    }


    companion object {
        private fun processCsvFiles(inputDir: Path, outputFile: Path) {
            val collectedRows = mutableListOf<List<String>>()
            var headerRow: List<String>? = null

            val csvFiles =
                inputDir.listDirectoryEntries().filter { file -> file.extension == "csv" }

            for (file in csvFiles) {
                val rows = file.readLines()

                if (rows.isNotEmpty()) {
                    val originalHeader = rows.first()
                    val dataRows = rows.drop(1)

                    if (dataRows.isNotEmpty()) {
                        val lastRow = dataRows.last()

                        if (headerRow == null) {
                            headerRow = listOf("target name") + originalHeader
                        }

                        val newRow = listOf(file.nameWithoutExtension) + lastRow
                        collectedRows.add(newRow)
                    }
                }
            }

            if (headerRow != null && collectedRows.isNotEmpty()) {
                outputFile.writeText(headerRow.joinToString(separator = ",", postfix = "\n"))
                outputFile.appendLines(collectedRows.map { it.joinToString(separator = ",") })
            } else {
                error("Can't compute overall stats: no CSV files found in $inputDir or no data rows found in any of them")
            }
        }
    }
}