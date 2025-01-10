package kotlinx.fuzz

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.test.Test

class LoggerTest {
    val log = KotlinLogging.logger { }

    @Test
    fun loggerTest() {
        log.debug { "Test" }
    }
}