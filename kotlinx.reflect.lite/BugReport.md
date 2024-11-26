All reproducers can be found [here](https://code.jetbrains.team/p/plan/repositories/kotlinx.fuzz/files/kotlinx.reflect.lite/kotlinx.reflect.lite/src/test/kotlin/Reproducers.kt). All cases that will be discussed here can be divided into two groups: bugs and reasonable cases that need more explanation in documentation.
## Bugs
### Behaviours that differ from kotlin.reflect
#### 1. Internal functions cannot be represented in kotlin after conversion to java method
```kotlin
class Example {
    internal fun foo() {}
}
val callable = Example::class.java.kotlin.members.single { it.name == "foo" }
callable.isAccessible = true
assertTrue { (callable as KFunction).javaMethod!!.kotlinFunction == callable }
```
We expect conversion from kotlin to java and then back to kotlin to be an identity transformation. However, in this case we get `null` and not the original function. This behaviour differs from `kotlin.reflect`
#### 2. Constructors of value class cannot be accessed
```kotlin
@JvmInline
value class Example(val x: Int) {
    constructor(): this(0)
}
val callable = Example::class.java.kotlin.constructors.single { it.parameters.isEmpty() }
callable.isAccessible = true
```
Last line throws `KotlinReflectionInternalError` that we don't expect to be shown outside the library. And we expect constructors to be accessible as well. This behaviour differs from `kotlin.reflect`
#### 3. Can't access field getter in value class
```kotlin
@JvmInline
value class Example(val x: Int) {
    constructor(): this(0)
    val field: Int get() = 0
}
val field = Example::class.java.kotlin.members.single { it.name == "field" }
field.isAccessible = true
field as KProperty1<Example, Int>
field.get(Example())
```
Last line throws `argument type mismatch` even though everything is correct. If we cast field to `KProperty1<Int, Int>` and pass `Int` as argument to getter, everything is going to be fine. Considered as bug since in `kotlin.reflect` works as expected
#### 4.Methods of value class expect an extra parameter
```kotlin
@JvmInline
value class Example(val x: Int) {
    constructor(): this(0)
    fun foo(): Int {
        return 0
    }
}
val callable = Example::class.java.kotlin.members.single { it.name == "foo" }
callable.isAccessible = true
assertTrue { callable.parameters.size == 1 }
callable.call(Example())
```
Last line expects 2 arguments even though `parameters.size == 1`. This behaviour differs from `kotlin.reflect`
#### 5. Void has isMarkedNullable set as true in some cases
```kotlin
class Example {
    var field: Int?
        get() = null
        set(value) {}
}
val field = Example::class.java.kotlin.members.single { it.name == "field" }
field.isAccessible = true
field as KMutableProperty1<Int?, Example>
assertFalse { field.setter.returnType.isMarkedNullable }
```
#### 6. Methods and constructors of value class cannot be represented
```kotlin
@JvmInline
value class Example(val x: Int) {
    constructor(): this(0)
    fun foo(): Int {
        return 0
    }
}
val callable = Example::class.java.kotlin.members.single { it.name == "foo" }
callable.isAccessible = true
assertTrue { (callable as KFunction).javaMethod!!.kotlinFunction == callable }
val callable1 = Example::class.java.kotlin.constructors.single { it.parameters.isEmpty() }
assertDoesNotThrow { callable1.javaConstructor }
```
Both assertions fail. Considered as bug since in `kotlin.reflect` everything works as expected
We expect `Unit` type that is return type of setter not to be marked as nullable in any case, but here we have it. This behaviour differs from `kotlin.reflect`
#### 7. Void can have arguments in some cases
```kotlin
class Example {
    var field: suspend (Int) -> Int
        get() = { x -> x }
        set(value) {}
}
val field = Example::class.java.kotlin.members.single { it.name == "field" }
field.isAccessible = true
field as KMutableProperty1<Example<Int>, Function2<Int, Continuation<Int>, Int>>
assertTrue { field.setter.returnType.arguments.isEmpty() }
```
Similar to isMarkedNullable case. This behaviour differs from `kotlin.reflect`
#### 8. Incorrect flags for secondary constructors
```kotlin
class Example() {
    constructor(x: Int) : this()
}
val constructor = Example::class.java.kotlin.constructors.single { it.parameters.size == 1 }
constructor.isAccessible = true
assertTrue { constructor.isFinal }
assertFalse { constructor.isOpen }
```
Both assertions fail for `kotlinx.reflect.lite` but not in `kotlin.reflect`
### Behaviours that are similar to kotlin.reflect
#### 1. Calling function with varargs
```kotlin
class Example {
    fun foo (vararg x: Example) {}
}
val callable = Example::class.java.kotlin.members.single { it.name == "foo" }
callable.isAccessible = true
callable.call(Example(), *listOf(Example()).toTypedArray())
```
We expect this code to run normally, however, it throws `argument type mismatch` as well as calling by `callable.call(Example(), Example()`. This behaviour is the same as if using `kotlin.reflect`
## Reasonable cases
### Behaviours that are similar to kotlin.reflect
#### 1. Suspend functions expect an additional parameter that is not mentioned
```kotlin
class Example() {
    suspend fun foo() {}
}
val callable = Example::class.java.kotlin.members.single { it.name == "foo" }
callable.isAccessible = true
assertTrue { callable.parameters.size == 1 }
callable.call(Example<Int>())
```
It is understandable that suspend function expects context as another argument, and it would be useful to mention this is in documentation. This behaviour is the same in `kotlin.reflect`
#### 2. Call-site variance differs from definition-site
```kotlin
class Example() {
    val field = Class1<Any>()
    class Class1<in K : Any>() {}
}
val field = Example::class.java.kotlin.members.single { it.name == "field" }
field.isAccessible = true
field as KProperty1<Example, Example.Class1<Any>>
assertTrue { field.returnType.arguments[0].variance == KVariance.IN }
```
It can be understood that actual variance is invariant but it better to be mentioned in documentation for less advanced kotlin users. This behaviour is the same in `kotlin.reflect`
#### 3. Java type is inlined for value class
```kotlin
@JvmInline
value class Example(val x: Int) {
    constructor(): this(0)
    operator fun minus(e: Example) = this
}
val callable = Example.Class0::class.java.kotlin.members.single { it.name == "minus" }
callable.isAccessible = true
assertTrue { callable.parameters.size == 2 }
assertTrue { callable.parameters[1].type.javaType.typeName == "Class0" }
```
Since in bytecode value classes are inlined this behaviour is understandable. But since `javaType.typeName` here is very different compared to `classifier.qualifiedName` it will be nice to mention it in documentation for less advanced kotlin users. This behaviour is the same in `kotlin.reflect`
