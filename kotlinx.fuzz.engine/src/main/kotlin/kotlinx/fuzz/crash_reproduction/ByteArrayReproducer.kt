package kotlinx.fuzz.crash_reproduction

import com.squareup.kotlinpoet.CodeBlock
import java.lang.reflect.Method
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.writeText

class ByteArrayReproducer(private val template: ReproducerTemplate, private val method: Method) : CrashReproducer {
    @OptIn(ExperimentalStdlibApi::class)
    override fun writeToFile(input: ByteArray, reproducerFile: Path) {
        val code = CodeBlock.builder()
            .addStatement("${if (method.declaringClass.kotlin.objectInstance != null){ method.declaringClass.kotlin.simpleName } else { "${method.declaringClass.kotlin.simpleName}::class.java.getDeclaredConstructor().newInstance()" }}.`${method.name}`(KFuzzerImpl(byteArrayOf(${input.joinToString(", ")})))")
            .build()

        reproducerFile.writeText(
            template.buildReproducer(
                MessageDigest.getInstance("SHA-1").digest(input).toHexString(),
                code,
                listOf(Pair("kotlinx.fuzz", "KFuzzerImpl"))
            )
        )
    }
}