package kotlinx.fuzz.gradle.junit

import com.squareup.kotlinpoet.*
import java.lang.reflect.Method
import kotlinx.fuzz.reproduction.KotlinpoetImport
import kotlinx.fuzz.reproduction.ReproducerTestTemplate

class JunitReproducerTemplate(private val instance: Any, private val method: Method) : ReproducerTestTemplate {
    override fun buildReproducer(
        identifier: String,
        testCode: CodeBlock,
        imports: List<KotlinpoetImport>,
        additionalObjects: List<TypeSpec>,
        additionalFunctions: List<FunSpec>,
    ): String {
        val fullClassName = instance::class.java.name
        val packageName = fullClassName.substringBeforeLast('.', missingDelimiterValue = "")

        val testFunction = FunSpec.builder("`${method.name} reproducer $identifier`")
            .addAnnotation(ClassName("org.junit.jupiter.api", "Test"))
            .returns(Unit::class)
            .addCode(testCode)
            .build()

        val objectSpec = TypeSpec.objectBuilder("Reproducer_$identifier")
            .addFunction(testFunction)
            .build()

        val fileSpec = FileSpec.builder(packageName, "")
            .addType(objectSpec)
            .addTypes(additionalObjects)
            .addFunctions(additionalFunctions)

        imports.forEach {
            fileSpec.addImport(it.packageName, it.className)
        }

        return fileSpec.build().toString()
    }
}
