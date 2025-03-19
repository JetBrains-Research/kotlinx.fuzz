package kotlinx.fuzz.junit

import com.squareup.kotlinpoet.*
import java.lang.reflect.Method
import kotlinx.fuzz.reproduction.TestReproducerTemplate

class JunitReproducerTemplate(private val instance: Any, private val method: Method) : TestReproducerTemplate {
    override fun buildReproducer(
        identifier: String,
        testCode: CodeBlock,
        imports: List<String>,
        additionalCode: String,
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

        val packageString = fileSpec.build().toString().split("\n")[0]
        val importsString = imports.joinToString("\n")
        val fileString = fileSpec.build()
            .toString()
            .split("\n")
            .drop(2)
            .joinToString("\n")

        return "$packageString\n\n$importsString\n$fileString\n$additionalCode"
    }
}
