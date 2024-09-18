/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import fuzz.utils.*
import kotlinx.datetime.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration

private const val billiard = 1_000_000_000L

class InstantTests {

    @FuzzTest(maxDuration = "2h")
    fun instantArithmetic(data: FuzzedDataProvider) = with(data) {
        val instant = Instant.fromEpochMilliseconds(consumeInstant().toEpochMilliseconds())
        val diffMillis = consumeLong(-1_000_000_000, 1_000_000_000)
        val diff = diffMillis.milliseconds

        val nextInstant =
            (instant.toEpochMilliseconds() + diffMillis).let { Instant.fromEpochMilliseconds(it) }

        assertEquals(diff, nextInstant - instant)
        assertEquals(nextInstant, instant + diff)
        assertEquals(instant, nextInstant - diff)
    }

    @FuzzTest(maxDuration = "2h")
    fun instantArithmeticCombined(data: FuzzedDataProvider) = with(data) {
        val instant = Instant.fromEpochMilliseconds(consumeInstant().toEpochMilliseconds())
        val diffSeconds = consumeLong(-billiard, billiard)
        val diffNanos = consumeLong(-billiard, billiard)

        val diff = diffSeconds.seconds + diffNanos.nanoseconds


        val nextInstant = Instant.fromEpochSeconds(
            instant.epochSeconds + diffSeconds,
            instant.nanosecondsOfSecond + diffNanos
        )
//                (instant.toEpochMilliseconds() ).let { Instant.fromEpochMilliseconds(it) }

        assertEquals(diff, nextInstant - instant)
        assertEquals(nextInstant, instant + diff)
        assertEquals(instant, nextInstant - diff)
    }

    @FuzzTest(maxDuration = "2h")
    fun instantArithmeticNano(data: FuzzedDataProvider) = with(data) {
        val instant = consumeInstant()
        val diffMillis = consumeLong(1000, 1_000_000_000)
        val diff = diffMillis.milliseconds

        val nextInstant =
            (instant.toEpochMilliseconds() + diffMillis).let { Instant.fromEpochMilliseconds(it) }

        assertEquals(diff, nextInstant - instant)
        assertEquals(nextInstant, instant + diff)
        assertEquals(instant, nextInstant - diff)

//            println("this: $instant, next: $nextInstant, diff: ${diff.toIsoString()}")
    }

    @FuzzTest(maxDuration = "2h")
    fun diffInvariant(data: FuzzedDataProvider) = with(data) {
        val millis1 = consumeLong(-2_000_000_000_000L, 2_000_000_000_000L)
        val millis2 = consumeLong(-2_000_000_000_000L, 2_000_000_000_000L)
        val instant1 = Instant.fromEpochMilliseconds(millis1)
        val instant2 = Instant.fromEpochMilliseconds(millis2)

        val diff = instant1.periodUntil(instant2, TimeZone.currentSystemDefault())
        val instant3 = instant1.plus(diff, TimeZone.currentSystemDefault())

        assertEquals(instant2, instant3)
    }

    @FuzzTest(maxDuration = "2h")
    fun diffInvariantSameAsDate(data: FuzzedDataProvider) = with(data) {
        val millis1 = consumeLong(-2_000_000_000_000L, 2_000_000_000_000L)
        val millis2 = consumeLong(-2_000_000_000_000L, 2_000_000_000_000L)
//            with(consumeTimeZone()) TZ@{
        with(TimeZone.UTC) TZ@{
            val date1 = Instant.fromEpochMilliseconds(millis1).toLocalDateTime().date
            val date2 = Instant.fromEpochMilliseconds(millis2).toLocalDateTime().date
            val instant1 = date1.atStartOfDayIn(this@TZ)
            val instant2 = date2.atStartOfDayIn(this@TZ)

            val diff1 = instant1.periodUntil(instant2, this@TZ)
            val diff2 = date1.periodUntil(date2)

            assertEquals(diff1, diff2)
        }
    }

