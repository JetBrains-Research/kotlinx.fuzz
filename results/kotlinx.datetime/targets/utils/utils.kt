/*
 * Copyright 2019-2024 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package fuzz.utils

import org.junit.jupiter.api.Assertions.assertEquals

inline fun <K_TYPE, J_TYPE> compareTest(
    createKotlin: () -> K_TYPE,
    createJava: () -> J_TYPE,
    kotlinToJava: (K_TYPE) -> J_TYPE,
    javaToKotlin: (J_TYPE) -> K_TYPE
) {
    val kotlinRes = runCatching { createKotlin() }
    val javaRes = runCatching { createJava() }

    assertEquals(kotlinRes.isSuccess, javaRes.isSuccess)
    return if (kotlinRes.isFailure || javaRes.isFailure) {
        Unit
    } else {
        val kotlinVal = kotlinRes.getOrThrow()
        val javaVal = javaRes.getOrThrow()

        val javaFromKotlin = kotlinToJava(kotlinVal)
        val kotlinFromJava = javaToKotlin(javaVal)

        assertEquals(kotlinVal, kotlinFromJava)
        assertEquals(javaVal, javaFromKotlin)
    }
}


inline fun <T> compareTest(
    firstBlock: () -> T,
    secondBlock: () -> T,
) {
    val firstRes = runCatching { firstBlock() }
    val secondRes = runCatching { secondBlock() }
    assertEquals(firstRes.isSuccess, secondRes.isSuccess)
    assertEquals(firstRes.getOrNull(), secondRes.getOrNull())
}


inline fun <T> tryOrNull(block: () -> T): T? = try {
    block()
} catch (t: Throwable) {
    null
}

inline fun isFine(block: () -> Unit) = try {
    block()
} catch (_: IllegalArgumentException) {
}