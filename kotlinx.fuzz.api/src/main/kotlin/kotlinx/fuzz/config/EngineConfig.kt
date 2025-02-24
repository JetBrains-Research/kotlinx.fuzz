package kotlinx.fuzz.config

sealed interface EngineConfig

interface JazzerConfig : EngineConfig {
    val libFuzzerRssLimitMb: Int
    val enableLogging: Boolean
}

class JazzerConfigImpl internal constructor(builder: KFuzzConfigBuilder) : JazzerConfig {
    override var libFuzzerRssLimitMb by builder.KFuzzPropProvider<Int>(
        "jazzer.libFuzzerArgs.rssLimitMb",
        intoString = { it.toString() },
        fromString = { it.toInt() },
        validate = { require(it >= 0) { "rssLimit must be positive!" } },
        default = 0,
    )
    override var enableLogging by builder.KFuzzPropProvider<Boolean>(
        "jazzer.enableLogging",
        intoString = { it.toString() },
        fromString = { it.toBooleanStrict() },
        default = false,
    )
}
