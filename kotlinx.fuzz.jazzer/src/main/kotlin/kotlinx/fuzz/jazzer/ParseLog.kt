package kotlinx.fuzz.jazzer

import java.nio.file.Path
import kotlin.io.path.readLines
import kotlin.time.Duration

const val HEADER = "execNr,timeSeconds,cov,ft,crashes\n"

internal data class LibfuzzerLogEntryNoTimestamp(
    val execNr: Long,
    val cov: Long,
    val ft: Long,
    val crashes: Long,
) {
    fun addTimestamp(timeSeconds: Long): LibfuzzerLogEntry = LibfuzzerLogEntry(
        timeSeconds = timeSeconds,
        execNr = execNr,
        cov = cov,
        ft = ft,
        crashes = crashes,
    )
}

internal data class LibfuzzerLogEntry(
    val timeSeconds: Long,
    val execNr: Long,
    val cov: Long,
    val ft: Long,
    val crashes: Long,
)

internal fun jazzerLogToCsv(file: Path, duration: Duration): String {
    var crashes = 0L

    /*
    Example of jazzer logs. More info about meaning in LibFuzzer's documentation: https://llvm.org/docs/LibFuzzer.html#output
#2165	    NEW    cov: 9 ft: 9 corp: 4/30b lim: 21 exec/s: 0 rss: 3154Mb L: 20/20 MS: 2 InsertRepeatedBytes-Custom-
#2285	    REDUCE cov: 9 ft: 9 corp: 4/27b lim: 21 exec/s: 0 rss: 3154Mb L: 17/17 MS: 10 ShuffleBytes-Custom-ChangeBit-Custom-ChangeByte-Custom-ChangeByte-Custom-EraseBytes-Custom-
#4194304	pulse  cov: 9 ft: 9 corp: 4/19b lim: 4096 exec/s: 2097152 rss: 3609Mb
     */
    val statsNoTimeStamps = file.readLines().mapNotNull { line ->
        val tokens = line.split("\\s+".toRegex())  // Split line into tokens
        when {
            tokens.size < 2 -> null

            tokens[0].startsWith("artifact_prefix=") -> {
                crashes++
                null
            }

            !tokens[0].startsWith("#") -> null
            @Suppress("MAGIC_NUMBER") tokens.size < 14 -> null

            tokens[1] !in setOf("NEW", "REDUCE", "pulse") -> null

            else -> @Suppress("MAGIC_NUMBER") LibfuzzerLogEntryNoTimestamp(
                execNr = tokens[0].substring(1).toLong(),
                cov = tokens[3].toLong(),
                ft = tokens[5].toLong(),
                crashes = crashes,
            )
        }
    }

    val maxExecNr = statsNoTimeStamps.maxOfOrNull { it.execNr } ?: 0
    val stats = listOf(
        LibfuzzerLogEntry(0, 0, 0, 0, 0),
    ) + statsNoTimeStamps.map { it.addTimestamp(it.execNr * duration.inWholeSeconds / maxExecNr) }

    val rows = stats.joinToString(separator = "\n") { entry ->
        with(entry) { "$execNr,$timeSeconds,$cov,$ft,$crashes" }
    }
    return HEADER + rows
}
