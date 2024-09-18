package fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import fuzz.utils.consumeTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinLocalTime
import org.junit.jupiter.api.Assertions.assertEquals

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
}