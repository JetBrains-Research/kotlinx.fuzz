package org.plan.research

import kotlin.test.Test
import kotlinx.reflect.lite.*
import kotlinx.reflect.lite.jvm.*
import kotlinx.reflect.lite.impl.*
import kotlinx.reflect.lite.full.*
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.coroutines.Continuation
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Example<T>() {
    constructor(x: Int): this()

    suspend fun fun0() = this
    fun fun1(vararg e: Example<T>) {}
    fun fun2(l: List<Int>) {}
    internal fun fun3() {}

    var field0: T?
        get() = null
        set(value) {}
    var field1: suspend (Int) -> Int
        get() = { x -> x }
        set(value) {}
    val field2 = Class1<Any>()

    @JvmInline
    value class Class0(val x: Int) {
        constructor(): this(0)
        fun fun0() : Int { return 0 }
        operator fun minus(e: Class0) = this

        val field0: Int get() = 0
    }
    class Class1<in K: Any>(){}
}

object Reproducers {
    @Test
    fun `wrong number of arguments`() {
        val callable = Example::class.java.kotlin.members.single { it.name == "fun0" }
        callable.isAccessible = true
        assertTrue { callable.parameters.size == 1 }
        assertDoesNotThrow { callable.call(Example<Int>()) } // Expects 2
    }

    @Test
    fun `argument type mismatch with varargs`() {
        val callable = Example::class.java.kotlin.members.single { it.name == "fun1" }
        callable.isAccessible = true
        assertTrue { callable.parameters.size == 2 }
        assertDoesNotThrow { callable.call(Example<Int>(), *listOf(Example<Int>()).toTypedArray()) }
        // callable.call(Example(), Example()) throws as well
    }

    @Test
    fun `nullable void`() {
        val field = Example::class.java.kotlin.members.single { it.name == "field0" }
        field.isAccessible = true
        field as KMutableProperty1<Int?, Example<Int>>
        assertFalse { field.setter.returnType.isMarkedNullable }
    }

    @Test
    fun `list as argument`() {
        val callable = Example::class.java.kotlin.members.single { it.name == "fun2" }
        callable.isAccessible = true
        assertTrue { callable.parameters.first().type.arguments.first().type!!.javaType.typeName == "int" }
    }

    @Test
    fun `internal functions are not represented in kotlin`() {
        val callable = Example::class.java.kotlin.members.single { it.name == "fun3" }
        callable.isAccessible = true
        assertTrue { (callable as KFunction).javaMethod!!.kotlinFunction == callable }
    }

    @Test
    fun `void with arguments`() {
        val field = Example::class.java.kotlin.members.single { it.name == "field1" }
        field.isAccessible = true
        field as KMutableProperty1<Function2<Int, Continuation<Int>, Int>, Example<Int>>
        assertTrue { field.setter.returnType.arguments.isEmpty() }
    }

    @Test
    fun `methods of value class are not represented`() {
        val callable = Example.Class0::class.java.kotlin.members.single { it.name == "fun0" }
        callable.isAccessible = true
        assertTrue { (callable as KFunction).javaMethod!!.kotlinFunction == callable }
    }

    @Test
    fun `constructors of value class are not represented`() {
        val callable = Example.Class0::class.java.kotlin.constructors.single { it.parameters.isEmpty() }
        assertDoesNotThrow { callable.javaConstructor }
    }

    @Test
    fun `methods of value class expect extra parameters`() {
        val callable = Example.Class0::class.java.kotlin.members.single { it.name == "fun0" }
        callable.isAccessible = true
        assertTrue { callable.parameters.size == 1 }
        assertDoesNotThrow { callable.call(Example.Class0()) }
    }

    @Test
    fun `constructors of value class cannot be accessed`() {
        val callable = Example.Class0::class.java.kotlin.constructors.single { it.parameters.isEmpty() }
        assertDoesNotThrow { callable.isAccessible = true }
    }

    @Test
    fun `unexpected javaType`() {
        val callable = Example.Class0::class.java.kotlin.members.single { it.name == "minus" }
        callable.isAccessible = true
        assertTrue { callable.parameters.size == 2 }
        assertTrue { callable.parameters[1].type.javaType.typeName == "org.plan.research.Example.Class0" }
    }

    @Test
    fun `can't access field getter`() {
        val field = Example.Class0::class.java.kotlin.members.single { it.name == "field0" }
        field.isAccessible = true
        field as KProperty1<Example.Class0, Int>
        assertDoesNotThrow { field.get(Example.Class0()) }
    }

    @Test
    fun `incorrect constructor flags`() {
        val constructor = Example::class.java.kotlin.constructors.single { it.parameters.size == 1 }
        constructor.isAccessible = true
        assertTrue{ constructor.isFinal }
        assertFalse{ constructor.isOpen }
    }

    @Test
    fun `incorrect variance`() {
        val field = Example::class.java.kotlin.members.single { it.name == "field2" }
        field.isAccessible = true
        field as KProperty1<Function2<Int, Continuation<Int>, Int>, Example<Int>>
        assertTrue { field.typeParameters[0].variance == KVariance.IN }
    }
}