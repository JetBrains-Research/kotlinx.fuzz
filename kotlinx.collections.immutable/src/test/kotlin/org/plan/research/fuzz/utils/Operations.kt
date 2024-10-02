/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package org.plan.research.fuzz.utils

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet

sealed interface EmptyOperation
sealed interface ListOperation {
    fun PersistentList<Int>.applyInternal(): PersistentList<Int>

    fun MutableList<Int>.applyInternal()

    fun apply(list: PersistentList<Int>): PersistentList<Int> =
        tryOr(list) {
            val next = list.applyInternal()
            validateInvariants(list, next)
            return next
        }

    fun apply(list: MutableList<Int>) {
        tryOrNull { list.applyInternal() }
//        validate(list, next)
//        return next
    }


    fun validateInvariants(preList: List<Int>, postList: List<Int>)

    fun reverse(list: PersistentList<Int>): PersistentList<Int>? =
        reverseOperation?.apply(list)

    fun reverse(list: MutableList<Int>): MutableList<Int>? = TODO()


    val reverseOperation: ListOperation? get() = null
    val canReverse get() = reverseOperation != null
}

sealed interface MapOperation {
    fun PersistentMap<Int, Int>.applyInternal(): PersistentMap<Int, Int>
    fun MutableMap<Int, Int>.applyInternal()

    fun apply(map: PersistentMap<Int, Int>): PersistentMap<Int, Int> = map.applyInternal()
    fun apply(map: MutableMap<Int, Int>) = map.applyInternal()

    fun validate(preMap: Map<Int, Int>, postMap: Map<Int, Int>)

    fun reverse(
        preMap: PersistentMap<Int, Int>,
        postMap: PersistentMap<Int, Int>
    ): PersistentMap<Int, Int>?
}

sealed interface SetOperation {
    fun PersistentSet<Int>.applyInternal(): PersistentSet<Int>
    fun MutableSet<Int>.applyInternal()

    fun apply(set: PersistentSet<Int>): PersistentSet<Int> = set.applyInternal()
    fun apply(set: MutableSet<Int>) = set.applyInternal()

    fun validateInvariants(preSet: PersistentSet<Int>, postSet: PersistentSet<Int>)
}