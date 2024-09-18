/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package fuzz.utils

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlinx.datetime.*
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.DateTimeFormatBuilder
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


val availableZoneIds = TimeZone.availableZoneIds.map { TimeZone.of(it) }.toTypedArray()

fun FuzzedDataProvider.consumeTimeZone(): TimeZone = pickValue(availableZoneIds)


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


internal fun FuzzedDataProvider.consumeFormat(): DateTimeFormat<LocalDateTime> {
    val opsNum = consumeInt(0, 20)
    val ops = List(opsNum) { consumeInt(0, 4) }
    return LocalDateTime.Format {
        ops.forEach {
            when (it) {
                0 -> amPmHour()
                1 -> dayOfMonth()
                2 -> monthNumber()
                3 -> year()
                4 -> second()
            }
        }
    }
}
