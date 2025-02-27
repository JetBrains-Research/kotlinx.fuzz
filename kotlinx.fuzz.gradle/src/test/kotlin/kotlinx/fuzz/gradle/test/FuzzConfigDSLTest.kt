@file:Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")

package kotlinx.fuzz.gradle.test

import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.fuzz.config.KFuzzConfigBuilder
import kotlinx.fuzz.config.LogLevel
import kotlinx.fuzz.gradle.FuzzConfigDSL
import org.junit.jupiter.api.assertDoesNotThrow

object FuzzConfigDsltest {
    @Test
    fun basicTest() {
        val dsl = object : FuzzConfigDSL(emptyMap()) {}
        dsl.apply {
            workDir = Path(".")
            reproducerDir = Path(".")
            instrument = emptyList()
        }
        val actualConfig = dsl.build()
        val expectedConfig = KFuzzConfigBuilder(emptyMap()).editOverride {
            global.workDir = Path(".")
            global.reproducerDir = Path(".")
            global.instrument = emptyList()
        }.build()
        assertEquals(expectedConfig.global.workDir, actualConfig.global.workDir)
    }

    @Test
    fun allDSLParameters() {
        assertDoesNotThrow {
            val dsl = object : FuzzConfigDSL(emptyMap()) {}
            dsl.apply {
                workDir = Path(".")
                reproducerDir = Path(".")
                hooks = true
                logLevel = LogLevel.DEBUG
                detailedLogging = true
                threads = 5

                maxFuzzTimePerTarget = 1.seconds
                keepGoing = 5
                instrument = emptyList()
                customHookExcludes = emptyList()
                dumpCoverage = false

                engine {
                    libFuzzerRssLimit = 5
                }

                coverage {
                    reportTypes = emptySet()
                    includeDependencies = emptySet()
                }
            }
            dsl.build()
        }
    }
}
