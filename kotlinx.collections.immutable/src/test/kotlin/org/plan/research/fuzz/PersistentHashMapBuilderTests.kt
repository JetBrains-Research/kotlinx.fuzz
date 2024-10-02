package org.plan.research.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.collections.immutable.persistentHashMapOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.plan.research.fuzz.utils.consumeMapOperation

class PersistentHashMapBuilderTests {
    @FuzzTest(maxDuration = "2h")
    fun randomOpsVsHashMap(data: FuzzedDataProvider) {
        val firstMap = data.consumeInts(1000)
            .asSequence().chunked(2).filter { it.size == 2 }
            .map { list -> list[0] to list[1] }
            .toMap()

        val builder = persistentHashMapOf<Int, Int>().builder().apply { putAll(firstMap) }
        val hashMap = hashMapOf<Int, Int>().apply { putAll(firstMap) }

        assertEquals(hashMap, builder)

        val opsNum = data.consumeInt(10, 1000)
        val ops = mutableListOf<org.plan.research.fuzz.utils.MapOperation>()
        repeat(opsNum) {
            val op = data.consumeMapOperation(builder)
            ops += op
            op.apply(builder)
            op.apply(hashMap)
            assertEquals(hashMap, builder)
        }
    }
}