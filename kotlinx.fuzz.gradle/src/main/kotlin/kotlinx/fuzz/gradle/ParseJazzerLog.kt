package kotlinx.fuzz.gradle

import java.io.File
import java.nio.file.Path
import kotlin.io.path.forEachLine
import kotlin.reflect.full.memberProperties
import kotlin.time.Duration


private data class LibfuzzerLogEntryNoTimestamp(
    val execNr: Int,
    val cov: Int,
    val ft: Int,
    val crashes: Int,
) {
    fun withTimestamp(timeSeconds: Long): LibfuzzerLogEntry = LibfuzzerLogEntry(
        timeSeconds = timeSeconds,
        execNr = execNr,
        cov = cov,
        ft = ft,
        crashes = crashes
    )
}

private data class LibfuzzerLogEntry(
    val timeSeconds: Long,
    val execNr: Int,
    val cov: Int,
    val ft: Int,
    val crashes: Int,
)

private data class CrashEntryNoTimestamp(
    val execNr: Int,
    val inputPath: String,
) {
    fun withTimestamp(timeSeconds: Long): CrashEnty = CrashEnty(
        timeSeconds = timeSeconds,
        execNr = execNr,
        inputPath = inputPath,
    )
}

private data class CrashEnty(
    val timeSeconds: Long,
    val execNr: Int,
    val inputPath: String
)

internal data class ParseResult(val statsCsv: String, val crashesCsv: String)

internal fun libfuzzerOutputToCsv(file: Path, duration: Duration): ParseResult {
    val lines =
        mutableListOf(LibfuzzerLogEntryNoTimestamp(execNr = 0, cov = 0, ft = 0, crashes = 0))
    val crashesNoTimestamps = mutableListOf<CrashEntryNoTimestamp>()
    val durationSeconds = duration.inWholeSeconds.toLong()

    file.forEachLine { line ->
        val tokens = line.split("\\s+".toRegex())  // Split line into tokens
        if (tokens.size < 2) return@forEachLine

        if (tokens[0].startsWith("artifact_prefix=")) {
            crashesNoTimestamps.add(
                CrashEntryNoTimestamp(
                    execNr = lines.last().execNr,
                    inputPath = tokens[5]
                )
            )
            return@forEachLine
        } else if (tokens[0].startsWith("#") && tokens.size >= 14 &&
            (tokens[1] == "NEW" || tokens[1] == "REDUCE" || tokens[1] == "pulse")
        ) {
            val execs = tokens[0].substring(1).toInt()
            val covBlks = tokens[3].toInt()
            val covFt = tokens[5].toInt()
            lines.add(
                LibfuzzerLogEntryNoTimestamp(
                    execNr = execs,
                    cov = covBlks,
                    ft = covFt,
                    crashes = crashesNoTimestamps.size
                )
            )
        }
    }

    val maxExecNr = lines.maxOf { it.execNr }
    val stats = lines.map { it.withTimestamp(it.execNr * durationSeconds / maxExecNr) }
    val crashes =
        crashesNoTimestamps.map { it.withTimestamp(it.execNr * durationSeconds / maxExecNr) }

    val statsCsv = stats.toCsv()
    val crashesCsv = crashes.toCsv()
    return ParseResult(statsCsv = statsCsv, crashesCsv = crashesCsv)
}


private inline fun <reified T : Any> List<T>.toCsv(): String {
    require(this.isNotEmpty())

    val properties = T::class.memberProperties
    val header = properties.joinToString(separator = ",") { it.name }
    val rows = this.map { item ->
        properties.joinToString(separator = ",") { prop ->
            prop.get(item)?.toString() ?: error("$item doesnt have property $prop")
        }
    }

    return (listOf(header) + rows).joinToString(separator = "\n")
}