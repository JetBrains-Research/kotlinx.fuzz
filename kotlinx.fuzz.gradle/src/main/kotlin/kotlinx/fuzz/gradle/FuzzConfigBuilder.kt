package kotlinx.fuzz.gradle

import kotlinx.fuzz.FuzzConfig
import kotlinx.fuzz.FuzzConfig.Companion.toPropertiesMap
import kotlin.io.path.Path
import kotlin.properties.Delegates

class FuzzConfigBuilder private constructor() {
    var fuzzEngine: String = FuzzConfig.Companion.FUZZ_ENGINE_DEFAULT
    var hooks: Boolean = FuzzConfig.Companion.HOOKS_DEFAULT
    var keepGoing: Int = FuzzConfig.Companion.KEEP_GOING_DEFAULT
    var reportCoverage: Boolean = FuzzConfig.Companion.REPORT_COVERAGE_DEFAULT
    var resultDir: String? = null
    lateinit var instrument: List<String>
    var customHookExcludes: List<String> = FuzzConfig.Companion.CUSTOM_HOOK_EXCLUDES_DEFAULT
    var maxSingleTargetFuzzTime: Int by Delegates.notNull<Int>()

    fun build(): FuzzConfig {
        require(resultDir != null)
        return FuzzConfig(
            fuzzEngine = fuzzEngine,
            hooks = hooks,
            keepGoing = keepGoing,
            reportCoverage = reportCoverage,
            resultDir = Path(resultDir!!),
            instrument = instrument,
            customHookExcludes = customHookExcludes,
            maxSingleTargetFuzzTime = maxSingleTargetFuzzTime,
        )
    }

    companion object {
        internal fun build(block: FuzzConfigBuilder.() -> Unit): FuzzConfig =
            FuzzConfigBuilder().apply(block).build()

        internal fun writeToSystemProperties(block: FuzzConfigBuilder.() -> Unit) {
            build(block).toPropertiesMap().forEach { (key, value) ->
                System.setProperty(key, value)
            }
        }
    }
}