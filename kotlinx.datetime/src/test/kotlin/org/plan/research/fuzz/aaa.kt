package org.plan.research.fuzz

import com.code_intelligence.jazzer.junit.FuzzTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder


fun main() {
    val pkg = "fuzz"

    val refConfig = ConfigurationBuilder().forPackages(pkg).setScanners(Scanners.MethodsAnnotated)
    val reflections = Reflections(refConfig)
    val methods = reflections.getMethodsAnnotatedWith(FuzzTest::class.java)
        .filter { it.declaringClass.packageName == pkg }

    assertEquals(null, methods.firstOrNull { !it.isFuzzTarget() })

    println(methods.groupBy { it.declaringClass }.values.joinToString("\n\n") { it.joinToString("\n") { it.fullName } })
}
