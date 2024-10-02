package org.plan.research.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.collections.immutable.toPersistentHashSet
import org.junit.jupiter.api.Assertions.assertEquals
import org.plan.research.fuzz.utils.consumeSetOperation

class PersistentHashSetBuilderTests {
    @FuzzTest(maxDuration = "2h")
    fun randomOpsVsOrderedMap(data: FuzzedDataProvider) {
        val firstSet = data.consumeInts(1000).toSet()

        val builder = firstSet.toPersistentHashSet().builder()
        val hashMap = firstSet.toMutableSet()

        assertEquals(hashMap, builder)

        val opsNum = data.consumeInt(10, 1000)
        repeat(opsNum) {
            val op = data.consumeSetOperation(builder)
            op.apply(builder)
            op.apply(hashMap)
            assertEquals(hashMap, builder)
        }
    }
}