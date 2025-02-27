package kotlinx.fuzz.config

import kotlin.time.Duration

interface TargetConfig {
    val maxFuzzTime: Duration
    val keepGoing: Long
    val dumpCoverage: Boolean

    object Defaults {
        const val MAX_FUZZ_TIME_STRING = "1m"
        const val KEEP_GOING = 0L
        const val DUMP_COVERAGE = true
    }
}

class TargetConfigImpl internal constructor(builder: KFuzzConfigBuilder) : TargetConfig {
    override var maxFuzzTime: Duration by builder.KFuzzPropProvider(
        nameSuffix = "maxFuzzTimePerTarget",
        intoString = { it.toString() },
        fromString = { Duration.parse(it) },
        validate = { require(it.isPositive()) { "maxFuzzTimePerTarget must be positive" } },
        default = Duration.parse(TargetConfig.Defaults.MAX_FUZZ_TIME_STRING),
    )
    override var keepGoing: Long by builder.KFuzzPropProvider(
        nameSuffix = "keepGoing",
        intoString = { it.toString() },
        fromString = { it.toLong() },
        validate = { require(it >= 0) { "keepGoing must be non-negative" } },
        default = TargetConfig.Defaults.KEEP_GOING,
    )
    override var dumpCoverage: Boolean by builder.KFuzzPropProvider(
        nameSuffix = "dumpCoverage",
        intoString = { it.toString() },
        fromString = { it.toBooleanStrict() },
        default = TargetConfig.Defaults.DUMP_COVERAGE,
    )
}
