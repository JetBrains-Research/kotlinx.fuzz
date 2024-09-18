package fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import fuzz.utils.*
import kotlinx.datetime.*
import org.junit.jupiter.api.Assertions.assertEquals

class LocalDateTimeTests {

    @FuzzTest(maxDuration = "2h")
    fun parseCheckExceptions(data: FuzzedDataProvider): Unit = with(data) {
        val s = consumeString(100)
        isFine { LocalDateTime.parse(s) }
    }

    @FuzzTest(maxDuration = "2h")
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

    @FuzzTest(maxDuration = "2h")
    fun convertToInstant(data: FuzzedDataProvider) = with(data) {
        val d = consumeDateTime()
        val tz = consumeTimeZone()
        compareTest(
            firstBlock = { d },
            secondBlock = { d.toInstant(tz).toLocalDateTime(tz) },
        )
    }

    @FuzzTest(maxDuration = "2h")
    fun parseVsIsoParse(data: FuzzedDataProvider): Unit = with(data) {
        val s = consumeString(100)
        compareTest(
            { LocalDateTime.parse(s) },
            { LocalDateTime.Formats.ISO.parse(s) }
        )
    }

    @FuzzTest(maxDuration = "2h")
    fun parseVsJava(data: FuzzedDataProvider) {
        val s = data.consumeString(100)
        compareTest(
            createKotlin = { LocalDateTime.parse(s) },
            createJava = { java.time.LocalDateTime.parse(s) },
            kotlinToJava = { it.copyj() },
            javaToKotlin = { it.toKotlinLocalDateTime() }
        )
    }

    @FuzzTest(maxDuration = "2h")
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