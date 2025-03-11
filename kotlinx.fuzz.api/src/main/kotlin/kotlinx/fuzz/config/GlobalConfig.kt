package kotlinx.fuzz.config

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.math.max

interface GlobalConfig {
    val workDir: Path
    val reproducerDir: Path
    val instrument: List<String>
    val customHookExcludes: List<String>
    val hooks: Boolean
    val logLevel: LogLevel
    val regressionEnabled: Boolean
    val detailedLogging: Boolean
    val threads: Int
    val supportJazzerTargets: Boolean
}

class GlobalConfigImpl internal constructor(builder: KFuzzConfigBuilder) : GlobalConfig {
    override var workDir: Path by builder.KFuzzPropProvider(
        nameSuffix = "workDir",
        intoString = { it.absolutePathString() },
        fromString = { Path(it).absolute() },
    )
    override var logLevel: LogLevel by builder.KFuzzPropProvider(
        nameSuffix = "logLevel",
        intoString = { it.toString() },
        fromString = { LogLevel.valueOf(it) },
        default = LogLevel.WARN,
    )
    override var reproducerDir: Path by builder.KFuzzPropProvider(
        nameSuffix = "reproducerDir",
        intoString = { it.absolutePathString() },
        fromString = { Path(it).absolute() },
    )
    override var instrument: List<String> by builder.KFuzzPropProvider(
        nameSuffix = "instrument",
        intoString = { it.joinToString(",") },
        fromString = { it.split(",") },
    )
    override var customHookExcludes: List<String> by builder.KFuzzPropProvider(
        nameSuffix = "customHookExcludes",
        intoString = { it.joinToString(",") },
        fromString = { it.split(",") },
        default = emptyList(),
    )
    override var hooks: Boolean by builder.KFuzzPropProvider(
        nameSuffix = "enableHooks",
        intoString = { it.toString() },
        fromString = { it.toBooleanStrict() },
        default = true,
    )
    override var regressionEnabled: Boolean by builder.KFuzzPropProvider(
        nameSuffix = "regression",
        intoString = { it.toString() },
        fromString = { it.toBooleanStrict() },
        default = false,
    )
    override var detailedLogging: Boolean by builder.KFuzzPropProvider(
        nameSuffix = "detailedLogging",
        intoString = { it.toString() },
        fromString = { it.toBooleanStrict() },
        default = false,
    )
    override var threads: Int by builder.KFuzzPropProvider(
        nameSuffix = "threads",
        intoString = { it.toString() },
        fromString = { it.toInt() },
        validate = { require(it > 0) { "'threads' must be positive" } },
        default = max(1, Runtime.getRuntime().availableProcessors() / 2),
    )
    override var supportJazzerTargets: Boolean by builder.KFuzzPropProvider(
        nameSuffix = "supportJazzerTargets",
        intoString = { it.toString() },
        fromString = { it.toBooleanStrict() },
        default = false,
    )
}

enum class LogLevel {
    DEBUG, ERROR, INFO, TRACE, WARN;
}
