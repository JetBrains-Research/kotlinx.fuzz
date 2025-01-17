package kotlinx.fuzz.gradle

import java.io.File
import java.nio.file.Path
import kotlin.io.path.appendLines
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readLines
import kotlin.io.path.writeText

class OverallStatsTask {
    companion object {
        fun processCsvFiles(inputDir: Path, outputFile: Path) {
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
                error("")
            }
        }
    }
}