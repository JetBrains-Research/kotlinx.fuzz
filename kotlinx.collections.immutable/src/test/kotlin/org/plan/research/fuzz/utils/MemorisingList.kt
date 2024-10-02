/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package org.plan.research.fuzz.utils

import kotlinx.collections.immutable.PersistentList
import org.junit.jupiter.api.Assertions.assertEquals


class MemorisingList(val history: MutableList<PersistentList<Int>>) {
    val last
        get() = history.last()

    val operations: MutableList<ListOperation> = mutableListOf()

    val builder = history[0].builder()

    fun applyOperation(operation: ListOperation) {
        val nextList = operation.apply(last)
        operations += operation
        history += nextList
    }

    fun validate() {
        val arrayList = history[0].toMutableList()

        assertEquals(history[0], arrayList)
        assertEquals(history[0], builder)

        for (i in 0..operations.lastIndex) {
            val preList = history[i]
            val postList = history[i + 1]
            val op = operations[i]

            validateInvariants(preList, postList, op)
            validateReverse(preList, postList, op)
            validateReplay(arrayList, postList, op)
            validateBuilder(builder, postList, op)
        }
        builder.clear()
        builder.addAll(history.first())
    }

    private fun validateReplay(
        arrayList: MutableList<Int>,
        postList: PersistentList<Int>,
        op: ListOperation
    ) {
        if (!validateReplay) return
        op.apply(arrayList)
        assertEquals(postList, arrayList)

    }

    private fun validateReverse(
        preList: PersistentList<Int>,
        postList: PersistentList<Int>,
        op: ListOperation
    ) {
        if (!validateReverse) return
        val reversed = op.reverse(postList) ?: return
        assertEquals(preList, reversed)
    }


    private fun validateInvariants(
        preList: PersistentList<Int>,
        postList: PersistentList<Int>,
        operation: ListOperation
    ) {
        if (!validateInvariants) return
        operation.validateInvariants(preList, postList)
    }

    private fun validateBuilder(
        builder: MutableList<Int>,
        postList: PersistentList<Int>,
        op: ListOperation
    ) {
        if (!validateBuilder) return
        op.apply(builder)
        assertEquals(postList, builder)
    }

}