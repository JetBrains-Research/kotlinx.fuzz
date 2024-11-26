package org.plan.research

import kotlin.test.Test
import kotlinx.reflect.lite.*
import kotlinx.reflect.lite.jvm.*
import kotlinx.reflect.lite.impl.*
import kotlinx.reflect.lite.full.*
import kotlinx.reflect.lite.jvm.kotlinFunction as kotlinFunctionLite
import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.reflect.jvm.kotlinFunction as kotlinFunction
import kotlin.coroutines.Continuation
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class Example<T>() {
    constructor(x: Int) : this()

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
        constructor() : this(0)

        fun fun0(): Int {
            return 0
        }

        operator fun minus(e: Class0) = this

        val field0: Int get() = 0
    }

    class Class1<in K : Any>() {}
}

object Reproducers {
    @Test
    fun `wrong number of arguments`() {
        val callable = Example::class.members.single { it.name == "fun0" }
        val callableLite = Example::class.java.kotlin.members.single { it.name == "fun0" }
        callable.isAccessible = true
        callableLite.isAccessible = true
        assertTrue { callable.parameters.size == 1 }
        assertTrue { callableLite.parameters.size == 1 }
        assertDoesNotThrow { callable.call(Example<Int>()) }
        assertDoesNotThrow { callableLite.call(Example<Int>()) }
    }

    @Test
    fun `argument type mismatch with varargs`() {
        val callable = Example::class.members.single { it.name == "fun1" }
        val callableLite = Example::class.java.kotlin.members.single { it.name == "fun1" }
        callable.isAccessible = true
        callableLite.isAccessible = true
        assertTrue { callable.parameters.size == 2 }
        assertTrue { callableLite.parameters.size == 2 }
        assertDoesNotThrow { callable.call(Example<Int>(), *listOf(Example<Int>()).toTypedArray()) }
        assertDoesNotThrow { callableLite.call(Example<Int>(), *listOf(Example<Int>()).toTypedArray()) }
        // callable.call(Example<Int>(), Example<Int>()) throws as well
    }

    @Test
    fun `nullable void`() {
        val field = Example::class.members.single { it.name == "field0" }
        val fieldLite = Example::class.java.kotlin.members.single { it.name == "field0" }
        field.isAccessible = true
        fieldLite.isAccessible = true
        field as kotlin.reflect.KMutableProperty1<Int?, Example<Int>>
        fieldLite as kotlinx.reflect.lite.KMutableProperty1<Int?, Example<Int>>
        assertFalse { field.setter.returnType.isMarkedNullable }
        assertFalse { fieldLite.setter.returnType.isMarkedNullable }
    }

    @Test
    fun `internal functions are not represented in kotlin`() {
        val callable = Example::class.members.single { it.name == "fun3" }
        val callableLite = Example::class.java.kotlin.members.single { it.name == "fun3" }
        callable.isAccessible = true
        callableLite.isAccessible = true
        assertTrue { (callable as kotlin.reflect.KFunction).javaMethod!!.kotlinFunction == callable }
        assertTrue { (callableLite as kotlinx.reflect.lite.KFunction).javaMethod!!.kotlinFunctionLite == callable }
    }

    @Test
    fun `void with arguments`() {
        val field = Example::class.members.single { it.name == "field1" }
        val fieldLite = Example::class.java.kotlin.members.single { it.name == "field1" }
        field.isAccessible = true
        fieldLite.isAccessible = true
        field as kotlin.reflect.KMutableProperty1<Example<Int>, Function2<Int, Continuation<Int>, Int>>
        fieldLite as kotlinx.reflect.lite.KMutableProperty1<Example<Int>, Function2<Int, Continuation<Int>, Int>>
        assertTrue { field.setter.returnType.arguments.isEmpty() }
        assertTrue { fieldLite.setter.returnType.arguments.isEmpty() }
    }

