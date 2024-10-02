/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("RemoveRedundantQualifierName")

import kotlinx.datetime.*
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows


class Reproduce {

    @Test
    fun LocalDateUntil() {
        val firstDate = kotlinx.datetime.LocalDate(year = 0, monthNumber = 5, dayOfMonth = 31)
        val secondDate = kotlinx.datetime.LocalDate(year = 0, monthNumber = 4, dayOfMonth = 1)

        val ktPeriod = firstDate.periodUntil(secondDate)
        val jvPeriod = firstDate.toJavaLocalDate().until(secondDate.toJavaLocalDate())

        assertEquals(ktPeriod, jvPeriod.toKotlinDatePeriod())
    }

    @Test
    fun InstantMaxValues() {
        val maxSecond = 31556889864403199L + 1L
        println(Instant.fromEpochSeconds(maxSecond))
        println(java.time.Instant.ofEpochSecond(maxSecond))
    }

    @Test
    fun datePeriodParse() {
        val s = "P"
        assertThrows<Throwable> { kotlinx.datetime.DateTimePeriod.parse(s) }
        assertThrows<Throwable> { kotlinx.datetime.DatePeriod.parse(s) }

        assertThrows<Throwable> { java.time.Period.parse(s) }
        assertThrows<Throwable> { java.time.Duration.parse(s) }
        assertThrows<Throwable> { kotlin.time.Duration.parseIsoString(s) }
    }

    @Test
    fun instantFromString() {
        val s = "-211242-10-21t20:20:44+10"
        assertThrows<Throwable> { java.time.Instant.parse(s) }
        assertThrows<Throwable> { kotlinx.datetime.Instant.parse(s) }
    }

    @Test
    fun datePeriodToJavaPeriod() {
        val kotlinDatePeriod = kotlinx.datetime.DatePeriod(months = 88)
        val javaDatePeriod = java.time.Period.ofMonths(88)
        assertEquals(javaDatePeriod, kotlinDatePeriod.toJavaPeriod())
    }

    @Test
    fun utcOffsetParse() {
        val s = "+1"
        assertDoesNotThrow { java.time.ZoneOffset.of(s) }
        assertDoesNotThrow { kotlinx.datetime.UtcOffset.Companion.parse(s) }
    }

    @Test
    fun localDateParseVsIsoParse() {
        val s = "+" + "0".repeat(7) + "2222-07-22"
        assertThrows<Throwable> { kotlinx.datetime.LocalDate.parse(s) }
        assertThrows<Throwable> { kotlinx.datetime.LocalDate.Formats.ISO.parse(s) }
    }

    @Test
    fun localDateTimeParseVsIsoParse() {
        val s = "+" + "0".repeat(7) + "2020-08-30T18:43"
        assertThrows<Throwable> { kotlinx.datetime.LocalDateTime.parse(s) }
        assertThrows<Throwable> { kotlinx.datetime.LocalDateTime.Formats.ISO.parse(s) }
    }

    @OptIn(FormatStringsInDatetimeFormats::class)
    @Test
    fun localDateFormatByUnicode() {
        val s = "g]"
        assertThrows<IllegalArgumentException> { LocalDate.Format { byUnicodePattern(s) } }
    }
}
