/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import fuzz.utils.consumeFormat
import fuzz.utils.isFine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.DayOfWeekNames


class FormattersTest {
    @FuzzTest(maxDuration = "2h")
    fun localDateTime(data: FuzzedDataProvider): Unit = with(data) {
        val format = consumeFormat()
        val s = consumeAsciiString(200)
        format.parseOrNull(s)
    }


    @FuzzTest(maxDuration = "2h")
    fun localDateMyFormat(data: FuzzedDataProvider): Unit = with(data) {
        val format = LocalDate.Format {
            dayOfMonth()
            dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
            monthNumber()
        }
        val s = consumeString(100)
        isFine { format.parse(s) }
    }
}
