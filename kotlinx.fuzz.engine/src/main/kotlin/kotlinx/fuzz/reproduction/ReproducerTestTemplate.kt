package kotlinx.fuzz.reproduction

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec

interface ReproducerTestTemplate {
    fun buildReproducer(
        identifier: String,
        testCode: CodeBlock,
        imports: List<KotlinpoetImport> = emptyList(),
        additionalObjects: List<TypeSpec> = emptyList(),
        additionalFunctions: List<FunSpec> = emptyList(),
    ): String
}

data class KotlinpoetImport(val packageName: String, val className: String)
