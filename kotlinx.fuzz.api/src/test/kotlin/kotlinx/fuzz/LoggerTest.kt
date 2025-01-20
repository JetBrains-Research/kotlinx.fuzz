package kotlinx.fuzz

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class LoggerTest {
    val log = KotlinLogging.logger { }

    @Test
    fun loggerTest() {
        val logFile = File("kotlinx.fuzz.log")
        logFile.writeText("")
        log.debug { "Test" }
        Thread.sleep(100) // We need to wait so that file updates
        val newLines = logFile.readLines()
        assertTrue { newLines.last().endsWith("[DEBUG][kotlinx.fuzz.LoggerTest] - Test") }
    }
}