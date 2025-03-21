package org.plan.research.utils

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.bytestring.ByteString
import kotlinx.io.snapshot
import org.plan.research.Constants
import java.io.File
import java.lang.reflect.InvocationTargetException
import kotlin.random.Random
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubclassOf
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

inline fun <R> catching(
    vararg catchingClass: KClass<out Throwable> = arrayOf(
        RuntimeException::class,
        IOException::class,
    ),
    block: () -> R
): Result<R> = try {
    Result.success(block())
} catch (e: InvocationTargetException) {
//            e as InvocationTargetException
    if (catchingClass.none { e.targetException::class.isSubclassOf(it) }) {
        throw e
    }
    Result.failure(e)
} catch (e: Throwable) {
    if (catchingClass.none { e::class.isSubclassOf(it) }) {
        throw e
    }
    Result.failure(e)
}

class Couple<T>(val test: T, val control: T) {

    companion object {
    }

    fun <U> invokeOperation(
        function: KCallable<U>,
        args1: Array<*>,
        args2: Array<*>,
        postAssertion: (Result<U>, Result<U>) -> Unit = ::assertEqualsComplete
    ) {
        val testRes = catching<U> { function.call(test, *args1) }
        val controlRes = catching<U> { function.call(control, *args2) }
        postAssertion(testRes, controlRes)
    }
}

fun <U> assertEqualsComplete(testRes: Result<U>, controlRes: Result<U>) {
    assertTrue(testRes.isSuccess == controlRes.isSuccess, "Exactly one failed")
    if (testRes.isSuccess && controlRes.isSuccess) {
        val testVal = testRes.getOrThrow()
        val controlVal = controlRes.getOrThrow()
        if (testVal == null && controlVal == null) {
            Unit
        } else {
            when (testVal) {
                is ByteArray -> assertContentEquals(testVal, controlVal as ByteArray)
                is Buffer -> {
                    controlVal as Buffer
                    assertEquals(testVal.snapshot(), controlVal.snapshot())
                }

                else -> assertEquals(testRes, controlRes)
            }
        }
    }
}

inline fun <T> template(
    source: T,
    buf: T,
    data: FuzzedDataProvider,
    funs: Array<KFunction<*>>,
    genArgsFallback: KCallable<*>.() -> Array<*> = {
        error("Unexpected method: $this")
    }
): List<KCallable<*>> {
    val couple = Couple(source, buf)
    val n = data.consumeInt(0, Constants.MAX_OPERATIONS_NUMBER)
    return couple.callNOps(n, funs, data, genArgsFallback)
}

inline fun <T> Couple<T>.callNOps(
    n: Int,
    funs: Array<KFunction<*>>,
    data: FuzzedDataProvider,
    genArgsFallback: KFunction<*>.() -> Array<*> = { error("Unexpected method: $this") }
): List<KFunction<*>> {
    val ops = mutableListOf<KFunction<*>>()
    val largs = mutableListOf<Array<*>>()
    repeat(n) {
        val op = data.pickValue(funs)
        ops += op

        val args = op.generateArguments(data, genArgsFallback)
        largs += args
        val args2 = copyArguments(args, data)
        invokeOperation(op, args, args2)
    }
    return ops
}

inline fun <reified T> Array<T>.getChunk(
    chunkNumber: Int, chunkSize: Int
): Array<T> = toList()
    .shuffled(Random(chunkNumber))
    .take(chunkSize)
    .toTypedArray()

inline fun <reified T> Array<T>.splitIntoChunks(
    chunksCount: Int,
    chunkSize: Int
): Array<Array<T>> = Array(chunksCount) { i -> this.getChunk(i, chunkSize) }

inline fun <T> T.callOps(
    n: Int,
    funs: Array<KFunction<*>>,
    data: FuzzedDataProvider,
    genArgsFallback: KCallable<*>.() -> Array<*> = { error("Unexpected method: $this") }
): List<KFunction<*>> {
    val ops = mutableListOf<KFunction<*>>()
    repeat(n) {
        val op = data.pickValue(funs)
        ops += op
        val args = op.generateArguments(data, genArgsFallback)
        catching { op.call(this, *args) }
//        invokeOperation(op, args, args2)
    }
    return ops
}

// 32MB
private val bytesNum = 1 shl 25
val manyBytes: ByteArray by lazy { Random(42).nextBytes(bytesNum) }
val largeByteString: ByteString by lazy { ByteString(Random(42).nextBytes(bytesNum)) }

fun main() {
    val random = Random(777)
    val f = File("data.bin")
    f.writeBytes(random.nextBytes(2L.shl(16).toInt()))
}