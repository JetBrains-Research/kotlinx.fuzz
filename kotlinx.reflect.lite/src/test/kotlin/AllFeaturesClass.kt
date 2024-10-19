package org.plan.research

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class AllFeaturesClass(
    val publicProperty: Int,
    private val privateProperty: String,
    protected var protectedProperty: Boolean
) : ExampleInterface, AbstractBaseClass(), DelegatedInterface by DelegatedClass() {

    // Public constructor
    constructor() : this(0, "Default", false)

    // Private constructor
    private constructor(secretValue: Int) : this(secretValue, "Secret", true)

    // Protected constructor
    protected constructor(secretMessage: String) : this(99, secretMessage, true)

    // Internal constructor
    internal constructor(internalValue: Int, internalString: String) : this(internalValue, internalString, true)

    var List<Int>.extension: Int
        get() = 42
        set(value) {}

    var publicField: Int = 42
        get() = field
        set(value) {
            field = value
            println("Public Field updated to: $value")
        }

    private val privateField: String = "ok"
        get() = field.uppercase()

    internal var internalField: Double = 3.14
        private set

    protected var protectedField: Boolean = true
        protected set(value) {
            println("Setting protected field to $value")
            field = value
        }

    private lateinit var lateInitProperty: String

    val lazyProperty: String by lazy {
        println("Lazy property initialized")
        "Lazy Value"
    }

    companion object {
        const val CONSTANT: String = "Constant Value"

        lateinit var companionLateInit: String

        val companionLazyProperty: String by lazy {
            println("Companion lazy property initialized")
            "Companion Lazy Value"
        }

        fun createInstance(): AllFeaturesClass {
            return AllFeaturesClass(10, "Companion Instance", false)
        }
    }

    @JvmInline
    value class InlineClass(val value: Int)

    @Target(AnnotationTarget.FUNCTION)
    annotation class CustomAnnotation

    infix fun infixMethod(value: Int): AllFeaturesClass {
        println("Infix method called with $value")
        return this
    }

    tailrec fun tailrecMethod(x: Int): Int {
        return if (x <= 1) x else tailrecMethod(x - 1)
    }

    fun performOperation(a: Int, b: Int, operation: (Int, Int) -> Int): Int {
        return operation(a, b)
    }

    context(Context) fun contextReceiverFunction() {
        someMethod()
    }

    @OptIn(ExperimentalContracts::class)
    inline fun <T> runIf(condition: Boolean, block: () -> T): T? {
        contract {
            returnsNotNull() implies condition
        }
        return if (condition) block() else null
    }

    fun buildStringList(block: MutableList<String>.() -> Unit): List<String> {
        return mutableListOf<String>().apply(block)
    }

    @CustomAnnotation
    fun annotatedMethod() {
        println("Method with custom annotation called")
    }

    suspend fun suspendMethod() {
        println("Suspend method called")
    }

    override fun delegatedMethod() {
        println("Overridden method using delegation")
    }

    override fun interfaceMethod() {
        println("Overridden interface method")
    }

    override fun abstractMethod() {
        println("Overridden abstract method")
    }

    fun AllFeaturesClass.extensionFunction() {
        println("Extension function called on FeatureRichClass")
    }

    inline fun <reified T> checkType(value: Any): Boolean {
        return value is T
    }

    fun multipleParameters(vararg numbers: Int) {
        println("Varargs called with values: ${numbers.joinToString()}")
    }

    sealed class SealedClass {
        object Success : SealedClass()
        object Failure : SealedClass()
        data class Loading(val progress: Int) : SealedClass()
    }

    enum class Status(val code: Int) {
        ACTIVE(1) {
            override fun printStatus() {
                println("Status: Active with code $code")
            }
        },
        INACTIVE(0) {
            override fun printStatus() {
                println("Status: Inactive with code $code")
            }
        },
        SUSPENDED(-1) {
            override fun printStatus() {
                println("Status: Suspended with code $code")
            }
        };

        abstract fun printStatus()
    }

    object SingletonObject {
        fun doSomething() {
            println("Singleton Object doing something")
        }
    }

    operator fun unaryMinus(): AllFeaturesClass {
        return AllFeaturesClass(-this.publicProperty, "Negative", !this.protectedProperty)
    }

    fun Companion.extensionCompanionFunction() {
        println("Extension function on companion object")
    }

    external fun externalMethod()

    var visibilityRestrictedField: String = "Initial"
        private set


    class CovariantProducer<out T>(private val value: T) {
        fun get(): T = value
    }

    class ContravariantConsumer<in T> {
        fun consume(value: T) {
            println("Consumed $value")
        }
    }

    inline fun <reified T> reifiedExample(value: Any) {
        if (value is T) {
            println("Value is of type ${T::class.simpleName}")
        } else {
            println("Value is NOT of type ${T::class.simpleName}")
        }
    }

    fun methodWithLocalFunction() {
        fun localFunction(x: Int): Int {
            return x * x
        }
        println("Local function result: ${localFunction(5)}")
    }

    fun methodThatThrows() {
        throw IllegalArgumentException("This method always throws an exception!")
    }

    fun <T> varianceExample(producer: CovariantProducer<out T>, consumer: ContravariantConsumer<in T>) {
        val value = producer.get()
        consumer.consume(value)
    }
}

interface DelegatedInterface {
    fun delegatedMethod()
}

class DelegatedClass : DelegatedInterface {
    override fun delegatedMethod() {
        println("Delegated method implementation")
    }
}

abstract class AbstractBaseClass {
    abstract fun abstractMethod()
}

interface ExampleInterface {
    fun interfaceMethod()
}

class Context {
    fun someMethod() {
        println("Context implementation")
    }
}