package kotlinx.fuzz

import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlinx.fuzz.config.KFuzzConfigBuilder

class KFuzzConfigTest {
    @Test
    fun testBasic() {
        val props = mapOf(
            "kotlinx.fuzz.workDir" to "tmp",
            "kotlinx.fuzz.reproducerDir" to "tmp2",
            "kotlinx.fuzz.instrument" to "",
        )
        val config = KFuzzConfigBuilder(props)
            .editFallback {
                target.maxFuzzTime = Duration.parse("10s")
            }
            .build()
        val actualWorkDirString = config.global.workDir.fileName.toString()
        assertEquals("tmp", actualWorkDirString)
        assertEquals(Duration.parse("10s"), config.target.maxFuzzTime)
    }

    @Test
    fun testEditAndCopy() {
        val props = mapOf(
            "kotlinx.fuzz.workDir" to "tmp",
            "kotlinx.fuzz.reproducerDir" to "tmp2",
            "kotlinx.fuzz.instrument" to "",
            "kotlinx.fuzz.maxFuzzTimePerTarget" to "10s",
        )
        val config = KFuzzConfigBuilder(props)
            .editFallback { global.workDir = Path("bad") }
            .build()
        val configClone = KFuzzConfigBuilder.fromAnotherConfig(config)
            .editOverride { target.maxFuzzTime = Duration.parse("5s") }
            .build()
        val actualWorkDirString = configClone.global.workDir.fileName.toString()
        assertEquals("tmp", actualWorkDirString)
        assertEquals(Duration.parse("10s"), config.target.maxFuzzTime)
        assertEquals(Duration.parse("5s"), configClone.target.maxFuzzTime)
    }
}
