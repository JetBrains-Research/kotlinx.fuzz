package fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import fuzz.utils.compareTest
import fuzz.utils.isFine
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinTimeZone
import java.time.ZoneId

class TimeZoneTests {
    @FuzzTest(maxDuration = "2h")
    fun parseVsJava(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100)
        compareTest(
            createKotlin = { TimeZone.of(s) },
            createJava = { ZoneId.of(s) },
            kotlinToJava = { it.id.let { ZoneId.of(it) } },
            javaToKotlin = { it.toKotlinTimeZone() }
        )
    }

    @FuzzTest(maxDuration = "2h")
    fun parseCheckExceptions(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100)
        isFine { TimeZone.of(s) }
    }
}