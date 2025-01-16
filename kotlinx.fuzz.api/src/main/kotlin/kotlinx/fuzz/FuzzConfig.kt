package kotlinx.fuzz

/**
 * Class that stores generals fuzzing configuration
 *
 * @param fuzzEngine - name of engine to be used
 * @param hooks - apply fuzzing instrumentation
 * @param instrument - glob patterns matching names of classes that should be instrumented for fuzzing
 * @param customHookExcludes - Glob patterns matching names of classes that should not be instrumented with hooks (custom and built-in)
 * @param maxSingleTargetFuzzTime - max time to fuzz single target in seconds
 */
data class FuzzConfig(
    val fuzzEngine: String,
    val hooks: Boolean,
    val instrument: List<String>,
    val customHookExcludes: List<String>,
    val maxSingleTargetFuzzTime: Int,
) {
    companion object {
        fun fromSystemProperties(): FuzzConfig {
            return FuzzConfig(
                fuzzEngine = System.getProperty("kotlinx.fuzz.engine", "jazzer"),
                hooks = System.getProperty("kotlinx.fuzz.hooks")?.toBoolean() ?: false,
                instrument = System.getProperty("kotlinx.fuzz.instrument")
                    ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty) ?: emptyList(),
                customHookExcludes = System.getProperty("kotlinx.fuzz.customHookExcludes")
                    ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty) ?: emptyList(),
                maxSingleTargetFuzzTime = System.getProperty("kotlinx.fuzz.maxSingleTargetFuzzTime", "10").toInt()
            )
        }
    }
}
