package kotlinx.fuzz

import java.math.BigDecimal
import java.math.MathContext
import java.nio.charset.Charset

class KFuzzerImpl(data: ByteArray) : KFuzzer {
    private class Reader(data: ByteArray) {
        private val iterator = data.iterator()

        fun isInputFinished() = !iterator.hasNext()

        fun readBoolean() = readByte() != 0.toByte()

        fun readByte() = if (iterator.hasNext()) {
            iterator.next()
        } else {
            0
        }

        fun readShort() = (readByte().toInt() shl 8 or (readByte().toInt() and 0xFF)).toShort()

        fun readInt() = (readByte().toInt() shl 24) or ((readByte().toInt() and 0xFF) shl 16) or
                ((readByte().toInt() and 0xFF) shl 8) or (readByte().toInt() and 0xFF)

        fun readLong() = (readInt().toLong() shl 32) or (readInt().toLong() and 0xFFFFFFFFL)

        fun readFloat() = Float.fromBits(readInt())

        fun readDouble() = Double.fromBits(readLong())
    }

    private val iterator = Reader(data)

    private operator fun IntRange.contains(other: IntRange): Boolean {
        return other.first >= this.first && other.last <= this.last
    }

    private inline fun <reified T> fitIntoIntRange(n: T, range: IntRange): T {
        val rangeSize = range.last.toLong() - range.first + 1
        return when (T::class) {
            Byte::class -> (((n as Byte).toLong() - Byte.MIN_VALUE) % rangeSize + range.first).toByte() as T
            Short::class -> (((n as Short).toLong() - Short.MIN_VALUE) % rangeSize + range.first).toShort() as T
            Int::class -> (((n as Int).toLong() - Int.MIN_VALUE) % rangeSize + range.first).toInt() as T
            else -> error("Not integer type")
        }
    }

    private fun fitIntoBigDecimalRange(
        value: BigDecimal,
        oldMin: BigDecimal,
        oldMax: BigDecimal,
        newMin: BigDecimal,
        newMax: BigDecimal
    ): BigDecimal {
        val normalized = value.subtract(oldMin, MathContext.DECIMAL128)
            .divide(oldMax.subtract(oldMin, MathContext.DECIMAL128), MathContext.DECIMAL128)

        return newMin.add(
            normalized.multiply(newMax.subtract(newMin, MathContext.DECIMAL128), MathContext.DECIMAL128),
            MathContext.DECIMAL128
        )
    }

    override fun consumeBoolean() = iterator.readBoolean()

    override fun consumeBooleanOrNull() =
        if (iterator.readBoolean()) {
            iterator.readBoolean()
        } else {
            null
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

        if (consumeBoolean()) return null
        return consumeBooleans(maxLength)
    }

    override fun consumeByte(range: IntRange): Byte {
        require(!range.isEmpty()) { "range is empty" }
        require(range in Byte.MIN_VALUE..Byte.MAX_VALUE) { "range should be a subset of [Byte.MIN_VALUE..Byte.MAX_VALUE] but was $range" }

        return fitIntoIntRange(iterator.readByte(), range)
    }

    override fun consumeByteOrNull(range: IntRange): Byte? {
        require(!range.isEmpty()) { "range is empty" }
        require(range in Byte.MIN_VALUE..Byte.MAX_VALUE) { "range should be a subset of [Byte.MIN_VALUE..Byte.MAX_VALUE] but was $range" }

        if (iterator.readBoolean()) return null
        return consumeByte(range)
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

        if (consumeBoolean()) return null
        return consumeBytes(maxLength, range)
    }

    override fun consumeRemainingAsByteArray(): ByteArray {
        val list = mutableListOf<Byte>()
        while (!iterator.isInputFinished()) {
            list.add(consumeByte())
        }
        return list.toByteArray()
    }

    override fun consumeShort(range: IntRange): Short {
        require(!range.isEmpty()) { "range is empty" }
        require(range in Short.MIN_VALUE..Short.MAX_VALUE) { "range should be a subset of [Short.MIN_VALUE..Short.MAX_VALUE] but was $range" }

        return fitIntoIntRange(iterator.readShort(), range)
    }

    override fun consumeShortOrNull(range: IntRange): Short? {
        require(!range.isEmpty()) { "range is empty" }
        require(range in Short.MIN_VALUE..Short.MAX_VALUE) { "range should be a subset of [Short.MIN_VALUE..Short.MAX_VALUE] but was $range" }

        if (iterator.readBoolean()) return null
        return consumeShort(range)
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

        if (consumeBoolean()) return null
        return consumeShorts(maxLength, range)
    }

    override fun consumeInt(range: IntRange): Int {
        require(!range.isEmpty()) { "range is empty" }

        return fitIntoIntRange(iterator.readInt(), range)
    }

    override fun consumeIntOrNull(range: IntRange): Int? {
        require(!range.isEmpty()) { "range is empty" }

        if (iterator.readBoolean()) return null
        return consumeInt(range)
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

        if (consumeBoolean()) return null
        return consumeInts(maxLength, range)
    }

