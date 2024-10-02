package org.plan.research.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.datetime.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.plan.research.fuzz.utils.*

class LocalDateTimeTests {

    @FuzzTest(maxDuration = "30s")
    fun parseCheckExceptions(data: FuzzedDataProvider): Unit = with(data) {
        val s = consumeString(100)
        isFine { LocalDateTime.parse(s) }
    }

    @FuzzTest(maxDuration = "30s")
    fun convertToJava(data: FuzzedDataProvider) {
        fun test(ktDateTime: LocalDateTime) {
            val jtDateTime = with(ktDateTime) {
                java.time.LocalDateTime.of(
                    year,
                    month,
                    dayOfMonth,
                    hour,
                    minute,
                    second,
                    nanosecond
                )
            }

            assertEquals(ktDateTime, jtDateTime.toKotlinLocalDateTime())
            assertEquals(jtDateTime, ktDateTime.toJavaLocalDateTime())

            assertEquals(ktDateTime, LocalDateTime.parse(jtDateTime.toString()))
            assertEquals(jtDateTime, ktDateTime.toString().let(java.time.LocalDateTime::parse))
        }

        test(data.consumeDateTime())
    }

    @FuzzTest(maxDuration = "30s")
    fun convertToInstant(data: FuzzedDataProvider) = with(data) {
        val d = consumeDateTime()
        val tz = consumeTimeZone()
        compareTest(
            firstBlock = { d },
            secondBlock = { d.toInstant(tz).toLocalDateTime(tz) },
        )
    }

    @FuzzTest(maxDuration = "30s")
    fun parseVsIsoParse(data: FuzzedDataProvider): Unit = with(data) {
        val s = consumeString(100)
        compareTest(
            { LocalDateTime.parse(s) },
            { LocalDateTime.Formats.ISO.parse(s) }
        )
    }

    @FuzzTest(maxDuration = "30s")
    fun parseVsJava(data: FuzzedDataProvider) {
        val s = data.consumeString(100)
        compareTest(
            createKotlin = { LocalDateTime.parse(s) },
            createJava = { java.time.LocalDateTime.parse(s) },
            kotlinToJava = { it.copyj() },
            javaToKotlin = { it.toKotlinLocalDateTime() }
        )
    }

    @FuzzTest(maxDuration = "30s")
    fun isoParseVsJava(data: FuzzedDataProvider) {
        val s = data.consumeString(100)
        compareTest(
            createKotlin = { LocalDateTime.parse(s) },
            createJava = { java.time.LocalDateTime.parse(s) },
            kotlinToJava = { it.copyj() },
            javaToKotlin = { it.toKotlinLocalDateTime() }
        )
    }
}