    @FuzzTest(maxDuration = "2h")
    fun convertToJava(data: FuzzedDataProvider) {
        fun test(seconds: Long, nanosecond: Int) {
            val ktInstant = Instant.fromEpochSeconds(seconds, nanosecond.toLong())
            val jtInstant = java.time.Instant.ofEpochSecond(seconds, nanosecond.toLong())

            assertEquals(ktInstant, jtInstant.toKotlinInstant())
            assertEquals(jtInstant, ktInstant.toJavaInstant())

            assertEquals(ktInstant, Instant.parse(jtInstant.toString()))
            assertEquals(jtInstant, ktInstant.toString().let(java.time.Instant::parse))
        }

        val seconds = data.consumeLong(-1_000_000_000_000, 1_000_000_000_000)
        val nanos = data.consumeInt()
        test(seconds, nanos)
    }

    @FuzzTest(maxDuration = "2h")
    fun diffVsJavaDiff(data: FuzzedDataProvider) = with(data) {
        val kfirst = consumeInstant()
        val ksecond = consumeInstant()

        val jfirst = kfirst.copyj()
        val jsecond = ksecond.copyj()

        compareTest(
            firstBlock = { kfirst.until(ksecond, DateTimeUnit.SECOND) },
            secondBlock = { jfirst.until(jsecond, ChronoUnit.SECONDS) },
        )
    }

    @FuzzTest(maxDuration = "2h")
    fun diffVsJavaDiffWithZoneId(data: FuzzedDataProvider) = with(data) {
        val kfirst = consumeInstant()
        val ksecond = consumeInstant()

        val ktz = consumeTimeZone()
        val jtz = ktz.toJavaZoneId()

        val jfirst = kfirst.copyj().atZone(jtz)
        val jsecond = ksecond.copyj().atZone(jtz)


        compareTest(
            firstBlock = { kfirst.until(ksecond, DateTimeUnit.SECOND, ktz) },
            secondBlock = { jfirst.until(jsecond, ChronoUnit.SECONDS) },
        )
    }

    @FuzzTest(maxDuration = "2h")
    fun diffNoNanos(data: FuzzedDataProvider) = with(data) {
        val first = consumeInstant(nanoFrom = 0, nanoTo = 0)
        val second = consumeInstant(nanoFrom = 0, nanoTo = 0)

        compareTest(
            createKotlin = { second - first },
            createJava = { java.time.Duration.between(first.copyj(), second.copyj()) },
            kotlinToJava = { it.copyj() },
            javaToKotlin = { it.toKotlinDuration() }
        )
    }

    @FuzzTest(maxDuration = "2h")
    fun parseCheckException(data: FuzzedDataProvider): Unit = with(data) {
        val s = consumeString(100)
        isFine { Instant.parse(s) }
    }

    @FuzzTest(maxDuration = "2h")
    fun fromMillisecondsVsJava(data: FuzzedDataProvider) {
        val milliseconds = data.consumeLong()
        compareTest(
            createKotlin = { Instant.fromEpochMilliseconds(milliseconds) },
            createJava = { java.time.Instant.ofEpochMilli(milliseconds) },
            kotlinToJava = { it.toJavaInstant() },
            javaToKotlin = { it.toKotlinInstant() }
        )
    }

    @FuzzTest(maxDuration = "2h")
    fun fromSecondsVsJava(data: FuzzedDataProvider) {
        val seconds = data.consumeLong(Long.MIN_VALUE / 1000L, Long.MAX_VALUE / 1000L)
        val nanos = data.consumeLong()
        compareTest(
            createKotlin = { Instant.fromEpochSeconds(seconds, nanos) },
            createJava = { java.time.Instant.ofEpochSecond(seconds, nanos) },
            kotlinToJava = { it.toJavaInstant() },
            javaToKotlin = { it.toKotlinInstant() }
        )
    }

    @FuzzTest(maxDuration = "2h")
    fun parseVsJava(data: FuzzedDataProvider) {
        val s = data.consumeString(100)
        compareTest(
            createKotlin = { Instant.parse(s) },
            createJava = { java.time.Instant.parse(s) },
            kotlinToJava = { it.copyj() },
            javaToKotlin = { it.toKotlinInstant() }
        )
    }
}
