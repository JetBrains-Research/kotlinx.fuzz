package kotlinx.fuzz.reproduction

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.nio.charset.Charset
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.jvmErasure
import kotlinx.fuzz.*

class OutputRegisteringKFuzzer(data: ByteArray) : KFuzzer {
    private val fuzzer = KFuzzerImpl(data)
    private val mutableValues = mutableListOf<Any?>()
    val values get() = mutableValues.toList()

    override fun boolean(): Boolean {
        mutableValues.add(fuzzer.boolean())
        return mutableValues.last() as Boolean
    }

    override fun booleanOrNull(): Boolean? {
        mutableValues.add(fuzzer.booleanOrNull())
        return mutableValues.last() as Boolean?
    }

    override fun booleans(maxLength: Int): BooleanArray {
        mutableValues.add(fuzzer.booleans(maxLength))
        return mutableValues.last() as BooleanArray
    }

    override fun booleansOrNull(maxLength: Int): BooleanArray? {
        mutableValues.add(fuzzer.booleansOrNull(maxLength))
        return mutableValues.last() as BooleanArray?
    }

    override fun byte(range: IntRange): Byte {
        mutableValues.add(fuzzer.byte(range))
        return mutableValues.last() as Byte
    }

    override fun byteOrNull(range: IntRange): Byte? {
        mutableValues.add(fuzzer.byteOrNull(range))
        return mutableValues.last() as Byte?
    }

    override fun bytes(maxLength: Int, range: IntRange): ByteArray {
        mutableValues.add(fuzzer.bytes(maxLength, range))
        return mutableValues.last() as ByteArray
    }

    override fun bytesOrNull(maxLength: Int, range: IntRange): ByteArray? {
        mutableValues.add(fuzzer.bytesOrNull(maxLength, range))
        return mutableValues.last() as ByteArray?
    }

    override fun remainingAsByteArray(): ByteArray {
        mutableValues.add(fuzzer.remainingAsByteArray())
        return mutableValues.last() as ByteArray
    }

    override fun short(range: IntRange): Short {
        mutableValues.add(fuzzer.short(range))
        return mutableValues.last() as Short
    }

    override fun shortOrNull(range: IntRange): Short? {
        mutableValues.add(fuzzer.shortOrNull(range))
        return mutableValues.last() as Short?
    }

    override fun shorts(maxLength: Int, range: IntRange): ShortArray {
        mutableValues.add(fuzzer.shorts(maxLength, range))
        return mutableValues.last() as ShortArray
    }

    override fun shortsOrNull(maxLength: Int, range: IntRange): ShortArray? {
        mutableValues.add(fuzzer.shortsOrNull(maxLength, range))
        return mutableValues.last() as ShortArray?
    }

    override fun int(range: IntRange): Int {
        mutableValues.add(fuzzer.int(range))
        return mutableValues.last() as Int
    }

    override fun intOrNull(range: IntRange): Int? {
        mutableValues.add(fuzzer.intOrNull(range))
        return mutableValues.last() as Int?
    }

    override fun ints(maxLength: Int, range: IntRange): IntArray {
        mutableValues.add(fuzzer.ints(maxLength, range))
        return mutableValues.last() as IntArray
    }

    override fun intsOrNull(maxLength: Int, range: IntRange): IntArray? {
        mutableValues.add(fuzzer.intsOrNull(maxLength, range))
        return mutableValues.last() as IntArray?
    }

    override fun long(range: LongRange): Long {
        mutableValues.add(fuzzer.long(range))
        return mutableValues.last() as Long
    }

    override fun longOrNull(range: LongRange): Long? {
        mutableValues.add(fuzzer.longOrNull(range))
        return mutableValues.last() as Long?
    }

    override fun longs(maxLength: Int, range: LongRange): LongArray {
        mutableValues.add(fuzzer.longs(maxLength, range))
        return mutableValues.last() as LongArray
    }

    override fun longsOrNull(maxLength: Int, range: LongRange): LongArray? {
        mutableValues.add(fuzzer.longsOrNull(maxLength, range))
        return mutableValues.last() as LongArray?
    }

    override fun float(range: FloatRange): Float {
        mutableValues.add(fuzzer.float(range))
        return mutableValues.last() as Float
    }

    override fun floatOrNull(range: FloatRange): Float? {
        mutableValues.add(fuzzer.floatOrNull(range))
        return mutableValues.last() as Float?
    }

    override fun floats(maxLength: Int, range: FloatRange): FloatArray {
        mutableValues.add(fuzzer.floats(maxLength, range))
        return mutableValues.last() as FloatArray
    }

    override fun floatsOrNull(maxLength: Int, range: FloatRange): FloatArray? {
        mutableValues.add(fuzzer.floatsOrNull(maxLength, range))
        return mutableValues.last() as FloatArray?
    }

    override fun double(range: DoubleRange): Double {
        mutableValues.add(fuzzer.double(range))
        return mutableValues.last() as Double
    }

    override fun doubleOrNull(range: DoubleRange): Double? {
        mutableValues.add(fuzzer.doubleOrNull(range))
        return mutableValues.last() as Double?
    }

    override fun doubles(maxLength: Int, range: DoubleRange): DoubleArray {
        mutableValues.add(fuzzer.doubles(maxLength, range))
        return mutableValues.last() as DoubleArray
    }

