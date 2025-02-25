package kotlinx.fuzz.reproduction

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.lang.reflect.Method
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.*
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.jvmErasure

class ListAnyReproducer(
    private val template: ReproducerTemplate,
    private val instance: Any,
    private val method: Method,
) : CrashReproducer {
    private fun arrayToString(executionResult: ExecutionResult): String =
        when {
            executionResult.typeName.startsWith("Boolean") ->
                "booleanArrayOf(${(executionResult.value as BooleanArray).joinToString(", ")})"

            executionResult.typeName.startsWith("Byte") ->
                "byteArrayOf(${(executionResult.value as ByteArray).joinToString(", ") { "$it as Byte" }})"

            executionResult.typeName.startsWith("Short") ->
                "shortArrayOf(${(executionResult.value as ShortArray).joinToString(", ") { "$it as Short" }})"

            executionResult.typeName.startsWith("Int") ->
                "intArrayOf(${(executionResult.value as IntArray).joinToString(", ")})"

            executionResult.typeName.startsWith("Long") ->
                "longArrayOf(${(executionResult.value as LongArray).joinToString(", ")})"

            executionResult.typeName.startsWith("Float") ->
                "floatArrayOf(${(executionResult.value as FloatArray).joinToString(", ") { "$it as Float" }})"

            executionResult.typeName.startsWith("Double") ->
                "doubleArrayOf(${(executionResult.value as DoubleArray).joinToString(", ")})"

            executionResult.typeName.startsWith("Char") ->
                "charArrayOf(${(executionResult.value as CharArray).joinToString(", ")})"

            else -> error("Unsupported execution result type: ${executionResult.typeName}")
        }

    private fun CodeBlock.Builder.addFunction(function: KFunction<*>) {
        val returnType = function.returnType.jvmErasure.asTypeName()
        val isNullable = function.returnType.isMarkedNullable
        val castOperator = if (isNullable) "as?" else "as"

        val parameters = function.parameters.drop(1)

        add("override fun %N(", function.name)
        parameters.forEachIndexed { index, param ->
            val paramType = param.type.jvmErasure.asTypeName()

            val resolvedParamType = if (param.type.arguments.isNotEmpty()) {
                val typeArguments = param.type.arguments
                val resolvedArguments = typeArguments.map {
                    it.type?.jvmErasure?.asTypeName() ?: TypeVariableName("T")
                }
                paramType.parameterizedBy(*resolvedArguments.toTypedArray())
            } else {
                paramType
            }

            val finalParamType = if (param.type.isMarkedNullable) {
                resolvedParamType.copy(nullable = true)
            } else {
                resolvedParamType
            }

            add("%N: %T", param.name, finalParamType)
            if (index < parameters.size - 1) {
                add(", ")
            }
        }
        add(
            "): %T = iterator.next() $castOperator %T\n",
            returnType.copy(nullable = isNullable),
            returnType.copy(nullable = isNullable),
        )
    }

    private fun buildListReproducerObject(): CodeBlock = buildCodeBlock {
        addStatement("val listReproducer = object : %T {", ClassName("kotlinx.fuzz", "KFuzzer"))
        indent()

        addStatement("val iterator = values.iterator()")

        for (function in kotlinx.fuzz.KFuzzer::class.declaredFunctions) {
            addFunction(function)
        }

        unindent()
        addStatement("}")
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun writeToFile(input: ByteArray, reproducerFile: Path) {
        val objectCode = buildListReproducerObject()
        val instanceString = method.declaringClass.kotlin.objectInstance?.let {
            method.declaringClass.kotlin.simpleName
        } ?: "${method.declaringClass.kotlin.simpleName}::class.java.getDeclaredConstructor().newInstance()"
        val code = buildCodeBlock {
            addStatement(
                "val values = listOf<Any?>(" +
                    registerOutputs(instance, method, input).joinToString(", ") { executionResult ->
                        executionResult.value?.let {
                            if (executionResult.typeName.contains("Array")) {
                                arrayToString(executionResult)
                            } else {
                                executionResult.value.toString()
                            }
                        } ?: "null"
                    } +
                    ")",
            )
            add(objectCode)
            addStatement("$instanceString.`${method.name}`(listReproducer)")
        }

        reproducerFile.writeText(
            template.buildReproducer(
                MessageDigest.getInstance("SHA-1").digest(input).toHexString(),
                code,
                emptyList(),
            ),
        )
    }
}
