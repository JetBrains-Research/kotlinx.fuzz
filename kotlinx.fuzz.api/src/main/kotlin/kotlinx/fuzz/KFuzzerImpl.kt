package kotlinx.fuzz

import com.github.curiousoddman.rgxgen.RgxGen
import java.math.BigDecimal
import java.math.MathContext
import java.nio.charset.Charset
import java.util.*
import kotlinx.fuzz.KFuzzer.RegexConfiguration

class KFuzzerImpl(data: ByteArray) : KFuzzer, Random() {
    private val iterator = Reader(data)

    private operator fun IntRange.contains(other: IntRange): Boolean =
        other.first >= this.first && other.last <= this.last

    private inline fun <reified T : Number> fitIntoIntRange(n: T, range: IntRange): T {
        val rangeSize = range.last.toLong() - range.first + 1
        return when (n) {
            is Byte -> ((n.toLong() - Byte.MIN_VALUE) % rangeSize + range.first).toByte() as T
            is Short -> ((n.toLong() - Short.MIN_VALUE) % rangeSize + range.first).toShort() as T
            is Int -> ((n.toLong() - Int.MIN_VALUE) % rangeSize + range.first).toInt() as T
            else -> error("Not integer type")
        }
    }

    private fun fitIntoBigDecimalRange(
        value: BigDecimal,
        oldMin: BigDecimal,
        oldMax: BigDecimal,
        newMin: BigDecimal,
        newMax: BigDecimal,
    ): BigDecimal {
        val normalized = value.subtract(oldMin, MathContext.DECIMAL128)
            .divide(oldMax.subtract(oldMin, MathContext.DECIMAL128), MathContext.DECIMAL128)

        return newMin.add(
            normalized.multiply(newMax.subtract(newMin, MathContext.DECIMAL128), MathContext.DECIMAL128),
            MathContext.DECIMAL128,
        )
    }

    /**
     * Implementation taken from #Random.next
     */
    override fun next(bits: Int): Int = (consumeInt() ushr (48 - bits)).toInt()

    override fun consumeBoolean(): Boolean = iterator.readBoolean()

    override fun consumeBooleanOrNull(): Boolean? = when {
        iterator.readBoolean() -> iterator.readBoolean()
        else -> null
    }

