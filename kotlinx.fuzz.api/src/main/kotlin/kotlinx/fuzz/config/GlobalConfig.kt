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
    val customHookClasses: MutableSet<String>
    val logLevel: LogLevel
    val detailedLogging: Boolean
    val threads: Int
    val supportJazzerTargets: Boolean
    val reproducerType: ReproducerType
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
        fromString = { LogLevel.valueOf(it.uppercase()) },
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
        fromString = { it.nonEmptySplit(",") },
        default = emptyList(),
    )
    override val customHookClasses: MutableSet<String> by builder.KFuzzPropProvider(
        nameSuffix = "customHookClasses",
        intoString = { it.joinToString(",") },
        fromString = { it.nonEmptySplit(",").toMutableSet() },
        default = mutableSetOf(),
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
    override var reproducerType: ReproducerType by builder.KFuzzPropProvider(
        nameSuffix = "reproducerType",
        intoString = { it.toString() },
        fromString = { ReproducerType.valueOf(it.uppercase()) },
        default = ReproducerType.LIST_BASED_INLINE,
    )
}

enum class LogLevel {
    DEBUG, ERROR, INFO, TRACE, WARN;
}

/**
 * Types of reproducers that are described [here](docs/Crash reproduction.md).
 * LIST_BASED_NO_INLINE --- create a List<Any?> based KFuzzer and call user's method
 * LIST_BASED_INLINE --- create a List<Any?> based KFuzzer and inline user's method
 */
enum class ReproducerType {
    LIST_BASED_INLINE, LIST_BASED_NO_INLINE
}
