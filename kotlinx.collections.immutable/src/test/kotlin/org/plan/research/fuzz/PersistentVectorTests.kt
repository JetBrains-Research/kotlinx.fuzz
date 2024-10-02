package org.plan.research.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.collections.immutable.toPersistentList
import org.plan.research.fuzz.utils.MAX_BUFFER_SIZE
import org.plan.research.fuzz.utils.MemorisingList
import org.plan.research.fuzz.utils.consumeListOperation
import org.plan.research.fuzz.utils.forceConsumeInts

class PersistentVectorTests {
    @FuzzTest(maxDuration = "2h")
    fun randomOps(data: FuzzedDataProvider) = with(data) {
        val first = consumeInts(1000).toList()
        val memorisingList = MemorisingList(mutableListOf(first.toPersistentList()))

        val opsNum = consumeInt(0, 1000)
        repeat(opsNum) {
            val op = consumeListOperation(memorisingList.last)
            memorisingList.applyOperation(op)
            if (memorisingList.last.size > MAX_BUFFER_SIZE) {
                memorisingList.history.removeLast()
                memorisingList.operations.removeLast()
            }
        }
        memorisingList.validate()
    }

    @FuzzTest(maxDuration = "2h")
    fun bubbleSort(data: FuzzedDataProvider) = with(data) {
        org.plan.research.fuzz.templates.persistentListBubbleSort(
            forceConsumeInts(
                consumeInt(
                    MAX_BUFFER_SIZE + 1,
                    1000
                )
            )
        )
    }
}