package kotlinx.fuzz.config

/**
 * Configuration for fuzzing. Can be obtained via static methods or [KFuzzConfigBuilder] directly.
 */
interface KFuzzConfig {
    val global: GlobalConfig
    val target: TargetConfig
    val engine: EngineConfig
    val coverage: CoverageConfig

    fun toPropertiesMap(): Map<String, String>

    companion object {
        const val PROPERTY_NAME_PREFIX = "kotlinx.fuzz."
        fun fromSystemProperties(): KFuzzConfigBuilder = KFuzzConfigBuilder(getSystemPropertiesMap())
        fun fromAnotherConfig(config: KFuzzConfig): KFuzzConfigBuilder = KFuzzConfigBuilder(config.toPropertiesMap())
    }
}
