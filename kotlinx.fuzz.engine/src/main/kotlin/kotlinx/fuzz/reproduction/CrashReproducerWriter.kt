package kotlinx.fuzz.reproduction

import java.lang.reflect.Method
import java.nio.file.Path

abstract class CrashReproducerWriter(
    private val template: TestReproducerTemplate,
    private val method: Method,
) {
    protected val Method.instanceString: String
        get() = if (isDeclaredInObject()) {
            method.declaringClass.kotlin.simpleName.orEmpty()
        } else {
            "${method.declaringClass.kotlin.simpleName}()"
        }
    protected fun invokeTestAndRegisterOutputs(instance: Any, method: Method, input: ByteArray): List<Any?> {
        val fuzzer = OutputRegisteringKFuzzer(input)
        try {
            method.invoke(instance, fuzzer)
        } catch (_: Throwable) {
            // Nothing to do here
        }
        return fuzzer.values
    }

    private fun Method.isDeclaredInObject() = this.declaringClass.kotlin.objectInstance != null

    abstract fun writeToFile(input: ByteArray, reproducerFile: Path)
}