    override fun consumeBooleans(maxLength: Int): BooleanArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Boolean>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeBoolean())
        }
        return list.toBooleanArray()
    }

    override fun consumeBooleansOrNull(maxLength: Int): BooleanArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeBooleans(maxLength)
            else -> null
        }
    }

    override fun consumeByte(range: IntRange): Byte {
        require(range.isNotEmpty()) { "range is empty" }
        require(range in Byte.MIN_VALUE..Byte.MAX_VALUE) { "range should be a subset of [Byte.MIN_VALUE..Byte.MAX_VALUE] but was $range" }

        return fitIntoIntRange(iterator.readByte(), range)
    }

    override fun consumeByteOrNull(range: IntRange): Byte? {
        require(range.isNotEmpty()) { "range is empty" }
        require(range in Byte.MIN_VALUE..Byte.MAX_VALUE) { "range should be a subset of [Byte.MIN_VALUE..Byte.MAX_VALUE] but was $range" }

        return when {
            consumeBoolean() -> consumeByte(range)
            else -> null
        }
    }

    override fun consumeBytes(maxLength: Int, range: IntRange): ByteArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Byte>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeByte(range))
        }
        return list.toByteArray()
    }

    override fun consumeBytesOrNull(maxLength: Int, range: IntRange): ByteArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeBytes(maxLength, range)
            else -> null
        }
    }

    override fun consumeRemainingAsByteArray(): ByteArray {
        val list = mutableListOf<Byte>()
        while (!iterator.isInputFinished()) {
            list.add(consumeByte())
        }
        return list.toByteArray()
    }

    override fun consumeShort(range: IntRange): Short {
        require(range.isNotEmpty()) { "range is empty" }
        require(range in Short.MIN_VALUE..Short.MAX_VALUE) { "range should be a subset of [Short.MIN_VALUE..Short.MAX_VALUE] but was $range" }

        return fitIntoIntRange(iterator.readShort(), range)
    }

    override fun consumeShortOrNull(range: IntRange): Short? {
        require(range.isNotEmpty()) { "range is empty" }
        require(range in Short.MIN_VALUE..Short.MAX_VALUE) { "range should be a subset of [Short.MIN_VALUE..Short.MAX_VALUE] but was $range" }

        return when {
            consumeBoolean() -> consumeShort(range)
            else -> null
        }
    }

    override fun consumeShorts(maxLength: Int, range: IntRange): ShortArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Short>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeShort(range))
        }
        return list.toShortArray()
    }

    override fun consumeShortsOrNull(maxLength: Int, range: IntRange): ShortArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeShorts(maxLength, range)
            else -> null
        }
    }

    override fun consumeInt(range: IntRange): Int {
        require(range.isNotEmpty()) { "range is empty" }

        return fitIntoIntRange(iterator.readInt(), range)
    }

    override fun consumeIntOrNull(range: IntRange): Int? {
        require(range.isNotEmpty()) { "range is empty" }

        return when {
            consumeBoolean() -> consumeInt(range)
            else -> null
        }
    }

    override fun consumeInts(maxLength: Int, range: IntRange): IntArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Int>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeInt(range))
        }
        return list.toIntArray()
    }

    override fun consumeIntsOrNull(maxLength: Int, range: IntRange): IntArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeInts(maxLength, range)
            else -> null
        }
    }

    private fun fitIntoIntRange(result: Long, range: LongRange): Long = when {
        range.first < 0 && range.last > 0 && range.last - Long.MAX_VALUE > range.first -> when {
            result < range.first -> result - Long.MIN_VALUE + range.first

            result <= range.last -> {
                val normalized = result + (range.first - Long.MIN_VALUE)
                when {
                    normalized > range.last -> range.first + (normalized - range.last)
                    else -> normalized
                }
            }

            else -> range.first + (range.first - Long.MIN_VALUE) + (result - range.last) - 1L
        }

        else -> {
            val rangeSize = range.last - range.first + 1
            var normalized = result % rangeSize - Long.MIN_VALUE % rangeSize + range.first
            while (result < range.first) {
                normalized += rangeSize
            }
            normalized
        }
    }

    override fun consumeLong(range: LongRange): Long {
        require(range.isNotEmpty()) { "range is empty" }
        return fitIntoIntRange(iterator.readLong(), range)
    }

    override fun consumeLongOrNull(range: LongRange): Long? {
        require(range.isNotEmpty()) { "range is empty" }

        return when {
            consumeBoolean() -> consumeLong(range)
            else -> null
        }
    }

    override fun consumeLongs(maxLength: Int, range: LongRange): LongArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Long>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeLong(range))
        }
        return list.toLongArray()
    }

    override fun consumeLongsOrNull(maxLength: Int, range: LongRange): LongArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeLongs(maxLength, range)
            else -> null
        }
    }

    override fun consumeFloat(range: FloatRange): Float {
        require(range.isNotEmpty()) { "range is empty" }

        return fitIntoBigDecimalRange(
            iterator.readFloat().toBigDecimal(),
            Float.MIN_VALUE.toBigDecimal(),
            Float.MAX_VALUE.toBigDecimal(),
            range.start.toBigDecimal(),
            range.endInclusive.toBigDecimal(),
        ).toFloat()
    }

    override fun consumeFloatOrNull(range: FloatRange): Float? {
        require(range.isNotEmpty()) { "range is empty" }

        return when {
            consumeBoolean() -> consumeFloat(range)
            else -> null
        }
    }

    override fun consumeFloats(maxLength: Int, range: FloatRange): FloatArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Float>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeFloat(range))
        }
        return list.toFloatArray()
    }

    override fun consumeFloatsOrNull(maxLength: Int, range: FloatRange): FloatArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeFloats(maxLength, range)
            else -> null
        }
    }

    override fun consumeDouble(range: DoubleRange): Double {
        require(range.isNotEmpty()) { "range is empty" }

        return fitIntoBigDecimalRange(
            iterator.readDouble().toBigDecimal(),
            Double.MIN_VALUE.toBigDecimal(),
            Double.MAX_VALUE.toBigDecimal(),
            range.start.toBigDecimal(),
            range.endInclusive.toBigDecimal(),
        ).toDouble()
    }

    override fun consumeDoubleOrNull(range: DoubleRange): Double? {
        require(range.isNotEmpty()) { "range is empty" }

        return when {
            consumeBoolean() -> consumeDouble(range)
            else -> null
        }
    }

    override fun consumeDoubles(maxLength: Int, range: DoubleRange): DoubleArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Double>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeDouble(range))
        }
        return list.toDoubleArray()
    }

    override fun consumeDoublesOrNull(maxLength: Int, range: DoubleRange): DoubleArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeDoubles(maxLength, range)
            else -> null
        }
    }

    override fun consumeChar(range: CharRange): Char {
        require(range.isNotEmpty()) { "range is empty" }

        return (consumeShort((range.first.code + Short.MIN_VALUE)..(range.last.code + Short.MIN_VALUE)) - Short.MIN_VALUE).toChar()
    }

    override fun consumeCharOrNull(range: CharRange): Char? {
        require(range.isNotEmpty()) { "range is empty" }

        return when {
            consumeBoolean() -> consumeChar(range)
            else -> null
        }
    }

    override fun consumeChars(maxLength: Int, range: CharRange): CharArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Char>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeChar(range))
        }
        return list.toCharArray()
    }

    override fun consumeCharsOrNull(maxLength: Int, range: CharRange): CharArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeChars(maxLength, range)
            else -> null
        }
    }

    override fun consumeString(maxLength: Int, charset: Charset): String {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val length = consumeInt(0..maxLength)
        val byteBuffer = mutableListOf<Byte>()

        while (true) {
            byteBuffer.add(iterator.readByte())
            if (String(byteBuffer.toByteArray(), charset).length >= length) {
                break
            }
        }

        return String(byteBuffer.toByteArray(), charset).take(length)
    }

    override fun consumeStringOrNull(maxLength: Int, charset: Charset): String? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeString(maxLength, charset)
            else -> null
        }
    }

    override fun consumeRemainingAsString(charset: Charset): String {
        val bytes = mutableListOf<Byte>()
        while (!iterator.isInputFinished()) {
            bytes.add(iterator.readByte())
        }
        return String(bytes.toByteArray(), charset)
    }

    override fun consumeAsciiString(maxLength: Int): String = consumeString(maxLength, Charsets.US_ASCII)

    override fun consumeAsciiStringOrNull(maxLength: Int): String? = consumeStringOrNull(maxLength, Charsets.US_ASCII)

    override fun consumeRemainingAsAsciiString(): String = consumeRemainingAsString(Charsets.US_ASCII)

    override fun consumeLetter(): Char {
        val alphabetSize = ('z' - 'a' + 1) * 2
        val index = consumeByte(0 until alphabetSize)
        return when {
            index < alphabetSize / 2 -> 'a' + index.toInt()
            else -> 'A' + index.toInt() - (alphabetSize / 2)
        }
    }

    override fun consumeLetterOrNull(): Char? = when {
        consumeBoolean() -> consumeLetter()
        else -> null
    }

    override fun consumeLetters(maxLength: Int): CharArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Char>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeLetter())
        }
        return list.toCharArray()
    }

    override fun consumeLettersOrNull(maxLength: Int): CharArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        if (consumeBoolean()) {
            return null
        }

        val list = mutableListOf<Char>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(consumeLetter())
        }
        return list.toCharArray()
    }

    override fun consumeLetterString(maxLength: Int): String {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val length = consumeInt(0..maxLength)
        return String(consumeLetters(length))
    }

    override fun consumeLetterStringOrNull(maxLength: Int): String? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            consumeBoolean() -> consumeLetterString(maxLength)
            else -> null
        }
    }

    override fun consumeRemainingAsLetterString(): String = buildString {
        while (!iterator.isInputFinished()) {
            append(consumeLetter())
        }
    }

    override fun consumeRegexString(regex: Regex, configuration: RegexConfiguration): String {
        val rgxGen = RgxGen.parse(configuration.asRegexProperties(), regex.pattern)
        return rgxGen.generate(this)
    }

    override fun consumeRegexStringOrNull(regex: Regex, configuration: RegexConfiguration) =
        if (consumeBoolean()) null else consumeRegexString(regex, configuration)

    private class Reader(data: ByteArray) {
        private val iterator = data.iterator()

        fun isInputFinished(): Boolean = !iterator.hasNext()

        fun readBoolean(): Boolean = readByte() != 0.toByte()

        fun readByte(): Byte = when {
            iterator.hasNext() -> iterator.next()
            else -> 0
        }

        fun readShort(): Short = (readByte().toInt() shl 8 or (readByte().toInt() and 0xFF)).toShort()

        fun readInt(): Int =
            (readByte().toInt() shl 24) or ((readByte().toInt() and 0xFF) shl 16) or
                ((readByte().toInt() and 0xFF) shl 8) or (readByte().toInt() and 0xFF)

        fun readLong(): Long = (readInt().toLong() shl 32) or (readInt().toLong() and 0xFF_FF_FF_FFL)

        fun readFloat(): Float = Float.fromBits(readInt())

        fun readDouble(): Double = Double.fromBits(readLong())
    }
}
