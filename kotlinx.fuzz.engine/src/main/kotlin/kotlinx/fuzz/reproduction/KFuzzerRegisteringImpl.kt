package kotlinx.fuzz.reproduction

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.nio.charset.Charset
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.jvmErasure
import kotlinx.fuzz.*

data class ExecutionResult(val typeName: String, val value: Any?)

class KFuzzerRegisteringImpl(data: ByteArray) : KFuzzer {
    private val fuzzer = KFuzzerImpl(data)
    val values: MutableList<ExecutionResult> = mutableListOf()

    override fun boolean(): Boolean {
        values.add(ExecutionResult("Boolean", fuzzer.boolean()))
        return values.last().value as Boolean
    }

    override fun booleanOrNull(): Boolean? {
        values.add(ExecutionResult("Boolean?", fuzzer.booleanOrNull()))
        return values.last().value as Boolean?
    }

    override fun booleans(maxLength: Int): BooleanArray {
        values.add(ExecutionResult("BooleanArray", fuzzer.booleans(maxLength)))
        return values.last().value as BooleanArray
    }

    override fun booleansOrNull(maxLength: Int): BooleanArray? {
        values.add(ExecutionResult("BooleanArray?", fuzzer.booleansOrNull(maxLength)))
        return values.last().value as BooleanArray?
    }

    override fun byte(range: IntRange): Byte {
        values.add(ExecutionResult("Byte", fuzzer.byte(range)))
        return values.last().value as Byte
    }

    override fun byteOrNull(range: IntRange): Byte? {
        values.add(ExecutionResult("Byte?", fuzzer.byteOrNull(range)))
        return values.last().value as Byte?
    }

    override fun bytes(maxLength: Int, range: IntRange): ByteArray {
        values.add(ExecutionResult("ByteArray", fuzzer.bytes(maxLength, range)))
        return values.last().value as ByteArray
    }

    override fun bytesOrNull(maxLength: Int, range: IntRange): ByteArray? {
        values.add(ExecutionResult("ByteArray?", fuzzer.bytesOrNull(maxLength, range)))
        return values.last().value as ByteArray?
    }

    override fun remainingAsByteArray(): ByteArray {
        values.add(ExecutionResult("ByteArray", fuzzer.remainingAsByteArray()))
        return values.last().value as ByteArray
    }

    override fun short(range: IntRange): Short {
        values.add(ExecutionResult("Short", fuzzer.short(range)))
        return values.last().value as Short
    }

    override fun shortOrNull(range: IntRange): Short? {
        values.add(ExecutionResult("Short?", fuzzer.shortOrNull(range)))
        return values.last().value as Short?
    }

    override fun shorts(maxLength: Int, range: IntRange): ShortArray {
        values.add(ExecutionResult("ShortArray", fuzzer.shorts(maxLength, range)))
        return values.last().value as ShortArray
    }

    override fun shortsOrNull(maxLength: Int, range: IntRange): ShortArray? {
        values.add(ExecutionResult("ShortArray?", fuzzer.shortsOrNull(maxLength, range)))
        return values.last().value as ShortArray?
    }

    override fun int(range: IntRange): Int {
        values.add(ExecutionResult("Int", fuzzer.int(range)))
        return values.last().value as Int
    }

    override fun intOrNull(range: IntRange): Int? {
        values.add(ExecutionResult("Int?", fuzzer.intOrNull(range)))
        return values.last().value as Int?
    }

    override fun ints(maxLength: Int, range: IntRange): IntArray {
        values.add(ExecutionResult("IntArray", fuzzer.ints(maxLength, range)))
        return values.last().value as IntArray
    }

    override fun intsOrNull(maxLength: Int, range: IntRange): IntArray? {
        values.add(ExecutionResult("IntArray?", fuzzer.intsOrNull(maxLength, range)))
        return values.last().value as IntArray?
    }

    override fun long(range: LongRange): Long {
        values.add(ExecutionResult("Long", fuzzer.long(range)))
        return values.last().value as Long
    }

    override fun longOrNull(range: LongRange): Long? {
        values.add(ExecutionResult("Long?", fuzzer.longOrNull(range)))
        return values.last().value as Long?
    }

    override fun longs(maxLength: Int, range: LongRange): LongArray {
        values.add(ExecutionResult("LongArray", fuzzer.longs(maxLength, range)))
        return values.last().value as LongArray
    }

    override fun longsOrNull(maxLength: Int, range: LongRange): LongArray? {
        values.add(ExecutionResult("LongArray?", fuzzer.longsOrNull(maxLength, range)))
        return values.last().value as LongArray?
    }

    override fun float(range: FloatRange): Float {
        values.add(ExecutionResult("Float", fuzzer.float(range)))
        return values.last().value as Float
    }

    override fun floatOrNull(range: FloatRange): Float? {
        values.add(ExecutionResult("Float?", fuzzer.floatOrNull(range)))
        return values.last().value as Float?
    }

    override fun floats(maxLength: Int, range: FloatRange): FloatArray {
        values.add(ExecutionResult("FloatArray", fuzzer.floats(maxLength, range)))
        return values.last().value as FloatArray
    }

    override fun floatsOrNull(maxLength: Int, range: FloatRange): FloatArray? {
        values.add(ExecutionResult("FloatArray?", fuzzer.floatsOrNull(maxLength, range)))
        return values.last().value as FloatArray?
    }

    override fun double(range: DoubleRange): Double {
        values.add(ExecutionResult("Double", fuzzer.double(range)))
        return values.last().value as Double
    }

