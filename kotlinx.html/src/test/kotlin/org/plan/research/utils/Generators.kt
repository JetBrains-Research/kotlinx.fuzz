package org.plan.research.utils

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlinx.html.AttributeEnum
import kotlinx.html.Tag
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

fun KClass<*>.randomFunctions(data: FuzzedDataProvider): List<KFunction<*>> {
    val num = data.consumeInt(0, 3)
    val methods = ReflectionUtils.tagToMethods[this]
    return when {
        methods == null -> error("No methods for $this")
        methods.isEmpty() -> emptyList()
        else -> List(num) { data.pickValue(methods) }
    }
}

private fun genArg(data: FuzzedDataProvider, paramType: KType): Any? = when {
    paramType.isMarkedNullable && data.consumeBoolean() -> null
    paramType.isSubtypeOf(typeOf<Enum<*>?>()) -> data.pickValue(paramType.jvmErasure.java.enumConstants)
    else -> when (val a = paramType.jvmErasure) {
        Enum::class -> error("enum")
        String::class -> data.consumeString(10)
        AttributeEnum::class -> data.pickValue(a.java.enumConstants)
        else -> error("Unexpected argument type: $paramType")
    }
}


fun genLambda(data: FuzzedDataProvider, tref: TRef): Tag.() -> Unit = {
    val receiver = this
    val calls = receiver::class.randomFunctions(data)
    calls.forEach {
        tref += it.name
        it.callWithData(receiver, data, tref.last())
    }
}

data class TRef(val tag: String, val children: MutableList<TRef> = mutableListOf()) {
    companion object {
        lateinit var root: TRef
        private const val SPACE = " "
    }

    fun last() = children.last()

    operator fun plusAssign(child: String) {
        children.add(TRef(child))
    }


    @Suppress("unused", "MemberVisibilityCanBePrivate")
    fun prettyPrint(indent: Int = 0): String = buildString {
        append("${SPACE.repeat(indent)}$tag {\n")
        children.forEach {
            append(it.prettyPrint(indent + 2))
        }
        append("${SPACE.repeat(indent)}}\n")
    }
}

fun <T : Tag> KFunction<*>.callWithData(
    receiver: T,
    data: FuzzedDataProvider,
    tref: TRef
) {
    assert(parameters.first().type.isSubtypeOf(typeOf<Tag>()))
    val otherParameterTypes = parameters.slice(1 until parameters.size - 1)
    val args = otherParameterTypes.map {
        genArg(data, it.type)
    }
    val lastParamType = parameters.last().type
    val argLast: Any? = if (lastParamType.isSubtypeOf(typeOf<Function<Unit>>())) {
        genLambda(data, tref)
    } else {
        genArg(data, parameters.last().type)
    }
    call(receiver, *args.toTypedArray(), argLast)
}