package org.plan.research.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinLocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.plan.research.fuzz.utils.compareTest
import org.plan.research.fuzz.utils.consumeDate
import org.plan.research.fuzz.utils.consumeTime
import org.plan.research.fuzz.utils.isFine

class LocalTimeTests {

    @FuzzTest(maxDuration = "2h")
    fun convertToJava(data: FuzzedDataProvider) {
        fun test(ktTime: LocalTime) {
            val jtTime =
                with(ktTime) { java.time.LocalTime.of(hour, minute, second, nanosecond) }

            assertEquals(ktTime, jtTime.toKotlinLocalTime())
            assertEquals(jtTime, ktTime.toJavaLocalTime())

            assertEquals(ktTime, LocalTime.parse(jtTime.toString()))
            assertEquals(jtTime, ktTime.toString().let(java.time.LocalTime::parse))
        }

        test(data.consumeTime())
    }

    @FuzzTest(maxDuration = "2h")
    fun parseVsJava(data: FuzzedDataProvider): Unit = with(data) {
        val s = consumeString(100)
        compareTest(
            createKotlin = { LocalTime.parse(s) },
            createJava = { java.time.LocalTime.parse(s) },
            javaToKotlin = { it.toKotlinLocalTime() },
            kotlinToJava = { it.toJavaLocalTime() }
        )
    }

    @FuzzTest(maxDuration = "2h")
    fun isoParseVsJava(data: FuzzedDataProvider): Unit = with(data) {
        val s = consumeString(100)
        compareTest(
            createKotlin = { LocalTime.Formats.ISO.parse(s) },
            createJava = { java.time.LocalTime.parse(s) },
            javaToKotlin = { it.toKotlinLocalTime() },
            kotlinToJava = { it.toJavaLocalTime() }
        )
    }

    @FuzzTest(maxDuration = "2h")
    fun parseCheckExceptions(data: FuzzedDataProvider): Unit = with(data) {
        val s = consumeString(100)
        isFine { LocalTime.parse(s) }
    }

    @FuzzTest(maxDuration = "2h")
    fun isoParseCheckExceptions(data: FuzzedDataProvider): Unit = with(data) {
        val s = consumeString(100)
        isFine { LocalTime.Formats.ISO.parse(s) }
    }

    @FuzzTest(maxDuration = "2h")
    fun atDateCheckExceptions(data: FuzzedDataProvider): Unit = with(data) {
        val time = consumeTime()
        val date = consumeDate()
        isFine { time.atDate(date) }
    }
}
