package fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import fuzz.utils.compareTest
import fuzz.utils.copyj
import fuzz.utils.isFine
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

class DurationTests {
    @FuzzTest(maxDuration = "2h")
    fun parseVsJava(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100).uppercase()
        compareTest(
            createKotlin = { Duration.parse(s) },
            createJava = { java.time.Duration.parse(s) },
            kotlinToJava = { it.copyj() },
            javaToKotlin = { it.toKotlinDuration() },
        )
    }

    @FuzzTest(maxDuration = "2h")
    fun parseIsoVsJava(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100).uppercase()
        compareTest(
            createKotlin = { Duration.parseIsoString(s) },
            createJava = { java.time.Duration.parse(s) },
            kotlinToJava = { it.copyj() },
            javaToKotlin = { it.toKotlinDuration() },
        )
    }

    @FuzzTest(maxDuration = "2h")
    fun parseCheckExceptions(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100)
        isFine { Duration.parse(s) }
    }

    @FuzzTest(maxDuration = "2h")
    fun parseIsoCheckExceptions(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100)
        isFine { Duration.parseIsoString(s) }
    }
}