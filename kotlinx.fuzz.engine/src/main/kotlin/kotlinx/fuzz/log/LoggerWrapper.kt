package kotlinx.fuzz.log

import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.event.Level

/**
 * slf4j logger wrapper, that only prints the output if kotlinx.fuzz logging is enabled
 */
internal class LoggerWrapper(
    private val logger: Logger,
) : Logger by logger {
    private fun isLevelEnabled(level: Level): Boolean {
        return LoggerFacade.LOG_LEVEL.toInt() <= level.toInt()
    }

    override fun isTraceEnabled(): Boolean = isLevelEnabled(Level.TRACE)
    override fun isTraceEnabled(marker: Marker?): Boolean = isLevelEnabled(Level.TRACE)
    override fun isDebugEnabled(): Boolean = isLevelEnabled(Level.DEBUG)
    override fun isDebugEnabled(marker: Marker?): Boolean = isLevelEnabled(Level.DEBUG)
    override fun isInfoEnabled(): Boolean = isLevelEnabled(Level.INFO)
    override fun isInfoEnabled(marker: Marker?): Boolean = isLevelEnabled(Level.INFO)
    override fun isWarnEnabled(): Boolean = LoggerFacade.LOG_LEVEL.toInt() <= Level.WARN.toInt()
    override fun isWarnEnabled(marker: Marker?): Boolean = LoggerFacade.LOG_LEVEL.toInt() <= Level.WARN.toInt()
    override fun isErrorEnabled(): Boolean = LoggerFacade.LOG_LEVEL.toInt() <= Level.ERROR.toInt()
    override fun isErrorEnabled(marker: Marker?): Boolean = isLevelEnabled(Level.ERROR)
}
