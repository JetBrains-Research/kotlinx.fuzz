@file:Suppress("WRONG_ORDER_IN_CLASS_LIKE_STRUCTURES", "KDOC_EXTRA_PROPERTY")

package kotlinx.fuzz.gradle

import kotlin.reflect.KProperty
import kotlinx.fuzz.config.KFuzzConfig
import kotlinx.fuzz.config.KFuzzConfigBuilder

/**
 * DSL for specifying fuzzing config. Sample usage:
 *
 * ```kotlin
 * fuzzConfig {
 *      workDir = Path("fuzz-workdir")
 *      maxFuzzTimePerTarget = 1.hour
 *      coverage {
 *          reportTypes = setOf(CoverageReportType.HTML)
 *      }
 * }
 * ```
 *
 * @property workDir Working directory for internal fuzzing files. Default: {buildDir}/fuzz
 * @property reproducerDir Directory for crash reproducers. Default: {workDir}/reproducers
 * @property hooks Whether to apply custom hooks (currently unsupported). Default: true
 * @property logLevel Sets the logging level for kotlinx.fuzz library. Default: WARN
 * @property detailedLogging Forwards logs from fuzzing engine. Default: false
 * @property threads How many threads to use for parallel fuzzing. Default: #cpu_cores / 2
 * @property maxFuzzTimePerTarget Max time to fuzz each @KFuzzTest. Default: 1 minute
 * @property keepGoing How many crashes to find before stopping fuzzing, or 0 for unlimited. Default: 0
 * @property instrument Which packages to instrument with coverage tracking. Should include your files.
 * @property customHookExcludes In which packages NOT to apply custom hooks. Default: none
 * @property dumpCoverage Whether to dump coverage data. Default: true
 * @property coverage Section for specifying coverage params. See [CoverageConfigDSL]
 * @property engine Section for specifying engine params (currently only Jazzer). See [JazzerConfigDSL]
 *
 * @see KFuzzConfig
 */
open class FuzzConfigDSL(
    projectProperties: Map<String, String>,
) {
    private val builder = KFuzzConfigBuilder(projectProperties)

    // ========== global ==========
    var workDir by KFConfigDelegate { global::workDir }
    var reproducerDir by KFConfigDelegate { global::reproducerDir }
    var hooks by KFConfigDelegate { global::hooks }
    var logLevel by KFConfigDelegate { global::logLevel }
    var detailedLogging by KFConfigDelegate { global::detailedLogging }
    var threads by KFConfigDelegate { global::threads }

    // ========== target ==========
    var maxFuzzTimePerTarget by KFConfigDelegate { target::maxFuzzTime }
    var keepGoing by KFConfigDelegate { target::keepGoing }

    // TODO: default to project packages?
    var instrument by KFConfigDelegate { global::instrument }
    var customHookExcludes by KFConfigDelegate { global::customHookExcludes }
    var dumpCoverage by KFConfigDelegate { target::dumpCoverage }
    private val builtConfig: KFuzzConfig by lazy { builder.build() }

    fun build(): KFuzzConfig = builtConfig

    // ========== engine ==========

    sealed interface EngineConfigDSL

    /**
     * @property libFuzzerRssLimit LibFuzzer rss limit parameter. Default: 0
     */
    inner class JazzerConfigDSL : EngineConfigDSL {
        var libFuzzerRssLimit by KFConfigDelegate { engine::libFuzzerRssLimitMb }
    }

    /**
     * TODO: no support for different engines yet. See [KFuzzConfigBuilder.KFuzzConfigImpl]
     */
    private val engineDSL = JazzerConfigDSL()
    fun engine(block: JazzerConfigDSL.() -> Unit) {
        engineDSL.block()
    }

    // ========== coverage ==========

    /**
     * @property reportTypes Which reports to generate. Default: HTML
     * @property includeDependencies Which dependencies to calculate coverage for. Default: none
     */
    @Suppress("USE_DATA_CLASS")
    inner class CoverageConfigDSL {
        var reportTypes by KFConfigDelegate { coverage::reportTypes }
        var includeDependencies by KFConfigDelegate { coverage::includeDependencies }
    }

    private val coverageDSL = CoverageConfigDSL()

    fun coverage(block: CoverageConfigDSL.() -> Unit) {
        coverageDSL.block()
    }

    // ========== internals ==========

    private inner class KFConfigDelegate<T : Any>(
        propertySelector: KFuzzConfigBuilder.KFuzzConfigImpl.() -> KProperty<T>,
    ) {
        private val kfProp = builder.getPropertyDelegate(propertySelector)

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
            kfProp.getValue(thisRef, property)

        // because editFallback is necessary, getValue() is not possible before building
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            builder.editFallback { kfProp.setValue(thisRef, property, value) }
        }
    }
}
