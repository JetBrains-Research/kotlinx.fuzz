package kotlinx.fuzz.gradle.test

import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds
import kotlinx.fuzz.config.KFuzzConfigBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

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
                global.instrument = emptyList()
                global.workDir = Path("test")
                global.reproducerDir = Path("test")
            }.build()
        }
    }

    @Test
    fun `enough set`() {
        assertDoesNotThrow {
            KFuzzConfigBuilder(emptyMap()).editOverride {
                global.instrument = listOf("1", "2")
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
                }
                global.detailedLogging = false
                global.hooks = true
                target.keepGoing = 339
                global.instrument = listOf()
                global.customHookExcludes = listOf("exclude")
                target.maxFuzzTime = 1000.seconds
                global.workDir = Path("test")
                global.reproducerDir = Path("test")
            }.build()
        }
    }
}
