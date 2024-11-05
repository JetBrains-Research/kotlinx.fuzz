package org.plan.research

import Aboba
import kotlinx.reflect.lite.jvm.kotlinPackage

fun main() {
    val x = Aboba::class.java.kotlinPackage
    println(x)
}