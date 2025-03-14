package kotlinx.fuzz.reproduction

import com.squareup.kotlinpoet.CodeBlock

interface ReproducerTestTemplate {
    fun buildReproducer(
        identifier: String,
        testCode: CodeBlock,
        imports: List<KotlinpoetImport> = emptyList(),
        additionalCode: String = "",
    ): String
}

data class KotlinpoetImport(val packageName: String, val className: String)
