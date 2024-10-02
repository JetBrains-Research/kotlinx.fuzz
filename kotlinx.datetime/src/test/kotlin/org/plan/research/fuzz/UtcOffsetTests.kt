package org.plan.research.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toJavaZoneOffset
import kotlinx.datetime.toKotlinUtcOffset
import org.plan.research.fuzz.utils.compareTest
import org.plan.research.fuzz.utils.isFine

class UtcOffsetTests {
    @FuzzTest(maxDuration = "30s")
    fun parseVsJava(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100).uppercase()
        compareTest(
            createKotlin = { UtcOffset.parse(s) },
            createJava = { java.time.ZoneOffset.of(s) },
            kotlinToJava = { it.toJavaZoneOffset() },
            javaToKotlin = { it.toKotlinUtcOffset() }
        )
    }

    @FuzzTest(maxDuration = "30s")
    fun parseCheckExceptions(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100)
        isFine { UtcOffset.parse(s) }
    }

    @FuzzTest(maxDuration = "30s")
    fun isoBasicParseCheckExceptions(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100)
        isFine { UtcOffset.Formats.ISO_BASIC.parse(s) }
    }

    @FuzzTest(maxDuration = "30s")
    fun fourDigitsParseCheckExceptions(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100)
        isFine { UtcOffset.Formats.FOUR_DIGITS.parse(s) }
    }
}