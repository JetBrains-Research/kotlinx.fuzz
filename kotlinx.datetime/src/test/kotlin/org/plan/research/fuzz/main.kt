package org.plan.research.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import java.lang.reflect.Method

val Method.fullName: String
    get() = "${declaringClass.canonicalName}.${name}"

fun Method.isFuzzTarget(): Boolean {
    return returnType == Void.TYPE && parameters.size == 1 && parameterTypes[0] == FuzzedDataProvider::class.java
}

fun main() {
    val reflections = Reflections("fuzz", Scanners.MethodsAnnotated)
    val methods = reflections.getMethodsAnnotatedWith(FuzzTest::class.java)

    check(methods.all { it.isFuzzTarget() })

    println(methods.groupBy { it.declaringClass }.values.joinToString("\n\n") { it.joinToString("\n") { it.fullName } })
}