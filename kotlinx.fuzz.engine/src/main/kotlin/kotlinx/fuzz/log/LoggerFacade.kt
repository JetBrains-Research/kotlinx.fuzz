package kotlinx.fuzz.log

import kotlinx.fuzz.config.KFuzzConfig
import kotlinx.fuzz.config.LogLevel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

/**
 * Custom logger facade that uses slf4j service provider if available and falls back to StdoutLogger if not
 */
object LoggerFacade {
    val LOG_LEVEL by lazy {
        KFuzzConfig.fromSystemProperties()
            .global
            .logLevel
            .toSLF4JLevel()
    }
    private val isSlf4jAvailable: Boolean by lazy {
        val slf4jProviders = this::class.java.classLoader.getResource("org.slf4j.spi.SLF4JServiceProvider")
            ?.readText()
            .orEmpty()
            .split("\n")
            .filter { it.isNotBlank() }
        slf4jProviders.isNotEmpty()
    }

    fun getLogger(name: String): Logger = LoggerWrapper(
        when {
            isSlf4jAvailable -> LoggerFactory.getLogger(name)
            else -> StdoutLogger
        },
    )

    inline fun <reified T> getLogger(): Logger = getLogger(T::class.java.name)
}

private fun LogLevel.toSLF4JLevel(): Level = when (this) {
    LogLevel.TRACE -> Level.TRACE
    LogLevel.DEBUG -> Level.DEBUG
    LogLevel.INFO -> Level.INFO
    LogLevel.WARN -> Level.WARN
    LogLevel.ERROR -> Level.ERROR
}
