package fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import fuzz.utils.compareTest
import fuzz.utils.consumeDate
import fuzz.utils.copyj
import fuzz.utils.isFine
import kotlinx.datetime.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.Period

class LocalDateTests {
    @FuzzTest(maxDuration = "2h")
    fun fromDaysVsJava(data: FuzzedDataProvider) {
        val epochDay = data.consumeInt()
        compareTest(
            createKotlin = { LocalDate.fromEpochDays(epochDay) },
            createJava = { java.time.LocalDate.ofEpochDay(epochDay.toLong()) },
            kotlinToJava = { it.toJavaLocalDate() },
            javaToKotlin = { it.toKotlinLocalDate() }
        )
    }

    @FuzzTest(maxDuration = "2h")
    fun convertToJava(data: FuzzedDataProvider) {
        fun test(ktDate: LocalDate) {
            val jtDate = with(ktDate) { java.time.LocalDate.of(year, month, dayOfMonth) }

            assertEquals(ktDate, jtDate.toKotlinLocalDate())
            assertEquals(jtDate, ktDate.toJavaLocalDate())

            assertEquals(ktDate, LocalDate.parse(jtDate.toString()))
            assertEquals(jtDate, ktDate.toString().let(java.time.LocalDate::parse))
        }

        test(data.consumeDate())
    }

    @FuzzTest(maxDuration = "2h")
    fun parseCheckExceptions(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100)
        isFine { LocalDate.Formats.ISO.parse(s) }
    }

    @FuzzTest(maxDuration = "2h")
    fun parseIsoBasicCheckExceptions(data: FuzzedDataProvider): Unit = with(data) {
        val s = consumeString(100)
        val format = LocalDate.Formats.ISO_BASIC
        isFine { format.parseOrNull(s) }
    }

    @FuzzTest(maxDuration = "2h")
    fun parseWithDictVsJava(data: FuzzedDataProvider) {
        val len = data.consumeInt(1, 20)
        val chars = (0..9).toList().map { it.toString() } + "-"
        val s = List(len) { data.pickValue(chars) }.joinToString(separator = "")

        compareTest(
            createKotlin = { LocalDate.parse(s) },
            createJava = { java.time.LocalDate.parse(s) },
            kotlinToJava = { it.copyj() },
            javaToKotlin = { it.toKotlinLocalDate() },
        )
    }

    @FuzzTest(maxDuration = "2h")
    fun isoParseVsJava(data: FuzzedDataProvider) {
        val s = data.consumeString(100)
        compareTest(
            createKotlin = { LocalDate.Formats.ISO.parse(s) },
            createJava = { java.time.LocalDate.parse(s) },
            kotlinToJava = { it.toJavaLocalDate() },
            javaToKotlin = { it.toKotlinLocalDate() }
        )
    }

    @FuzzTest(maxDuration = "2h")
    fun parseVsJava(data: FuzzedDataProvider) {
        val s = data.consumeAsciiString(100)
        compareTest(
            createKotlin = { LocalDate.parse(s) },
            createJava = { java.time.LocalDate.parse(s) },
            kotlinToJava = { it.copyj() },
            javaToKotlin = { it.toKotlinLocalDate() },
        )
    }

    @FuzzTest(maxDuration = "2h")
    fun periodUntilVsJava(data: FuzzedDataProvider) {
        val mod = 0 //5000
        val a = data.consumeDate(-mod, mod)
        val b = data.consumeDate(-mod, mod)

        val kotlinRes = runCatching<DatePeriod> { a.periodUntil(b) }
        val javaRes = runCatching<Period> { a.copyj().until(b.copyj()) }

        assertEquals(kotlinRes.isSuccess, javaRes.isSuccess)

        if (kotlinRes.isFailure || javaRes.isFailure) return

        val kotlinVal = kotlinRes.getOrThrow()
        val javaVal = javaRes.getOrThrow()

        val javaFromKotlin = kotlinVal.toJavaPeriod()
        val kotlinFromJava = javaVal.toKotlinDatePeriod()

        assertEquals(kotlinVal, kotlinFromJava)
        assertEquals(javaVal, javaFromKotlin)
    }

    @FuzzTest(maxDuration = "2h")
    fun periodUntilNonNegativeVsJava(data: FuzzedDataProvider) {
        val aa = data.consumeDate()
        val bb = data.consumeDate()

        val a = if (aa < bb) aa else bb
        val b = if (aa < bb) bb else aa

        val kotlinRes = runCatching<DatePeriod> { a.periodUntil(b) }
        val javaRes = runCatching<Period> { a.copyj().until(b.copyj()) }

        assertEquals(kotlinRes.isSuccess, javaRes.isSuccess)

        if (kotlinRes.isFailure || javaRes.isFailure) return

        val kotlinVal = kotlinRes.getOrThrow()
        val javaVal = javaRes.getOrThrow()

        val javaFromKotlin = kotlinVal.toJavaPeriod()
        val kotlinFromJava = javaVal.toKotlinDatePeriod()

        assertEquals(kotlinVal, kotlinFromJava)
        assertEquals(javaVal, javaFromKotlin)
    }
}
