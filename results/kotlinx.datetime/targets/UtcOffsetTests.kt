package fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import fuzz.utils.compareTest
import fuzz.utils.isFine
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toJavaZoneOffset
import kotlinx.datetime.toKotlinUtcOffset

class UtcOffsetTests {
    @FuzzTest(maxDuration = "2h")
    fun parseVsJava(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100).uppercase()
        compareTest(
            createKotlin = { UtcOffset.parse(s) },
            createJava = { java.time.ZoneOffset.of(s) },
            kotlinToJava = { it.toJavaZoneOffset() },
            javaToKotlin = { it.toKotlinUtcOffset() }
        )
    }

    @FuzzTest(maxDuration = "2h")
    fun parseCheckExceptions(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100)
        isFine { UtcOffset.parse(s) }
    }

    @FuzzTest(maxDuration = "2h")
    fun isoBasicParseCheckExceptions(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100)
        isFine { UtcOffset.Formats.ISO_BASIC.parse(s) }
    }

    @FuzzTest(maxDuration = "2h")
    fun fourDigitsParseCheckExceptions(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100)
        isFine { UtcOffset.Formats.FOUR_DIGITS.parse(s) }
    }
}