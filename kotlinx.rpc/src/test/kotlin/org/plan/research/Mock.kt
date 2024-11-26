package org.plan.research

import com.code_intelligence.jazzer.api.HookType
import com.code_intelligence.jazzer.api.Jazzer
import com.code_intelligence.jazzer.api.MethodHook
import kotlinx.rpc.internal.utils.InternalRPCApi
import kotlinx.rpc.krpc.internal.logging.CommonLogger
import kotlinx.rpc.krpc.internal.logging.CommonLoggerFactory
import java.lang.invoke.MethodHandle
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

//@Suppress("UNUSED")
//@MethodHook(
//    type = HookType.REPLACE,
//    targetClassName = "ResultKt",
//    targetMethod = "runCatching",
//)
//fun runCatchingThrows(
//    method: MethodHandle,
//    thisObject: Object,
//    arguments: Array<Object>,
//    hookId: Int
//) {
//    error("run catching")
//}

@OptIn(InternalRPCApi::class)
fun mockLogger() {
    val loggerCompanion = CommonLogger.Companion
    val loggerKClass = loggerCompanion::class as KClass<CommonLogger.Companion>
    val factoryProperty = loggerKClass.declaredMemberProperties.find { it.name == "factory" }!! as KProperty0<*>
    // get the original factory (it is internal)
    factoryProperty.isAccessible = true
    val originalFactory = factoryProperty.get() as CommonLoggerFactory
    when (val originalFactoryName = originalFactory::class.simpleName) {
        "CommonLoggerFactoryImpl" -> {
            // apply the mock
            val newFactory = ThrowingLoggerFactory(originalFactory)
            // change `val` by changing its java backing field
            factoryProperty.javaField!!.set(loggerCompanion, newFactory)
        }
        ThrowingLoggerFactory::class.simpleName -> {
            // already mocked
        }
        else -> error("bad mock $originalFactoryName, wtf?")
    }
}

@OptIn(InternalRPCApi::class)
private class ThrowingLoggerFactory(private val delegate: CommonLoggerFactory) : CommonLoggerFactory {
    override fun getLogger(func: () -> Unit): CommonLogger {
        return ThrowingLogger(delegate.getLogger(func))
    }

    override fun getLogger(name: String): CommonLogger {
        return ThrowingLogger(delegate.getLogger(name))
    }
}

@OptIn(InternalRPCApi::class)
private class ThrowingLogger(delegate: CommonLogger) : CommonLogger by delegate {
    override fun error(msg: () -> Any?) {
        TODO("haha")
    }

    override fun error(t: Throwable?, msg: () -> Any?) {
        TODO("hehe")
    }
}
