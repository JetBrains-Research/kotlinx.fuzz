package kotlinx.fuzz.gradle.junit

import com.squareup.kotlinpoet.*
import java.lang.reflect.Method
import kotlinx.fuzz.reproduction.KotlinpoetImport
import kotlinx.fuzz.reproduction.ReproducerTemplate

class JunitReproducerTemplate(private val instance: Any, private val method: Method) : ReproducerTemplate {
    override fun buildReproducer(identifier: String, code: CodeBlock, imports: List<KotlinpoetImport>): String {
        val fullClassName = instance::class.java.name
        val packageName = fullClassName.substringBeforeLast('.', missingDelimiterValue = "")

        val testFunction = FunSpec.builder("`${method.name} reproducer $identifier`")
            .addAnnotation(ClassName("org.junit.jupiter.api", "Test"))
            .returns(Unit::class)
            .addCode(code)
            .build()

        val objectSpec = TypeSpec.objectBuilder("Reproducer_$identifier")
            .addFunction(testFunction)
            .build()

        val fileSpec = FileSpec.builder(packageName, "")
            .addType(objectSpec)

        imports.forEach {
            fileSpec.addImport(it.packageName, it.className)
        }

        return fileSpec.build().toString()
    }
}
