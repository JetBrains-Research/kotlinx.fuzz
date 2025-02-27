package kotlinx.fuzz.junit.test

import kotlin.reflect.KFunction
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds
import kotlinx.fuzz.ConfigurationException
import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod
import org.junit.platform.testkit.engine.EngineTestKit

class AnnotationsTest {
    @BeforeEach
    fun setup() {
        writeToSystemProperties {
            maxSingleTargetFuzzTime = 10.seconds
            instrument = listOf("kotlinx.fuzz.test.**")
            workDir = kotlin.io.path.createTempDirectory("fuzz-test")
            reproducerPath = workDir.resolve("reproducers")
        }
    }

    @KFuzzTest(maxFuzzTime = "not even a duration")
    fun invalidConfig(@Suppress("UNUSED_PARAMETER", "unused") data: KFuzzer) {
        error("unreachable")
    }

    @KFuzzTest(keepGoing = 2)
    fun overriddenConfig(data: KFuzzer) {
        // A _hacky_ way to test if config params are actually overridden.
        // It's based on probability of fuzzer finding the second exception before first being basically 0.
        // If keepGoing=2 is not set, we will only get the first error.
        if (!data.boolean()) {
            error("first error")
        }
        if (data.int() * 5 == 25) {
            error("second error")
        }
    }

    @Test
    fun testInvalidConfig() {
        val result = runMethodFuzz(AnnotationsTest::invalidConfig)
        assertEquals(TestExecutionResult.Status.FAILED, result.status)
        val exception = result.throwable!!.get().cause!!  // parent exception is from junit
        assertIs<ConfigurationException>(
            exception,
            message = "wrong exception type\n${exception.stackTraceToString()}",
        )
    }

    @Test
    fun testOverriddenConfig() {
        val result = runMethodFuzz(AnnotationsTest::overriddenConfig)
        assertEquals(TestExecutionResult.Status.FAILED, result.status)
        val exception = result.throwable!!.get()  // no parent exception
        assertEquals(
            "second error", exception.message!!,
            message = "wrong exception message\n${exception.stackTraceToString()}",
        )
    }

    private fun runMethodFuzz(method: KFunction<*>): TestExecutionResult {
        val methodFQN = "${AnnotationsTest::class.qualifiedName!!}#${method.name}(kotlinx.fuzz.KFuzzer)"
        return EngineTestKit
            .engine("kotlinx.fuzz")  // if wrong, will fail to load engine
            .selectors(selectMethod(methodFQN))
            .execute()
            .allEvents()
            .executions()
            .list()
            .first()
            .terminationInfo
            .executionResult!!
    }
}
