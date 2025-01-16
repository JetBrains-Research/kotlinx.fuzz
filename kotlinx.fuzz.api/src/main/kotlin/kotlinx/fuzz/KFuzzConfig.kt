package kotlinx.fuzz

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Class that stores generals fuzzing configuration
 *
 * @param fuzzEngine - name of engine to be used
 * @param hooks - apply fuzzing instrumentation
 * @param instrument - glob patterns matching names of classes that should be instrumented for fuzzing
 * @param customHookExcludes - Glob patterns matching names of classes that should not be instrumented with hooks (custom and built-in)
 * @param maxSingleTargetFuzzTime - max time to fuzz single target
 */
data class KFuzzConfig(
    val fuzzEngine: String,
    val hooks: Boolean,
    val instrument: List<String>,
    val customHookExcludes: List<String>,
    val maxSingleTargetFuzzTime: Duration,
) {
    companion object {
        fun fromSystemProperties(): KFuzzConfig = KFuzzConfig(
            fuzzEngine = System.getProperty("kotlinx.fuzz.engine", "jazzer"),
            hooks = System.getProperty("kotlinx.fuzz.hooks").toBooleanOrFalse(),
            instrument = System.getProperty("kotlinx.fuzz.instrument")?.asList().orEmpty(),
            customHookExcludes = System.getProperty("kotlinx.fuzz.customHookExcludes")?.asList().orEmpty(),
            maxSingleTargetFuzzTime = System.getProperty("kotlinx.fuzz.maxSingleTargetFuzzTime", "10").toInt().seconds,
        )
    }
}