    @Test
    fun `methods of value class are not represented`() {
        val callable = Example.Class0::class.members.single { it.name == "fun0" }
        val callableLite = Example.Class0::class.java.kotlin.members.single { it.name == "fun0" }
        callable.isAccessible = true
        callableLite.isAccessible = true
        assertTrue { (callable as kotlin.reflect.KFunction).javaMethod!!.kotlinFunction == callable }
        assertTrue { (callableLite as kotlinx.reflect.lite.KFunction).javaMethod!!.kotlinFunctionLite == callable }
    }

    @Test
    fun `constructors of value class are not represented`() {
        val callable = Example.Class0::class.constructors.single { it.parameters.isEmpty() }
        val callableLite = Example.Class0::class.java.kotlin.constructors.single { it.parameters.isEmpty() }
        assertDoesNotThrow { callable.javaConstructor }
        assertDoesNotThrow { callableLite.javaConstructor }
    }

    @Test
    fun `methods of value class expect extra parameters`() {
        val callable = Example.Class0::class.members.single { it.name == "fun0" }
        val callableLite = Example.Class0::class.java.kotlin.members.single { it.name == "fun0" }
        callable.isAccessible = true
        callableLite.isAccessible = true
        assertTrue { callable.parameters.size == 1 }
        assertTrue { callableLite.parameters.size == 1 }
        assertDoesNotThrow { callable.call(Example.Class0()) }
        assertDoesNotThrow { callableLite.call(Example.Class0()) }
    }

    @Test
    fun `constructors of value class cannot be accessed`() {
        val callable = Example.Class0::class.constructors.single { it.parameters.isEmpty() }
        val callableLite = Example.Class0::class.java.kotlin.constructors.single { it.parameters.isEmpty() }
        assertDoesNotThrow { callable.isAccessible = true }
        assertDoesNotThrow { callableLite.isAccessible = true }
    }

    @Test
    fun `unexpected javaType`() {
        val callable = Example.Class0::class.members.single { it.name == "minus" }
        val callableLite = Example.Class0::class.java.kotlin.members.single { it.name == "minus" }
        callable.isAccessible = true
        callableLite.isAccessible = true
        assertTrue { callable.parameters.size == 2 }
        assertTrue { callableLite.parameters.size == 2 }
        assertTrue { callable.parameters[1].type.javaType.typeName == "org.plan.research.Example.Class0" }
        assertTrue { callableLite.parameters[1].type.javaType.typeName == "org.plan.research.Example.Class0" }
    }

    @Test
    fun `can't access field getter`() {
        val field = Example.Class0::class.members.single { it.name == "field0" }
        val fieldLite = Example.Class0::class.java.kotlin.members.single { it.name == "field0" }
        field.isAccessible = true
        fieldLite.isAccessible = true
        field as kotlin.reflect.KProperty1<Example.Class0, Int>
        fieldLite as kotlinx.reflect.lite.KProperty1<Example.Class0, Int>
        assertDoesNotThrow { field.get(Example.Class0()) }
        assertDoesNotThrow { fieldLite.get(Example.Class0()) }
    }

    @Test
    fun `incorrect constructor flags`() {
        val constructor = Example::class.constructors.single { it.parameters.size == 1 }
        val constructorLite = Example::class.java.kotlin.constructors.single { it.parameters.size == 1 }
        constructor.isAccessible = true
        constructorLite.isAccessible = true
        assertTrue { constructor.isFinal }
        assertTrue { constructorLite.isFinal }
        assertFalse { constructor.isOpen }
        assertFalse { constructorLite.isOpen }
    }

    @Test
    fun `incorrect variance`() {
        val field = Example::class.members.single { it.name == "field2" }
        val fieldLite = Example::class.java.kotlin.members.single { it.name == "field2" }
        field.isAccessible = true
        fieldLite.isAccessible = true
        field as kotlin.reflect.KProperty1<Example.Class1<Any>, Example<Int>>
        fieldLite as kotlinx.reflect.lite.KProperty1<Example.Class1<Any>, Example<Int>>
        assertTrue { field.returnType.arguments[0].variance == kotlin.reflect.KVariance.IN }
        assertTrue { fieldLite.returnType.arguments[0].variance == kotlinx.reflect.lite.KVariance.IN }
    }
}