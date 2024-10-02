/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */


package org.plan.research.fuzz.utils

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.reflect.full.isSubclassOf


private fun Boolean.toInt(): Int = if (this) 1 else 0

data class Add(val element: Int) : ListOperation,
    EmptyOperation, SetOperation {
    override fun MutableList<Int>.applyInternal() {
        this.add(element)
    }

    override fun PersistentList<Int>.applyInternal(): PersistentList<Int> = this.add(element)

    override fun validateInvariants(preList: List<Int>, postList: List<Int>) {
        assertTrue(postList.last() == element)
        assertTrue(preList.size + 1 == postList.size)
    }

    override val reverseOperation get() = RemoveLast
    override fun PersistentSet<Int>.applyInternal(): PersistentSet<Int> = this.add(element)

    override fun MutableSet<Int>.applyInternal() {
        this.add(element)
    }

    override fun validateInvariants(preSet: PersistentSet<Int>, postSet: PersistentSet<Int>) {
        assertTrue(postSet.contains(element))
        assertEquals(postSet.size - preSet.size, (element !in preSet).toInt())
    }

}

data class AddAt(val index: Int, val element: Int) : ListOperation,
    EmptyOperation {
    override fun MutableList<Int>.applyInternal() {
        this.add(index, element)
    }

    override fun PersistentList<Int>.applyInternal(): PersistentList<Int> =
        this.add(index, element)

    override fun validateInvariants(preList: List<Int>, postList: List<Int>) {
        require(postList.size == preList.size + 1)
        require(postList[index] == element)
    }

    override val reverseOperation: ListOperation
        get() = RemoveAt(index)
}

data class AddAll(val elements: Collection<Int>) : ListOperation,
    EmptyOperation, SetOperation {
    override fun MutableList<Int>.applyInternal() {
        addAll(elements)
    }


    override fun PersistentList<Int>.applyInternal(): PersistentList<Int> =
        this.addAll(elements.toList())

    override fun validateInvariants(preList: List<Int>, postList: List<Int>) {
        require(postList.size == preList.size + elements.size)
        require(postList.subList(preList.size, postList.size) == elements.toList())
    }

    override fun PersistentSet<Int>.applyInternal(): PersistentSet<Int> {
        return addAll(elements)
    }

    override fun MutableSet<Int>.applyInternal() {
        this.addAll(elements)
    }

    override fun validateInvariants(preSet: PersistentSet<Int>, postSet: PersistentSet<Int>) {
        require(postSet.size == preSet.size + elements.toSet().count { it !in preSet })
        require(postSet.containsAll(elements))
    }

}

data class AddAllAt(val index: Int, val elements: Collection<Int>) : ListOperation,
    EmptyOperation {
    override fun MutableList<Int>.applyInternal() {
        addAll(index, elements)
    }

    override fun PersistentList<Int>.applyInternal(): PersistentList<Int> =
        this.addAll(index, elements.toList())

    override fun validateInvariants(preList: List<Int>, postList: List<Int>) {
        require(postList.size == preList.size + elements.size)
    }

}

data class RemoveAt(val index: Int) : ListOperation {
    override fun MutableList<Int>.applyInternal() {
        removeAt(index)
    }


    override fun PersistentList<Int>.applyInternal(): PersistentList<Int> =
        this.removeAt(index)


    override fun validateInvariants(preList: List<Int>, postList: List<Int>) {
        require(postList.size + 1 == preList.size)
    }
}

data class Set(val index: Int, val element: Int) : ListOperation {
    override fun MutableList<Int>.applyInternal() {
        this[index] = element
    }

    override fun PersistentList<Int>.applyInternal(): PersistentList<Int> {
        return this.set(index, element)
    }

    override fun validateInvariants(preList: List<Int>, postList: List<Int>) {
        require(postList[index] == element)
    }
}

data object RemoveLast : ListOperation {
    override fun MutableList<Int>.applyInternal() {
        this.removeLast()
    }

    override fun PersistentList<Int>.applyInternal(): PersistentList<Int> {
        return this.removeAt(this.lastIndex)
    }

    override fun validateInvariants(preList: List<Int>, postList: List<Int>) {
        assertEquals(preList.subList(0, preList.lastIndex), postList)
    }
}

data class RemoveAllPredicate(val values: kotlin.collections.Set<Int>) :
    ListOperation {
    override fun PersistentList<Int>.applyInternal(): PersistentList<Int> {
        return removeAll { values.contains(it) }
    }

    override fun MutableList<Int>.applyInternal() {
        removeAll { values.contains(it) }
    }

    override fun validateInvariants(preList: List<Int>, postList: List<Int>) {
        assertEquals(-1, postList.indexOfFirst { values.contains(it) })
    }

}


private val LIST_OPERATIONS = ListOperation::class.sealedSubclasses
//private val LIST_OPERATIONS = listOf(AddAll::class, RemoveAllPredicate::class)

private val EMPTY_LIST_OPERATIONS =
    LIST_OPERATIONS.filter { it.isSubclassOf(EmptyOperation::class) }

fun FuzzedDataProvider.consumeIntList(): List<Int> {
    val size = consumeInt(0, 10)
    return List(size) { consumeInt() }
}

fun FuzzedDataProvider.consumeRemoveIndex(list: List<*>): Int = consumeInt(0, list.lastIndex)

fun FuzzedDataProvider.consumeAddIndex(list: List<*>): Int = consumeInt(0, list.size)

fun FuzzedDataProvider.consumeListOperation(list: List<Int>): ListOperation {
    val operations = if (list.isEmpty()) EMPTY_LIST_OPERATIONS else LIST_OPERATIONS

    return when (val op = pickValue(operations)) {
        Add::class -> consumeAdd()
        AddAt::class -> consumeAddAt(list)
        AddAll::class -> consumeAddAll()
        AddAllAt::class -> consumeAddAllAt(list)
        RemoveAt::class -> consumeRemoveAt(list)
        RemoveAllPredicate::class -> RemoveAllPredicate(setOf(pickValue(list), consumeInt()))
        Set::class -> consumeSet(list)
        RemoveLast::class -> RemoveLast
        else -> TODO("can't generate $op")
    }
}

private fun FuzzedDataProvider.consumeSet(list: List<Int>) =
    Set(consumeRemoveIndex(list), consumeInt())

private fun FuzzedDataProvider.consumeRemoveAt(list: List<Int>) =
    RemoveAt(consumeRemoveIndex(list))

private fun FuzzedDataProvider.consumeAddAllAt(list: List<Int>) =
    AddAllAt(consumeAddIndex(list), consumeIntList())

private fun FuzzedDataProvider.consumeAddAll() = AddAll(consumeIntList())

private fun FuzzedDataProvider.consumeAddAt(list: List<Int>) =
    AddAt(consumeAddIndex(list), consumeInt())

private fun FuzzedDataProvider.consumeAdd() = Add(consumeInt())