    override fun doubleOrNull(range: DoubleRange): Double? {
        values.add(ExecutionResult("Double?", fuzzer.doubleOrNull(range)))
        return values.last().value as Double?
    }

    override fun doubles(maxLength: Int, range: DoubleRange): DoubleArray {
        values.add(ExecutionResult("DoubleArray", fuzzer.doubles(maxLength, range)))
        return values.last().value as DoubleArray
    }

    override fun doublesOrNull(maxLength: Int, range: DoubleRange): DoubleArray? {
        values.add(ExecutionResult("DoubleArray?", fuzzer.doublesOrNull(maxLength, range)))
        return values.last().value as DoubleArray?
    }

    override fun char(range: CharRange): Char {
        values.add(ExecutionResult("Char", fuzzer.char(range)))
        return values.last().value as Char
    }

    override fun char(charset: Charset): Char {
        values.add(ExecutionResult("Char", fuzzer.char(charset)))
        return values.last().value as Char
    }

    override fun char(charset: CharacterSet): Char {
        values.add(ExecutionResult("Char", fuzzer.char(charset)))
        return values.last().value as Char
    }

    override fun charOrNull(range: CharRange): Char? {
        values.add(ExecutionResult("Char?", fuzzer.charOrNull(range)))
        return values.last().value as Char?
    }

    override fun charOrNull(charset: Charset): Char? {
        values.add(ExecutionResult("Char?", fuzzer.charOrNull(charset)))
        return values.last().value as Char?
    }

    override fun charOrNull(charset: CharacterSet): Char? {
        values.add(ExecutionResult("Char?", fuzzer.charOrNull(charset)))
        return values.last().value as Char?
    }

    override fun chars(maxLength: Int, range: CharRange): CharArray {
        values.add(ExecutionResult("CharArray", fuzzer.chars(maxLength, range)))
        return values.last().value as CharArray
    }

    override fun chars(maxLength: Int, charset: Charset): CharArray {
        values.add(ExecutionResult("CharArray", fuzzer.chars(maxLength, charset)))
        return values.last().value as CharArray
    }

    override fun chars(maxLength: Int, charset: CharacterSet): CharArray {
        values.add(ExecutionResult("CharArray", fuzzer.chars(maxLength, charset)))
        return values.last().value as CharArray
    }

    override fun charsOrNull(maxLength: Int, range: CharRange): CharArray? {
        values.add(ExecutionResult("CharArray?", fuzzer.charsOrNull(maxLength, range)))
        return values.last().value as CharArray?
    }

    override fun charsOrNull(maxLength: Int, charset: Charset): CharArray? {
        values.add(ExecutionResult("CharArray?", fuzzer.charsOrNull(maxLength, charset)))
        return values.last().value as CharArray?
    }

    override fun charsOrNull(maxLength: Int, charset: CharacterSet): CharArray? {
        values.add(ExecutionResult("CharArray?", fuzzer.charsOrNull(maxLength, charset)))
        return values.last().value as CharArray?
    }

    override fun string(maxLength: Int, charset: Charset): String {
        values.add(ExecutionResult("String", fuzzer.string(maxLength, charset)))
        return values.last().value as String
    }

    override fun string(maxLength: Int, charset: CharacterSet): String {
        values.add(ExecutionResult("String", fuzzer.string(maxLength, charset)))
        return values.last().value as String
    }

    override fun string(regex: Regex, configuration: KFuzzer.RegexConfiguration): String {
        values.add(ExecutionResult("String", fuzzer.string(regex, configuration)))
        return values.last().value as String
    }

    override fun stringOrNull(maxLength: Int, charset: Charset): String? {
        values.add(ExecutionResult("String?", fuzzer.stringOrNull(maxLength, charset)))
        return values.last().value as String?
    }

    override fun stringOrNull(maxLength: Int, charset: CharacterSet): String? {
        values.add(ExecutionResult("String?", fuzzer.stringOrNull(maxLength, charset)))
        return values.last().value as String?
    }

    override fun stringOrNull(regex: Regex, configuration: KFuzzer.RegexConfiguration): String? {
        values.add(ExecutionResult("String?", fuzzer.stringOrNull(regex, configuration)))
        return values.last().value as String?
    }

    override fun remainingAsString(charset: Charset): String {
        values.add(ExecutionResult("String", fuzzer.remainingAsString(charset)))
        return values.last().value as String
    }

    override fun remainingAsString(charset: CharacterSet): String {
        values.add(ExecutionResult("String", fuzzer.remainingAsString(charset)))
        return values.last().value as String
    }
}

internal fun arrayToString(executionResult: ExecutionResult): String = when {
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

internal fun buildListReproducerObject(): TypeSpec {
    val result = TypeSpec.classBuilder("ListReproducer")
        .addSuperinterface(KFuzzer::class)
        .addModifiers(KModifier.PRIVATE)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("values", List::class.asClassName().parameterizedBy(ANY.copy(nullable = true)))
                .build(),
        )
        .addProperty(
            PropertySpec.builder("iterator", Iterator::class.asClassName().parameterizedBy(ANY.copy(nullable = true))).initializer("values.iterator()").addModifiers(
                KModifier.PRIVATE,
            )
                .build(),
        )
    for (function in KFuzzer::class.declaredFunctions) {
        result.addFunction(generateFunction(function))
    }
    return result.build()
}

private fun generateFunction(function: KFunction<*>): FunSpec {
    val returnType = function.returnType.jvmErasure.asClassName()
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
