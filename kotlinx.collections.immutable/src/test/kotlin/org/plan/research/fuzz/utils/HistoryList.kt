/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("DataClassPrivateConstructor")

package org.plan.research.fuzz.utils

import kotlinx.collections.immutable.PersistentList

data class HistoryList<T> private constructor(val history: MutableList<List<T>>) {
    companion object {
        fun <T> historyList(list: List<T>) = HistoryList(mutableListOf(list.toList()))
    }

    operator fun get(i: Int, n: Int): T {
        return history[i][n]
    }

    operator fun set(i: Int, n: Int, value: T) {
        history.add(history[i].persSet(n, value))
    }

    operator fun set(n: Int, value: T) {
        history.add(history.last().persSet(n, value))
    }
}

fun <T> List<T>.persSet(i: Int, value: T): List<T> {
    if (this is PersistentList) {
        return this.set(i, value)
    }

    val copy = this.toMutableList()
    copy[i] = value
    return copy
}

fun <T> List<T>.persistentSwap(i: Int, j: Int): List<T> {
    if (i == j) return this
    val t = this[i]
    if (this is PersistentList) {
        return this.set(i, this[j]).set(j, t)
    }
    val newList = this.toMutableList()
    newList[i] = this[j]
    newList[j] = t
    return newList
}