package kotlinx.fuzz.reproduction

import java.lang.reflect.Method
import java.nio.file.Path

abstract class CrashReproducerWriter(private val template: ReproducerTestTemplate, private val method: Method) {
    fun registerOutputs(instance: Any, method: Method, input: ByteArray): List<ExecutionResult> {
        val fuzzer = KFuzzerRegisteringImpl(input)
        try {
            method.invoke(instance, fuzzer)
        } finally {
            return fuzzer.values
        }
    }

    private fun Method.isDeclaredInObject() = this.declaringClass.kotlin.objectInstance != null
    protected fun Method.getInstanceString() = if (isDeclaredInObject()) {
        method.declaringClass.kotlin.simpleName
    } else {
        "${method.declaringClass.kotlin.simpleName}::class.java.getDeclaredConstructor().newInstance()"
    }

    abstract fun writeToFile(input: ByteArray, reproducerFile: Path)
}
