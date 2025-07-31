package kotlinx.fuzz.reproducer

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import java.lang.reflect.Method
import kotlinx.fuzz.log.LoggerFacade

class JunitReproducerTemplate(
    private val instance: Any,
    private val method: Method,
) : TestReproducerTemplate {
    private val log = LoggerFacade.getLogger<JunitReproducerTemplate>()

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
        for (import in imports) {
            val pkgName = import.substringBeforeLast('.')
            val importName = import.substringAfterLast('.')
            when (importName) {
                "*" -> {
                    log.warn("Reproducers do not support wildcard imports")
                    log.warn("Please add all the necessary imports to the reproducer test manually")
                    fileSpec.addFileComment("import $import")
                }
                else -> fileSpec.addImport(pkgName, importName)
            }
        }

        return "${fileSpec.build()}\n$additionalCode"
    }
}
