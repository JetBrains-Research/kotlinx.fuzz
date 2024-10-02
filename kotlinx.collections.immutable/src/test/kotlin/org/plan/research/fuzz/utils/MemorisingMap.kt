/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package org.plan.research.fuzz.utils

import kotlinx.collections.immutable.PersistentMap
import org.junit.jupiter.api.Assertions.assertEquals

class MemorisingMap(val history: MutableList<PersistentMap<Int, Int>>) {
    val last
        get() = history.last()

    val operations: MutableList<MapOperation> = mutableListOf()

    fun applyOperation(operation: MapOperation) {
        val nextList = operation.apply(last)
        operations += operation
        history += nextList
    }

    fun validate() {
        val hashMap = history[0].toMutableMap()
        assertEquals(history[0], hashMap)
        for (i in 0..operations.lastIndex) {
            val op = operations[i]
            val preMap = history[i]
            val postMap = history[i + 1]

            validateReverse(preMap, postMap, op)
            validateReplay(hashMap, postMap, op)
            validateInvariants(preMap, postMap, op)
        }
    }

    private fun validateReverse(
        preMap: PersistentMap<Int, Int>,
        postMap: PersistentMap<Int, Int>,
        operation: MapOperation
    ) {
        if (!validateReverse) return
        val reversed = operation.reverse(preMap, postMap) ?: return
        assertEquals(preMap, reversed)
    }

    private fun validateInvariants(
        preMap: PersistentMap<Int, Int>,
        postMap: PersistentMap<Int, Int>,
        operation: MapOperation
    ) {
        if (!validateInvariants) return
        operation.validate(preMap, postMap)
    }

    private fun validateReplay(
        hashMap: MutableMap<Int, Int>,
        postMap: PersistentMap<Int, Int>,
        operation: MapOperation
    ) {
        if (!validateReplay) return
        operation.apply(hashMap)
        assertEquals(postMap, hashMap)
    }
}