package org.plan.research.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.datetime.DateTimePeriod
import org.plan.research.fuzz.utils.isFine

class DateTimePeriodTests {
    @FuzzTest(maxDuration = "30s")
    fun parseCheckExceptions(data: FuzzedDataProvider) = with(data) {
        val s = consumeString(100)
        isFine { DateTimePeriod.parse(s) }
    }
}