package kotlinx.fuzz.config

private const val NAME_PREFIX = "jazzer"

sealed interface EngineConfig

interface JazzerConfig : EngineConfig {
    val libFuzzerRssLimitMb: Int
}

class JazzerConfigImpl internal constructor(builder: KFuzzConfigBuilder) : JazzerConfig {
    override var libFuzzerRssLimitMb: Int by builder.KFuzzPropProvider(
        "$NAME_PREFIX.libFuzzerArgs.rssLimitMb",
        intoString = { it.toString() },
        fromString = { it.toInt() },
        validate = { require(it >= 0) { "rssLimit must be positive!" } },
        default = 0,
    )
}
