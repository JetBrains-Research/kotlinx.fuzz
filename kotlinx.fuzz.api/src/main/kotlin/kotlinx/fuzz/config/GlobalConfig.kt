package kotlinx.fuzz.config

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString

interface GlobalConfig {
    val workDir: Path
    val reproducerDir: Path
    val hooks: Boolean
    val logLevel: LogLevel
    val regressionEnabled: Boolean
}

class GlobalConfigImpl internal constructor(builder: KFuzzConfigBuilder) : GlobalConfig {
    override var workDir by builder.KFuzzPropProvider<Path>(
        nameSuffix = "workDir",
        fromString = { Path(it) },
        intoString = { it.absolutePathString() },
    )
    override var logLevel by builder.KFuzzPropProvider<LogLevel>(
        nameSuffix = "logLevel",
        intoString = { it.toString() },
        fromString = { LogLevel.valueOf(it) },
        default = LogLevel.WARN,
    )
    override var reproducerDir by builder.KFuzzPropProvider<Path>(
        nameSuffix = "reproducerDir",
        intoString = { it.absolutePathString() },
        fromString = { Path(it).absolute() },
    )
    override var hooks by builder.KFuzzPropProvider<Boolean>(
        nameSuffix = "enableHooks",
        intoString = { it.toString() },
        fromString = { it.toBooleanStrict() },
        default = true,
    )
    override var regressionEnabled by builder.KFuzzPropProvider<Boolean>(
        nameSuffix = "regression",
        intoString = { it.toString() },
        fromString = { it.toBooleanStrict() },
        default = false,
    )
}

enum class LogLevel {
    DEBUG, ERROR, INFO, TRACE, WARN;
}
