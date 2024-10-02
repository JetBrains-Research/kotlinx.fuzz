package org.plan.research.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.collections.immutable.toPersistentList
import org.plan.research.fuzz.utils.MAX_BUFFER_SIZE
import org.plan.research.fuzz.utils.MemorisingList
import org.plan.research.fuzz.utils.consumeListOperation
import org.plan.research.fuzz.utils.initSize

class SmallPersistentVectorTests {
    @FuzzTest(maxDuration = "2h")
    fun randomOps(data: FuzzedDataProvider) {
        val first = data.consumeInts(initSize).toList()
        val memorisingList = MemorisingList(mutableListOf(first.toPersistentList()))

        memorisingList.last.iterator()

        val opsNum = data.consumeInt(10, 1000)
        repeat(opsNum) {
            val op = data.consumeListOperation(memorisingList.last)
            memorisingList.applyOperation(op)
            if (memorisingList.last.size <= MAX_BUFFER_SIZE) {
                memorisingList.history.removeLast()
                memorisingList.operations.removeLast()
            }
        }
        memorisingList.validate()
    }

    @FuzzTest(maxDuration = "2h")
    fun bubbleSort(data: FuzzedDataProvider) = with(data) {
        org.plan.research.fuzz.templates.persistentListBubbleSort(consumeInts(MAX_BUFFER_SIZE))
    }
}