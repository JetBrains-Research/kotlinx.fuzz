package org.plan.research.fuzz.utils

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlin.reflect.full.isSubclassOf

private val setOperations = SetOperation::class.sealedSubclasses
private val emptySetOperations = setOperations.filter { it.isSubclassOf(EmptyOperation::class) }

fun FuzzedDataProvider.consumeSetOperation(set: kotlin.collections.Set<Int>): SetOperation {
    val op = if (set.isEmpty()) pickValue(emptySetOperations) else pickValue(setOperations)

    return when (op) {
        Add::class -> Add(consumeInt())
        Remove::class -> Remove(consumeInt())
        AddAll::class -> AddAll(consumeInts(10).toSet())
        else -> throw IllegalStateException("Unexpected operation: $op")
    }
}

