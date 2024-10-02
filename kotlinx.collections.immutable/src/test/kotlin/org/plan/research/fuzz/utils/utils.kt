/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package org.plan.research.fuzz.utils

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import java.lang.reflect.Method

fun FuzzedDataProvider.forceConsumeInts(size: Int): List<Int> {
    return List(size) { this.consumeInt() }
//    val ints = arrayListOf<Int>()
//    while (ints.size < size) {
//        System.err.println("force consume")
//        val next = consumeInts(size - ints.size)
//        ints.addAll(next.toTypedArray())
//    }
//    return ints
}


inline fun <T> tryOrNull(block: () -> T): T? = try {
    block()
} catch (e: Throwable) {
    null
}

inline fun <T> tryOr(default: T, block: () -> T): T = tryOrNull(block) ?: default

var validateInvariants: Boolean = false

var validateReverse: Boolean = false

var validateReplay: Boolean = false

var validateBuilder: Boolean = false

val initSize: Int =
    (System.getenv("INIT_SIZE")?.toIntOrNull() ?: 100).also { println("INIT_SIZE: $it") }

const val MAX_BUFFER_SIZE = 32


val Method.fullName: String
    get() = "${declaringClass.canonicalName}.${name}"

fun Method.isFuzzTarget(): Boolean {
    return returnType == Void.TYPE && parameters.size == 1 && parameterTypes[0] == FuzzedDataProvider::class.java
}

fun main() {
    val pkg = "fuzz"
    
    val refConfig = ConfigurationBuilder().forPackages(pkg).setScanners(Scanners.MethodsAnnotated)
    val reflections = Reflections(refConfig)
    val methods = reflections.getMethodsAnnotatedWith(FuzzTest::class.java)
        .filter { it.declaringClass.packageName == pkg }

    assertEquals(null, methods.firstOrNull { !it.isFuzzTarget() })

    println(methods.groupBy { it.declaringClass }.values.joinToString("\n\n") { it.joinToString("\n") { it.fullName } })
}
