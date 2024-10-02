/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package org.plan.research.fuzz.utils

import kotlinx.collections.immutable.PersistentSet
import org.junit.jupiter.api.Assertions.assertEquals


class MemorisingSet(val history: MutableList<PersistentSet<Int>>) {
    val last
        get() = history.last()

    val operations: MutableList<SetOperation> = mutableListOf()

    val builder = history[0].builder()

    fun applyOperation(operation: SetOperation) {
        val nextSet = operation.apply(last)
        operations += operation
        history += nextSet
    }

    fun validate() {
        val arrayList = history[0].toMutableSet()

        assertEquals(history[0], arrayList)
        assertEquals(history[0], builder)

        for (i in 0..operations.lastIndex) {
            val preSet = history[i]
            val postSet = history[i + 1]
            val op = operations[i]

            validateInvariants(preSet, postSet, op)
            validateReplay(arrayList, postSet, op)
            validateBuilder(builder, postSet, op)
        }
        builder.clear()
        builder.addAll(history.first())
    }

    private fun validateReplay(
        mutableSet: MutableSet<Int>,
        postSet: PersistentSet<Int>,
        op: SetOperation
    ) {
        if (!validateReplay) return
        op.apply(mutableSet)
        assertEquals(postSet, mutableSet)

    }


    private fun validateInvariants(
        preSet: PersistentSet<Int>,
        postSet: PersistentSet<Int>,
        operation: SetOperation
    ) {
        if (!validateInvariants) return
        operation.validateInvariants(preSet, postSet)
    }

    private fun validateBuilder(
        builder: MutableSet<Int>,
        postSet: PersistentSet<Int>,
        op: SetOperation
    ) {
        if (!validateBuilder) return
        op.apply(builder)
        assertEquals(postSet, builder)
    }

}