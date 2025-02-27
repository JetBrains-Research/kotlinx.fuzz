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
import kotlinx.fuzz.KFuzzer

class ListAnyReproducerWriter(
    private val template: ReproducerTestTemplate,
    private val instance: Any,
    private val method: Method,
) : CrashReproducerWriter(template, method) {
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

    private fun generateFunction(function: KFunction<*>): FunSpec {
        val returnType = function.returnType.jvmErasure.asTypeName()
        val isNullable = function.returnType.isMarkedNullable
        val castOperator = if (isNullable) "as?" else "as"

        val parameters = function.parameters.drop(1)

        val result = FunSpec.builder(function.name)
            .returns(returnType.copy(nullable = isNullable))
            .addModifiers(KModifier.OVERRIDE)

        parameters.forEach { param ->
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

            result.addParameter(param.name!!, finalParamType)
        }
        result.addStatement("return iterator.next() $castOperator ${returnType.copy(nullable = isNullable)}")
        return result.build()
    }

    private fun buildListReproducerObject(): TypeSpec {
        val result = TypeSpec.classBuilder("ListReproducer")
            .addSuperinterface(KFuzzer::class)
            .addModifiers(KModifier.PRIVATE)
            .primaryConstructor(
                FunSpec.constructorBuilder().addParameter("values", List::class.asClassName().parameterizedBy(ANY.copy(nullable = true))).build(),
            )
            .addProperty(PropertySpec.builder("iterator", Iterator::class.asClassName().parameterizedBy(ANY.copy(nullable = true))).initializer("values.iterator()").addModifiers(
                KModifier.PRIVATE,
            )
                .build())
        for (function in KFuzzer::class.declaredFunctions) {
            result.addFunction(generateFunction(function))
        }
        return result.build()
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun writeToFile(input: ByteArray, reproducerFile: Path) {
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
            addStatement("$instanceString.`${method.name}`(ListReproducer(values))")
        }

        reproducerFile.writeText(
            template.buildReproducer(
                MessageDigest.getInstance("SHA-1").digest(input).toHexString(),
                code,
                additionalObjects = listOf(buildListReproducerObject()),
            ),
        )
    }
}
