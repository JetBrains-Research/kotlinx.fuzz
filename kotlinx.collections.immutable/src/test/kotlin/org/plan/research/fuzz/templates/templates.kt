package org.plan.research.fuzz.templates

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toPersistentList
import org.junit.jupiter.api.Assertions.assertTrue
import org.plan.research.fuzz.utils.*
import org.plan.research.fuzz.utils.HistoryList.Companion.historyList

inline fun persistentMapRandomOps(
    data: FuzzedDataProvider,
    toPersistent: Map<Int, Int>.() -> PersistentMap<Int, Int>
) {
    val firstMap = data.consumeInts(initSize)
        .asSequence().chunked(2).filter { it.size == 2 }
        .map { list -> list[0] to list[1] }
        .toMap()

    val memorisingMap = MemorisingMap(mutableListOf(firstMap.toPersistent()))

    val opsNum = data.consumeInt(10, 1000)
    repeat(opsNum) {
        val op = data.consumeMapOperation(memorisingMap.last)
        memorisingMap.applyOperation(op)
    }

    memorisingMap.validate()
}


inline fun persistentSetRandomOps(
    data: FuzzedDataProvider,
    toPersistent: Set<Int>.() -> PersistentSet<Int>,
) {
    val firstSet = data.consumeInts(initSize).toSet()

    val memorisingSet = MemorisingSet(mutableListOf(firstSet.toPersistent()))

    val opsNum = data.consumeInt(10, 1000)
    val ops = mutableListOf<SetOperation>()
    repeat(opsNum) {
        val op = data.consumeSetOperation(memorisingSet.last)
        ops += op
        memorisingSet.applyOperation(op)
    }

    memorisingSet.validate()
}

fun persistentListBubbleSort(ints: List<Int>) {
    val persistentHistory = historyList(ints.toTypedArray().toPersistentList())
    val listHistory = historyList(ints.toMutableList())
    listOf(1, 2, 3).sorted()

    bubbleSort(persistentHistory)
    bubbleSort(listHistory)

    assertTrue(persistentHistory == listHistory)
}

fun persistentListBubbleSort(ints: IntArray) {
    persistentListBubbleSort(ints.toList())
}


fun bubbleSort(historyList: HistoryList<Int>) {
    var done = false
    while (!done) {
        done = true
        val list = historyList.history.last()
        for (i in 0..<list.lastIndex) {
            if (list[i] > list[i + 1]) {
                done = false
                historyList.history.add(historyList.history.last().persistentSwap(i, i + 1))
            }
        }
    }
}
