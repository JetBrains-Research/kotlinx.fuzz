/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package org.plan.research.fuzz.utils

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlinx.datetime.*
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.DateTimeFormatBuilder
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import java.time.ZoneId
import kotlin.time.Duration


fun FuzzedDataProvider.consumeDate(yearFrom: Int = -20000, yearTo: Int = 20000): LocalDate {
    val year = consumeInt(yearFrom, yearTo)
    val month = pickValue(Month.entries)
    val day = consumeInt(1, java.time.YearMonth.of(year, month).lengthOfMonth())
    return LocalDate(year, month.number, day)
}

fun FuzzedDataProvider.consumeDateTime(): LocalDateTime = consumeDate().atTime(
    consumeInt(0, 24 - 1),
    consumeInt(0, 60 - 1),
    consumeInt(0, 60 - 1),
    consumeInt(0, 1_000_000_000 - 1)
)

fun FuzzedDataProvider.consumeTime(): LocalTime {
    val hour = consumeInt(0, 24 - 1)
    val minute = consumeInt(0, 60 - 1)
    val second = consumeInt(0, 60 - 1)
    val nanosecond = consumeInt(0, 1_000_000_000 - 1)
    return LocalTime(hour, minute, second, nanosecond)
}

fun FuzzedDataProvider.consumeInstant(
    from: Long = -1_000_000_000_000,
    to: Long = 1_000_000_000_000,
    nanoFrom: Int = Int.MIN_VALUE,
    nanoTo: Int = Int.MAX_VALUE
): Instant {
    val seconds = consumeLong(from, to)
    val nanos = consumeInt(nanoFrom, nanoTo)
    return Instant.fromEpochSeconds(seconds, nanos)
}


internal val availableZoneIds = TimeZone.availableZoneIds.map { TimeZone.of(it) }.toTypedArray()
internal val availableFixedOffsetZoneIds =
    availableZoneIds.filterIsInstance<FixedOffsetTimeZone>().toTypedArray()

internal val availableNonFixedOffsetZoneIds =
    availableZoneIds.filter { it !is FixedOffsetTimeZone }.toTypedArray()

fun FuzzedDataProvider.consumeTimeZone(): TimeZone = pickValue(availableZoneIds)

fun FuzzedDataProvider.consumeNonFixedOffsetTimeZone(): TimeZone = pickValue(
    availableNonFixedOffsetZoneIds
)

fun FuzzedDataProvider.consumeFixedOffsetTimeZone(): FixedOffsetTimeZone =
    pickValue(availableFixedOffsetZoneIds)


fun LocalDate.copyj(): java.time.LocalDate = java.time.LocalDate.of(year, monthNumber, dayOfMonth)

fun LocalDateTime.copyj(): java.time.LocalDateTime =
    java.time.LocalDateTime.of(year, monthNumber, dayOfMonth, hour, minute, second, nanosecond)

fun TimeZone.copyj(): java.time.ZoneId = ZoneId.of(id)

fun LocalTime.copyj(): java.time.LocalTime =
    java.time.LocalTime.of(hour, minute, second, nanosecond)


fun Instant.copyj(): java.time.Instant =
    java.time.Instant.ofEpochSecond(epochSeconds, nanosecondsOfSecond.toLong())

fun Duration.copyj(): java.time.Duration = java.time.Duration.ofSeconds(inWholeSeconds)

private typealias FormatterOp = DateTimeFormatBuilder.WithDateTime.() -> Unit

//private val ops = listOf<FormatterOp>(
//    { amPmHour() },
//    { dayOfMonth() },
//    { monthNumber() },
//    { year() },
//    { second() },
//)

private val PADDING_VALS = Padding.entries.toTypedArray()
private val MONTH_NAMES = arrayOf(MonthNames.ENGLISH_FULL, MonthNames.ENGLISH_ABBREVIATED)

internal fun FuzzedDataProvider.consumePadding(): Padding = pickValue(PADDING_VALS)
internal fun FuzzedDataProvider.consumeMonthNames(): MonthNames = pickValue(MONTH_NAMES)

internal fun FuzzedDataProvider.consumeTimeFormat(): DateTimeFormat<LocalTime> {
    val opsNum = consumeInt(0, 20)
    val ops = List(opsNum) { consumeInt(0, 6) }
    return LocalTime.Format {
        ops.forEach {
            when (it) {
                0 -> amPmHour(consumePadding())
                1 -> second(consumePadding())
                2 -> minute(consumePadding())
                3 -> hour(consumePadding())
                4 -> amPmMarker(consumeString(10), consumeString(10))
                5 -> secondFraction(consumeInt(1, 9), consumeInt(1, 9))
                6 -> secondFraction(consumeInt(1, 9))
            }
        }
    }
}

internal fun FuzzedDataProvider.consumeDateTimeFormat(): DateTimeFormat<LocalDateTime> {
    val opsNum = consumeInt(0, 20)
    val ops = List(opsNum) { consumeInt(0, 11) }
    return LocalDateTime.Format {
        ops.forEach {
            when (it) {
                0 -> amPmHour(consumePadding())
                1 -> monthNumber(consumePadding())
                2 -> year(consumePadding())
                3 -> second(consumePadding())
                4 -> minute(consumePadding())
                5 -> hour(consumePadding())
                6 -> dayOfMonth(consumePadding())
                7 -> monthName(consumeMonthNames())
                8 -> amPmMarker(consumeString(10), consumeString(10))
                9 -> secondFraction(consumeInt(1, 9), consumeInt(1, 9))
                10 -> secondFraction(consumeInt(1, 9))
                11 -> yearTwoDigits(consumeInt())
            }
        }
    }
}

internal fun FuzzedDataProvider.consumeDateFormat(): DateTimeFormat<LocalDate> {
    val opsNum = consumeInt(0, 20)
    val ops = List(opsNum) { consumeInt(0, 4) }
    return LocalDate.Format {
        ops.forEach {
            when (it) {
                0 -> dayOfMonth(consumePadding())
                1 -> monthNumber(consumePadding())
                2 -> year(consumePadding())
                3 -> monthName(consumeMonthNames())
                4 -> yearTwoDigits(consumeInt())
            }
        }
    }
}
