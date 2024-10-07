package org.plan.research.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinLocalTime
import org.plan.research.fuzz.utils.compareTest
import org.plan.research.fuzz.utils.consumeTimeFormat
import org.plan.research.fuzz.utils.isFine

class LocalTimeFormatTests {
    @OptIn(FormatStringsInDatetimeFormats::class)
    @FuzzTest(maxDuration = "2h")
    fun byUnicodePattern(data: FuzzedDataProvider): Unit = with(data) {
        val s = consumeString(100)
        try {
            LocalTime.Format { byUnicodePattern(s) }
        } catch (_: IllegalArgumentException) {
        } catch (_: java.lang.UnsupportedOperationException) {
        }
    }

    @OptIn(FormatStringsInDatetimeFormats::class)
    @FuzzTest(maxDuration = "2h")
    fun byUnicodePatternVsJava(data: FuzzedDataProvider): Unit = with(data) {
        val pattern = consumeString(20)
        val inputs = List(10) { consumeString(100) }
        compareTest(
            createKotlin = {
                val format = LocalTime.Format { byUnicodePattern(pattern) }
                inputs.map { format.parse(it) }
            },
            createJava = {
                val format = java.time.format.DateTimeFormatter.ofPattern(pattern)
                inputs.map { java.time.LocalTime.parse(it, format) }
            },
            kotlinToJava = { it.map(LocalTime::toJavaLocalTime) },
            javaToKotlin = { it.map(java.time.LocalTime::toKotlinLocalTime) }
        )
    }


    @FuzzTest(maxDuration = "2h")
    fun randomFormatAndParse(data: FuzzedDataProvider): Unit = with(data) {
        val s = consumeString(100)
        isFine {
            val format = consumeTimeFormat()
            format.parse(s)
        }
    }
}