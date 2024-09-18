package fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import fuzz.utils.compareTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.toJavaPeriod
import kotlinx.datetime.toKotlinDatePeriod
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.Period

class DatePeriodTests {
    @FuzzTest(maxDuration = "2h")
    fun convertToJava(data: FuzzedDataProvider) {

        fun assertJtPeriodNormalizedEquals(a: Period, b: Period) {
            assertEquals(a.days, b.days)
            assertEquals(a.months + a.years * 12, b.months + b.years * 12)
        }

        fun test(years: Int, months: Int, days: Int) {
            val ktPeriod = DatePeriod(years, months, days)
            val jtPeriod = Period.of(years, months, days)

            assertEquals(ktPeriod, jtPeriod.toKotlinDatePeriod())
            assertJtPeriodNormalizedEquals(jtPeriod, ktPeriod.toJavaPeriod())

            assertEquals(ktPeriod, jtPeriod.toString().let(DatePeriod::parse))
            assertJtPeriodNormalizedEquals(jtPeriod, ktPeriod.toString().let(Period::parse))
        }

        test(
            data.consumeInt(-1000, 1000),
            data.consumeInt(-1000, 1000),
            data.consumeInt(-1000, 1000)
        )
    }

    @FuzzTest(maxDuration = "2h")
    fun parseVsJava(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100)
        compareTest(
            createKotlin = { DatePeriod.parse(s) },
            createJava = { java.time.Period.parse(s) },
            kotlinToJava = { it.toJavaPeriod() },
            javaToKotlin = { it.toKotlinDatePeriod() },
        )
    }
}