    override fun doublesOrNull(maxLength: Int, range: DoubleRange): DoubleArray? {
        mutableValues.add(fuzzer.doublesOrNull(maxLength, range))
        return mutableValues.last() as DoubleArray?
    }

    override fun char(range: CharRange): Char {
        mutableValues.add(fuzzer.char(range))
        return mutableValues.last() as Char
    }

    override fun char(charset: Charset): Char {
        mutableValues.add(fuzzer.char(charset))
        return mutableValues.last() as Char
    }

    override fun char(charset: CharacterSet): Char {
        mutableValues.add(fuzzer.char(charset))
        return mutableValues.last() as Char
    }

    override fun charOrNull(range: CharRange): Char? {
        mutableValues.add(fuzzer.charOrNull(range))
        return mutableValues.last() as Char?
    }

    override fun charOrNull(charset: Charset): Char? {
        mutableValues.add(fuzzer.charOrNull(charset))
        return mutableValues.last() as Char?
    }

    override fun charOrNull(charset: CharacterSet): Char? {
        mutableValues.add(fuzzer.charOrNull(charset))
        return mutableValues.last() as Char?
    }

    override fun chars(maxLength: Int, range: CharRange): CharArray {
        mutableValues.add(fuzzer.chars(maxLength, range))
        return mutableValues.last() as CharArray
    }

    override fun chars(maxLength: Int, charset: Charset): CharArray {
        mutableValues.add(fuzzer.chars(maxLength, charset))
        return mutableValues.last() as CharArray
    }

    override fun chars(maxLength: Int, charset: CharacterSet): CharArray {
        mutableValues.add(fuzzer.chars(maxLength, charset))
        return mutableValues.last() as CharArray
    }

    override fun charsOrNull(maxLength: Int, range: CharRange): CharArray? {
        mutableValues.add(fuzzer.charsOrNull(maxLength, range))
        return mutableValues.last() as CharArray?
    }

    override fun charsOrNull(maxLength: Int, charset: Charset): CharArray? {
        mutableValues.add(fuzzer.charsOrNull(maxLength, charset))
        return mutableValues.last() as CharArray?
    }

    override fun charsOrNull(maxLength: Int, charset: CharacterSet): CharArray? {
        mutableValues.add(fuzzer.charsOrNull(maxLength, charset))
        return mutableValues.last() as CharArray?
    }

    override fun string(maxLength: Int, charset: Charset): String {
        mutableValues.add(fuzzer.string(maxLength, charset))
        return mutableValues.last() as String
    }

    override fun string(maxLength: Int, charset: CharacterSet): String {
        mutableValues.add(fuzzer.string(maxLength, charset))
        return mutableValues.last() as String
    }

    override fun string(regex: Regex, configuration: KFuzzer.RegexConfiguration): String {
        mutableValues.add(fuzzer.string(regex, configuration))
        return mutableValues.last() as String
    }

    override fun stringOrNull(maxLength: Int, charset: Charset): String? {
        mutableValues.add(fuzzer.stringOrNull(maxLength, charset))
        return mutableValues.last() as String?
    }

    override fun stringOrNull(maxLength: Int, charset: CharacterSet): String? {
        mutableValues.add(fuzzer.stringOrNull(maxLength, charset))
        return mutableValues.last() as String?
    }

    override fun stringOrNull(regex: Regex, configuration: KFuzzer.RegexConfiguration): String? {
        mutableValues.add(fuzzer.stringOrNull(regex, configuration))
        return mutableValues.last() as String?
    }

    override fun remainingAsString(charset: Charset): String {
        mutableValues.add(fuzzer.remainingAsString(charset))
        return mutableValues.last() as String
    }

    override fun remainingAsString(charset: CharacterSet): String {
        mutableValues.add(fuzzer.remainingAsString(charset))
        return mutableValues.last() as String
    }
}

internal fun arrayToString(value: Any): String = when (value) {
    is BooleanArray -> "booleanArrayOf(${value.joinToString(", ")})"
    is ByteArray -> "byteArrayOf(${value.joinToString(", ") { "$it as Byte" }})"
    is ShortArray -> "shortArrayOf(${value.joinToString(", ") { "$it as Short" }})"
    is IntArray -> "intArrayOf(${value.joinToString(", ")})"
    is LongArray -> "longArrayOf(${value.joinToString(", ")})"
    is FloatArray -> "floatArrayOf(${value.joinToString(", ") { "$it as Float" }})"
    is DoubleArray -> "doubleArrayOf(${value.joinToString(", ")})"
    is CharArray -> "charArrayOf(${value.joinToString(", ")})"
    else -> error("Unsupported execution result type: ${value::class.qualifiedName}")
}

internal fun isArray(value: Any) = when (value) {
    is BooleanArray -> true
    is ByteArray -> true
    is ShortArray -> true
    is IntArray -> true
    is LongArray -> true
    is FloatArray -> true
    is DoubleArray -> true
    is CharArray -> true
    else -> false
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

internal fun toCodeString(value: Any?) = value?.let {
    when {
        isArray(value) -> arrayToString(value)
        value is String -> "\"$value\""
        value is Char -> "\'$value\'"
        else -> value.toString()
    }
} ?: "null"

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
