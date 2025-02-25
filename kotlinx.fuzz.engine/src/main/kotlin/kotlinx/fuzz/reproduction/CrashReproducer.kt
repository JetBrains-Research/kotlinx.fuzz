package kotlinx.fuzz.reproduction

import java.lang.reflect.Method
import java.nio.file.Path

interface CrashReproducer {
    fun registerOutputs(instance: Any, method: Method, input: ByteArray): List<ExecutionResult> {
        val fuzzer = KFuzzerRegisteringImpl(input)
        try {
            method.invoke(instance, fuzzer)
        } finally {
            return fuzzer.values
        }
    }
    fun writeToFile(input: ByteArray, reproducerFile: Path)
}
