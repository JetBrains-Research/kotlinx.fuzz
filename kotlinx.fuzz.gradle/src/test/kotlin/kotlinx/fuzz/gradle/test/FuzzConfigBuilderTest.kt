package kotlinx.fuzz.gradle.test

import kotlinx.fuzz.config.KFuzzConfigBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds

object FuzzConfigBuilderTest {
    @Test
    fun `not initialize something`() {
        @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
        assertThrows<Throwable> { KFuzzConfigBuilder(emptyMap()).build() }
    }

    @Test
    fun `0 maxSingleTargetFuzzTime fails`() {
        assertThrows<IllegalArgumentException> {
            KFuzzConfigBuilder(emptyMap()).editOverride {
                target.maxFuzzTime = 0.seconds
                target.instrument = emptyList()
                global.workDir = Path("test")
                global.reproducerDir = Path("test")
            }.build()
        }
    }

    @Test
    fun `enough set`() {
        assertDoesNotThrow {
            KFuzzConfigBuilder(emptyMap()).editOverride {
                target.instrument = listOf("1", "2")
                target.maxFuzzTime = 30.seconds
                global.workDir = Path("test")
                global.reproducerDir = Path("test")
            }.build()
        }
    }

    @Test
    fun `all set`() {
        assertDoesNotThrow {
            KFuzzConfigBuilder(emptyMap()).editOverride {
                engine.apply {
                    libFuzzerRssLimitMb = 5
                    enableLogging = false
                }
                global.hooks = true
                target.keepGoing = 339
                target.instrument = listOf()
                target.customHookExcludes = listOf("exclude")
                target.maxFuzzTime = 1000.seconds
                global.workDir = Path("test")
                global.reproducerDir = Path("test")
            }.build()
        }
    }
}
