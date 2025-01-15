package kotlinx.fuzz.test

import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer
import kotlinx.fuzz.gradle.FuzzConfigBuilder
import kotlinx.fuzz.gradle.junit.KotlinxFuzzJunitEngine
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.testkit.engine.EngineTestKit

object EngineTest {
    @BeforeEach
    fun setup() {
        FuzzConfigBuilder.writeToSystemProperties {
            maxSingleTargetFuzzTime = 10
            instrument = listOf("kotlinx.fuzz.test.**")
        }
    }

    @Test
    fun `one pass one fail`() {
        EngineTestKit
            .engine(KotlinxFuzzJunitEngine())
            .selectors(selectClass(SimpleFuzzTest::class.java))
            .execute()
            .testEvents()
            .assertStatistics { it.started(2).succeeded(1).failed(1) }
    }

    object SimpleFuzzTest {
        @KFuzzTest
        fun `failure test`(data: KFuzzer) {
            if (data.consumeBoolean()) {
                error("Expected failure")
            }
        }

        @KFuzzTest
        fun `success test`(data: KFuzzer) {
        }
    }
}