package kotlinx.fuzz.reproduction

import com.squareup.kotlinpoet.*
import java.lang.reflect.Method
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.*

class ListAnyCallReproducerWriter(
    private val template: TestReproducerTemplate, private val instance: Any,
    private val method: Method,
) : CrashReproducerWriter(template, method) {
    @OptIn(ExperimentalStdlibApi::class)
    override fun writeToFile(input: ByteArray, reproducerFile: Path) {
        val code = buildCodeBlock {
            addStatement(
                "val values = listOf<Any?>(" +
                    invokeTestAndRegisterOutputs(instance, method, input).joinToString(", ") { toCodeString(it) } +
                    ")",
            )
            addStatement("${method.instanceString}.`${method.name}`(ListReproducer(values))")
        }

        reproducerFile.writeText(
            template.buildReproducer(
                MessageDigest.getInstance("SHA-1").digest(input).toHexString(),
                code,
                imports = listOf("import kotlinx.fuzz.KFuzzer"),
                additionalCode = buildListReproducerObject().toString(),
            ),
        )
    }
}
