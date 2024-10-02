package org.plan.research.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.collections.immutable.toPersistentList
import org.junit.jupiter.api.Assertions.assertEquals
import org.plan.research.fuzz.utils.consumeListOperation

class PersistentVectorBuilderTests {

    @FuzzTest(maxDuration = "2h")
    fun listBuilderRandomOps(data: FuzzedDataProvider) = with(data) {
        val initSize = consumeInt(0, 1024)
        val init = data.consumeInts(initSize)
        val opsNum = consumeInt(0, 1024)
        val builder = init.toTypedArray().toPersistentList().builder()
        val arrayList = init.toMutableList()
        repeat(opsNum) {
            val op = data.consumeListOperation(builder)
            op.apply(builder)
            op.apply(arrayList)
            assertEquals(arrayList, builder)
        }
    }
}