    override fun consumeLong(range: LongRange): Long {
        require(!range.isEmpty()) { "range is empty" }

        var result = iterator.readLong()

        if (range.first < 0 && range.last > 0 && range.last - Long.MAX_VALUE > range.first) {
            if (result < range.first) {
                return result - Long.MIN_VALUE + range.first
            } else if (result <= range.last) {
                result += (range.first - Long.MIN_VALUE)
                if (result > range.last) {
                    return range.first + (result - range.last)
                }
                return result
            } else {
                return range.first + (range.first - Long.MIN_VALUE) + (result - range.last) - 1L
            }
        } else {
            val rangeSize = range.last - range.first + 1
            result = result % rangeSize - Long.MIN_VALUE % rangeSize + range.first
            while (result < range.first) {
                result += rangeSize
            }
            return result
        }
    }

    override fun consumeLongOrNull(range: LongRange): Long? {
        require(!range.isEmpty()) { "range is empty" }

        if (iterator.readBoolean()) return null
        return consumeLong(range)
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

        if (consumeBoolean()) return null
        return consumeLongs(maxLength, range)
    }

    override fun consumeFloat(range: FloatRange): Float {
        require(!range.isEmpty()) { "range is empty" }

        return fitIntoBigDecimalRange(
            iterator.readFloat().toBigDecimal(),
            Float.MIN_VALUE.toBigDecimal(),
            Float.MAX_VALUE.toBigDecimal(),
            range.start.toBigDecimal(),
            range.endInclusive.toBigDecimal()
        ).toFloat()
    }

    override fun consumeFloatOrNull(range: FloatRange): Float? {
        require(!range.isEmpty()) { "range is empty" }

        if (iterator.readBoolean()) return null
        return consumeFloat(range)
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

        if (consumeBoolean()) return null
        return consumeFloats(maxLength, range)
    }

    override fun consumeDouble(range: DoubleRange): Double {
        require(!range.isEmpty()) { "range is empty" }

        return fitIntoBigDecimalRange(
            iterator.readDouble().toBigDecimal(),
            Double.MIN_VALUE.toBigDecimal(),
            Double.MAX_VALUE.toBigDecimal(),
            range.start.toBigDecimal(),
            range.endInclusive.toBigDecimal()
        ).toDouble()
    }

    override fun consumeDoubleOrNull(range: DoubleRange): Double? {
        require(!range.isEmpty()) { "range is empty" }

        if (iterator.readBoolean()) return null
        return consumeDouble(range)
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

        if (consumeBoolean()) return null
        return consumeDoubles(maxLength, range)
    }

    override fun consumeChar(range: CharRange): Char {
        require(!range.isEmpty()) { "range is empty" }

        return (consumeShort((range.first.code + Short.MIN_VALUE)..(range.last.code + Short.MIN_VALUE)) - Short.MIN_VALUE).toChar()
    }

    override fun consumeCharOrNull(range: CharRange): Char? {
        require(!range.isEmpty()) { "range is empty" }

        if (iterator.readBoolean()) return null
        return consumeChar(range)
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

        if (consumeBoolean()) return null
        return consumeChars(maxLength, range)
    }

    override fun consumeString(maxLength: Int, charset: Charset): String {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val length = consumeInt(0..maxLength)
        val byteBuffer = mutableListOf<Byte>()

        while (true) {
            byteBuffer.add(iterator.readByte())
            if (String(byteBuffer.toByteArray(), charset).length >= length) break
        }

        return String(byteBuffer.toByteArray(), charset).take(length)
    }

    override fun consumeStringOrNull(maxLength: Int, charset: Charset): String? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        if (consumeBoolean()) return null
        return consumeString(maxLength, charset)
    }

    override fun consumeRemainingAsString(charset: Charset): String {
        val bytes = mutableListOf<Byte>()
        while (!iterator.isInputFinished()) {
            bytes.add(iterator.readByte())
        }
        return String(bytes.toByteArray(), charset)
    }

    override fun consumeAsciiString(maxLength: Int) = consumeString(maxLength, Charsets.US_ASCII)

    override fun consumeAsciiStringOrNull(maxLength: Int) = consumeStringOrNull(maxLength, Charsets.US_ASCII)

    override fun consumeRemainingAsAsciiString() = consumeRemainingAsString(Charsets.US_ASCII)

    override fun consumeLetter(): Char {
        val index = consumeByte(0 until 52)
        return if (index < 26) {
            'a' + index.toInt()
        } else {
            'A' + index.toInt() - 26
        }
    }

    override fun consumeLetterOrNull() = if (consumeBoolean()) null else consumeLetter()

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

        if (consumeBoolean()) return null

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

        if (consumeBoolean()) return null
        return consumeLetterString(maxLength)
    }

    override fun consumeRemainingAsLetterString(): String {
        return buildString {
            while (!iterator.isInputFinished()) {
                append(consumeLetter())
            }
        }
    }

}