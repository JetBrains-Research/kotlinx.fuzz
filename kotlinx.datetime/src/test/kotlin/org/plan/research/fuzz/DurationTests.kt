package org.plan.research.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import org.plan.research.fuzz.utils.compareTest
import org.plan.research.fuzz.utils.copyj
import org.plan.research.fuzz.utils.isFine
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

class DurationTests {
    @FuzzTest(maxDuration = "30s")
    fun parseVsJava(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100).uppercase()
        compareTest(
            createKotlin = { Duration.parse(s) },
            createJava = { java.time.Duration.parse(s) },
            kotlinToJava = { it.copyj() },
            javaToKotlin = { it.toKotlinDuration() },
        )
    }

    @FuzzTest(maxDuration = "30s")
    fun parseIsoVsJava(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100).uppercase()
        compareTest(
            createKotlin = { Duration.parseIsoString(s) },
            createJava = { java.time.Duration.parse(s) },
            kotlinToJava = { it.copyj() },
            javaToKotlin = { it.toKotlinDuration() },
        )
    }

    @FuzzTest(maxDuration = "30s")
    fun parseCheckExceptions(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100)
        isFine { Duration.parse(s) }
    }

    @FuzzTest(maxDuration = "30s")
    fun parseIsoCheckExceptions(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100)
        isFine { Duration.parseIsoString(s) }
    }
}