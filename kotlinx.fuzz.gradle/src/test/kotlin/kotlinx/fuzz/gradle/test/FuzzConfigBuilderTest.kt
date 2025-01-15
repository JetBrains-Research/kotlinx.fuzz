package kotlinx.fuzz.gradle.test

import kotlinx.fuzz.gradle.FuzzConfigBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

object FuzzConfigBuilderTest {
    @Test
    fun `not initialize something`() {
        assertThrows<Throwable> { FuzzConfigBuilder.build {} }
    }

    @Test
    fun `0 maxSingleTargetFuzzTime fails`() {
        assertThrows<IllegalArgumentException> {
            FuzzConfigBuilder.build {
                maxSingleTargetFuzzTime = 0
                instrument = emptyList()
            }
        }
    }

    @Test
    fun `enough set`() {
        assertDoesNotThrow {
            FuzzConfigBuilder.build {
                instrument = listOf("1", "2")
                maxSingleTargetFuzzTime = 30
            }
        }
    }

    @Test
    fun `all set`() {
        assertDoesNotThrow {
            FuzzConfigBuilder.build {
                fuzzEngine = "engine"
                hooks = true
                keepGoing = 339
                instrument = listOf()
                customHookExcludes = listOf("exclude")
                maxSingleTargetFuzzTime = 1000
            }
        }
    }
}