package kotlinx.fuzz.reproduction

import com.squareup.kotlinpoet.CodeBlock

interface ReproducerTemplate {
    fun buildReproducer(identifier: String, code: CodeBlock, imports: List<KotlinpoetImport>): String
}

data class KotlinpoetImport(val packageName: String, val className: String)
