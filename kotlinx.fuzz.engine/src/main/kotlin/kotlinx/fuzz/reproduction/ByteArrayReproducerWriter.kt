package kotlinx.fuzz.reproduction

import com.squareup.kotlinpoet.CodeBlock
import java.lang.reflect.Method
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.writeText

class ByteArrayReproducerWriter(private val template: ReproducerTestTemplate, private val method: Method) :
    CrashReproducerWriter(template, method) {
    @OptIn(ExperimentalStdlibApi::class)
    override fun writeToFile(input: ByteArray, reproducerFile: Path) {
        val code = CodeBlock.builder()
            .addStatement(
                "${method.getInstanceString()}.`${method.name}`(KFuzzerImpl(byteArrayOf(${input.joinToString(", ")})))",
            )
            .build()

        reproducerFile.writeText(
            template.buildReproducer(
                MessageDigest.getInstance("SHA-1").digest(input).toHexString(),
                code,
                imports = listOf(KotlinpoetImport("kotlinx.fuzz", "KFuzzerImpl")),
            ),
        )
    }
}
