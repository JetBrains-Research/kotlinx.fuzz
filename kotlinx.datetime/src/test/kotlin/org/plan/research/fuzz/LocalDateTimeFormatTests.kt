package org.plan.research.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import org.plan.research.fuzz.utils.compareTest
import org.plan.research.fuzz.utils.consumeDateTimeFormat
import org.plan.research.fuzz.utils.isFine

class LocalDateTimeFormatTests {
    @OptIn(FormatStringsInDatetimeFormats::class)
    @FuzzTest(maxDuration = "30s")
    fun byUnicodePattern(data: FuzzedDataProvider): Unit = with(data) {
        val s = consumeString(100)
        try {
            LocalDateTime.Format { byUnicodePattern(s) }
        } catch (_: IllegalArgumentException) {
        } catch (_: java.lang.UnsupportedOperationException) {
        }
    }

    @OptIn(FormatStringsInDatetimeFormats::class)
    @FuzzTest(maxDuration = "30s")
    fun byUnicodePatternVsJava(data: FuzzedDataProvider): Unit = with(data) {
        val pattern = consumeString(20)
        val inputs = List(10) { consumeString(100) }
        compareTest(
            createKotlin = {
                val format = LocalDateTime.Format { byUnicodePattern(pattern) }
                inputs.map { format.parse(it) }
            },
            createJava = {
                val format = java.time.format.DateTimeFormatter.ofPattern(pattern)
                inputs.map { java.time.LocalDateTime.parse(it, format) }
            },
            kotlinToJava = { it.map(LocalDateTime::toJavaLocalDateTime) },
            javaToKotlin = { it.map(java.time.LocalDateTime::toKotlinLocalDateTime) }
        )
    }


    @FuzzTest(maxDuration = "30s")
    fun randomFormatAndParse(data: FuzzedDataProvider): Unit = with(data) {
        val s = consumeString(100)
        isFine {
            val format = consumeDateTimeFormat()
            format.parse(s)
        }
    }
}