package kotlinx.fuzz

import kotlinx.fuzz.config.KFConfig
import kotlinx.fuzz.config.TargetConfig
import kotlin.time.Duration
import org.junit.platform.commons.annotation.Testable

/**
 *  This annotation is used to mark targets for fuzzing.
 *  Global config parameters can be overloaded here. See [KFuzzConfig] for all parameters' description.
 *
 *  @param keepGoing how many bugs to discover before finishing fuzzing. Default: 1
 *  @param maxFuzzTime max time to fuzz this target, as a string that [kotlin.time.Duration.parse] can recognize.
 *  Default: "1m"
 *  @param instrument glob patterns matching names of classes that should be instrumented for fuzzing.
 *  Will be added to global config.
 *  @param customHookExcludes glob patterns matching names of classes that should NOT be instrumented with hooks.
 *  Will be added to global config.
 *  @param dumpCoverage whether fuzzer should generate jacoco `.exec` files
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Testable
annotation class KFuzzTest(
    val keepGoing: Long = TargetConfig.Defaults.KEEP_GOING,
    val maxFuzzTime: String = TargetConfig.Defaults.MAX_FUZZ_TIME_STRING,
    val instrument: Array<String> = [],
    val customHookExcludes: Array<String> = [],
    val dumpCoverage: Boolean = TargetConfig.Defaults.DUMP_COVERAGE,
)

fun KFConfig.addAnnotationParams(annotation: KFuzzTest): KFConfig = KFConfig.fromAnotherConfig(this)
    .editOverride {
        target.keepGoing = newUnlessDefault(
            old = this@addAnnotationParams.target.keepGoing,
            new = annotation.keepGoing,
            default = TargetConfig.Defaults.KEEP_GOING,
        )
        target.maxFuzzTime = newUnlessDefault(
            old = this@addAnnotationParams.target.maxFuzzTime,
            new = Duration.parse(annotation.maxFuzzTime),
            default = Duration.parse(TargetConfig.Defaults.MAX_FUZZ_TIME_STRING),
        )
        target.instrument = this@addAnnotationParams.target.instrument + annotation.instrument
        target.customHookExcludes = this@addAnnotationParams.target.customHookExcludes + annotation.customHookExcludes
        target.dumpCoverage = newUnlessDefault(
            old = this@addAnnotationParams.target.dumpCoverage,
            new = annotation.dumpCoverage,
            default = TargetConfig.Defaults.DUMP_COVERAGE,
        )
    }.build()

private fun <T> newUnlessDefault(old: T, new: T, default: T): T = if (new == default) old else new
