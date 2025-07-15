package kotlinx.fuzz

import com.github.curiousoddman.rgxgen.RgxGen
import com.github.curiousoddman.rgxgen.config.RgxGenOption
import com.github.curiousoddman.rgxgen.config.RgxGenProperties
import com.github.curiousoddman.rgxgen.model.RgxGenCharsDefinition
import com.github.curiousoddman.rgxgen.model.SymbolRange
import com.github.curiousoddman.rgxgen.model.WhitespaceChar
import com.github.curiousoddman.rgxgen.util.chars.CharList
import java.math.BigDecimal
import java.math.MathContext
import java.nio.charset.Charset
import java.util.Random
import kotlinx.fuzz.KFuzzer.RegexConfiguration

class KFuzzerImpl(data: ByteArray) : Random(), KFuzzer {
    private val iterator = Reader(data)

    private operator fun IntRange.contains(other: IntRange): Boolean =
        other.first >= this.first && other.last <= this.last

    /**
     * Fits a number into a range. Always returns the distance from the number to T.MIN_VALUE modulo size of the range
     *
     * @param n a number
     * @param range target range
     * @return a number that fits into the given range
     */
    private inline fun <reified T : Number> fitIntoRange(n: T, range: IntRange): T {
        val rangeSize = range.last.toLong() - range.first + 1
        return when (n) {
            is Byte -> ((n.toLong() - Byte.MIN_VALUE) % rangeSize + range.first).toByte() as T
            is Short -> ((n.toLong() - Short.MIN_VALUE) % rangeSize + range.first).toShort() as T
            is Int -> ((n.toLong() - Int.MIN_VALUE) % rangeSize + range.first).toInt() as T
            else -> error("Not integer type")
        }
    }

    /**
     * Fits a number into a range. Always returns the distance from the number to Long.MIN_VALUE modulo size of the range
     *
     * @param n a number
     * @param range target range
     * @return a number that fits into the given range
     */
    private fun fitIntoRange(n: Long, range: LongRange): Long = when {
        range.first < 0 && range.last > 0 && range.last - Long.MAX_VALUE > range.first -> when {
            n < range.first -> n - Long.MIN_VALUE + range.first

            n <= range.last -> {
                val normalized = n + (range.first - Long.MIN_VALUE)
                when {
                    normalized > range.last -> range.first + (normalized - range.last)
                    else -> normalized
                }
            }

            else -> range.first + (range.first - Long.MIN_VALUE) + (n - range.last) - 1L
        }

        else -> {
            val rangeSize = range.last - range.first + 1
            var normalized = n % rangeSize - Long.MIN_VALUE % rangeSize + range.first
            while (n < range.first) {
                normalized += rangeSize
            }
            normalized
        }
    }

    /**
     * Fits a float number into a range. Scales full range to a given one. Note that `fitIntoDoubleRange` has the same logic
     *
     * @param value a number
     * @param range target range
     * @return a number that fits into the given range
     */
    private fun fitIntoFloatRange(
        value: Float,
        range: FloatRange,
        context: MathContext = MathContext.DECIMAL128,
    ): Float = when {
        value.isFinite() -> {
            val normalized = value.toBigDecimal().add(Float.MAX_VALUE.toBigDecimal(), context)
                .divide(Float.MAX_VALUE.toBigDecimal().multiply(BigDecimal(2), context), context)

            range.start.toBigDecimal()
                .add(
                    normalized.multiply(
                        range.endInclusive.toBigDecimal().subtract(range.start.toBigDecimal(), context), context,
                    ),
                    context,
                )
                .toFloat()
        }
        // value is not finite, but it is in range
        value in range -> value
        // value is not finite, but it is not included in the range
        // no proper way to handle this, lets just return any valid entry
        else -> range.start
    }

    /**
     * Fits a double number into a range. Scales full range to a given one. Note that `fitIntoFloatRange` has the same logic
     *
     * @param value a number
     * @param range target range
     * @return a number that fits into the given range
     */
    private fun fitIntoDoubleRange(
        value: Double,
        range: DoubleRange,
        context: MathContext = MathContext.DECIMAL128,
    ): Double = when {
        value.isFinite() -> {
            val normalized = value.toBigDecimal().add(Double.MAX_VALUE.toBigDecimal(), context)
                .divide(Double.MAX_VALUE.toBigDecimal().multiply(BigDecimal(2), context), context)

            range.start.toBigDecimal()
                .add(
                    normalized.multiply(
                        range.endInclusive.toBigDecimal().subtract(range.start.toBigDecimal(), context), context,
                    ),
                    context,
                )
                .toDouble()
        }
        // value is not finite, but it is in range
        value in range -> value
        // value is not finite, but it is not included in the range
        // no proper way to handle this, lets just return any valid entry
        else -> range.start
    }

    /**
     * Implementation taken from #Random.next
     */
    override fun next(bits: Int): Int = (int() ushr (48 - bits))

    override fun boolean(): Boolean = iterator.readBoolean()

    override fun booleanOrNull(): Boolean? = when {
        iterator.readBoolean() -> iterator.readBoolean()
        else -> null
    }

    override fun booleans(maxLength: Int): BooleanArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Boolean>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(boolean())
        }
        return list.toBooleanArray()
    }

    override fun booleansOrNull(maxLength: Int): BooleanArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            boolean() -> booleans(maxLength)
            else -> null
        }
    }

    override fun byte(range: IntRange): Byte {
        require(range.isNotEmpty()) { "range is empty" }
        require(range in Byte.MIN_VALUE..Byte.MAX_VALUE) { "range should be a subset of [Byte.MIN_VALUE..Byte.MAX_VALUE] but was $range" }

        return fitIntoRange(iterator.readByte(), range)
    }

    override fun byteOrNull(range: IntRange): Byte? {
        require(range.isNotEmpty()) { "range is empty" }
        require(range in Byte.MIN_VALUE..Byte.MAX_VALUE) { "range should be a subset of [Byte.MIN_VALUE..Byte.MAX_VALUE] but was $range" }

        return when {
            boolean() -> byte(range)
            else -> null
        }
    }

    override fun bytes(maxLength: Int, range: IntRange): ByteArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Byte>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(byte(range))
        }
        return list.toByteArray()
    }

    override fun bytesOrNull(maxLength: Int, range: IntRange): ByteArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            boolean() -> bytes(maxLength, range)
            else -> null
        }
    }

    override fun remainingAsByteArray(): ByteArray {
        val list = mutableListOf<Byte>()
        while (!iterator.isInputFinished()) {
            list.add(byte())
        }
        return list.toByteArray()
    }

    override fun short(range: IntRange): Short {
        require(range.isNotEmpty()) { "range is empty" }
        require(range in Short.MIN_VALUE..Short.MAX_VALUE) { "range should be a subset of [Short.MIN_VALUE..Short.MAX_VALUE] but was $range" }

        return fitIntoRange(iterator.readShort(), range)
    }

    override fun shortOrNull(range: IntRange): Short? {
        require(range.isNotEmpty()) { "range is empty" }
        require(range in Short.MIN_VALUE..Short.MAX_VALUE) { "range should be a subset of [Short.MIN_VALUE..Short.MAX_VALUE] but was $range" }

        return when {
            boolean() -> short(range)
            else -> null
        }
    }

    override fun shorts(maxLength: Int, range: IntRange): ShortArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Short>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(short(range))
        }
        return list.toShortArray()
    }

    override fun shortsOrNull(maxLength: Int, range: IntRange): ShortArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            boolean() -> shorts(maxLength, range)
            else -> null
        }
    }

    override fun int(range: IntRange): Int {
        require(range.isNotEmpty()) { "range is empty" }

        return fitIntoRange(iterator.readInt(), range)
    }

    override fun intOrNull(range: IntRange): Int? {
        require(range.isNotEmpty()) { "range is empty" }

        return when {
            boolean() -> int(range)
            else -> null
        }
    }

    override fun ints(maxLength: Int, range: IntRange): IntArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Int>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(int(range))
        }
        return list.toIntArray()
    }

    override fun intsOrNull(maxLength: Int, range: IntRange): IntArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            boolean() -> ints(maxLength, range)
            else -> null
        }
    }

    override fun long(range: LongRange): Long {
        require(range.isNotEmpty()) { "range is empty" }
        return fitIntoRange(iterator.readLong(), range)
    }

    override fun longOrNull(range: LongRange): Long? {
        require(range.isNotEmpty()) { "range is empty" }

        return when {
            boolean() -> long(range)
            else -> null
        }
    }

    override fun longs(maxLength: Int, range: LongRange): LongArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Long>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(long(range))
        }
        return list.toLongArray()
    }

    override fun longsOrNull(maxLength: Int, range: LongRange): LongArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            boolean() -> longs(maxLength, range)
            else -> null
        }
    }

    override fun float(range: FloatRange): Float {
        require(range.isNotEmpty()) { "range is empty" }
        return fitIntoFloatRange(iterator.readFloat(), range)
    }

    override fun floatOrNull(range: FloatRange): Float? {
        require(range.isNotEmpty()) { "range is empty" }

        return when {
            boolean() -> float(range)
            else -> null
        }
    }

    override fun floats(maxLength: Int, range: FloatRange): FloatArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Float>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(float(range))
        }
        return list.toFloatArray()
    }

    override fun floatsOrNull(maxLength: Int, range: FloatRange): FloatArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            boolean() -> floats(maxLength, range)
            else -> null
        }
    }

    override fun double(range: DoubleRange): Double {
        require(range.isNotEmpty()) { "range is empty" }
        return fitIntoDoubleRange(iterator.readDouble(), range)
    }

    override fun doubleOrNull(range: DoubleRange): Double? {
        require(range.isNotEmpty()) { "range is empty" }

        return when {
            boolean() -> double(range)
            else -> null
        }
    }

    override fun doubles(maxLength: Int, range: DoubleRange): DoubleArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Double>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(double(range))
        }
        return list.toDoubleArray()
    }

    override fun doublesOrNull(maxLength: Int, range: DoubleRange): DoubleArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            boolean() -> doubles(maxLength, range)
            else -> null
        }
    }

    override fun char(range: CharRange): Char {
        require(range.isNotEmpty()) { "range is empty" }

        return (short((range.first.code + Short.MIN_VALUE)..(range.last.code + Short.MIN_VALUE)) - Short.MIN_VALUE).toChar()
    }

    override fun charOrNull(range: CharRange): Char? {
        require(range.isNotEmpty()) { "range is empty" }

        return when {
            boolean() -> char(range)
            else -> null
        }
    }

    override fun char(charset: Charset): Char {
        val bytes = mutableListOf<Byte>()
        var str = ""
        while (str.isEmpty()) {
            bytes.add(byte())
            str = String(bytes.toByteArray(), charset)
        }
        return str.first()
    }

    override fun charOrNull(charset: Charset): Char? = when {
        boolean() -> char(charset)
        else -> null
    }

    override fun char(charset: CharacterSet): Char {
        val index = int(0 until charset.size)
        var current = 0
        for (range in charset.ranges) {
            val rangeSize = range.last - range.first + 1
            when {
                current + rangeSize > index -> return range.first + (index - current)
                else -> current += rangeSize
            }
        }
        return charset.symbols.take(index - current + 1).last()
    }

    override fun charOrNull(charset: CharacterSet): Char? = when {
        boolean() -> char(charset)
        else -> null
    }

    override fun chars(maxLength: Int, range: CharRange): CharArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Char>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(char(range))
        }
        return list.toCharArray()
    }

    override fun charsOrNull(maxLength: Int, range: CharRange): CharArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            boolean() -> chars(maxLength, range)
            else -> null
        }
    }

    override fun chars(maxLength: Int, charset: Charset): CharArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Char>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(char(charset))
        }
        return list.toCharArray()
    }

    override fun charsOrNull(maxLength: Int, charset: Charset): CharArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            boolean() -> chars(maxLength, charset)
            else -> null
        }
    }

    override fun chars(maxLength: Int, charset: CharacterSet): CharArray {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val list = mutableListOf<Char>()
        while (list.size < maxLength && !iterator.isInputFinished()) {
            list.add(char(charset))
        }
        return list.toCharArray()
    }

    override fun charsOrNull(maxLength: Int, charset: CharacterSet): CharArray? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            boolean() -> chars(maxLength, charset)
            else -> null
        }
    }

    override fun string(maxLength: Int, charset: Charset): String {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        val byteBuffer = mutableListOf<Byte>()
        while (true) {
            byteBuffer.add(iterator.readByte())
            if (String(byteBuffer.toByteArray(), charset).length >= maxLength) {
                break
            }
        }

        return String(byteBuffer.toByteArray(), charset).take(maxLength)
    }

    override fun stringOrNull(maxLength: Int, charset: Charset): String? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            boolean() -> string(maxLength, charset)
            else -> null
        }
    }

    override fun remainingAsString(charset: Charset): String {
        val bytes = mutableListOf<Byte>()
        while (!iterator.isInputFinished()) {
            bytes.add(iterator.readByte())
        }
        return String(bytes.toByteArray(), charset)
    }

    override fun string(maxLength: Int, charset: CharacterSet): String {
        require(maxLength > 0) { "maxLength must be greater than 0" }
        return String(chars(maxLength, charset))
    }

    override fun stringOrNull(maxLength: Int, charset: CharacterSet): String? {
        require(maxLength > 0) { "maxLength must be greater than 0" }

        return when {
            boolean() -> string(maxLength, charset)
            else -> null
        }
    }

    override fun remainingAsString(charset: CharacterSet): String = buildString {
        while (!iterator.isInputFinished()) {
            append(char(charset))
        }
    }

    override fun string(regex: Regex, configuration: RegexConfiguration): String {
        val rgxGen = RgxGen.parse(configuration.asRegexProperties(), regex.pattern)
        return rgxGen.generate(this)
    }

    override fun stringOrNull(regex: Regex, configuration: RegexConfiguration) = when {
        boolean() -> string(regex, configuration)
        else -> null
    }

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

internal fun CharacterSet.toRgxGenProperties(): RgxGenCharsDefinition = RgxGenCharsDefinition.of(
    ranges.map { SymbolRange.range(it.first.code, it.last.code) },
    CharList.charList(symbols.joinToString("")),
)

internal fun RegexConfiguration.asRegexProperties(): RgxGenProperties {
    val properties = RgxGenProperties()
    RgxGenOption.INFINITE_PATTERN_REPETITION.setInProperties(
        properties,
        this.maxInfinitePatternLength,
    )

    RgxGenOption.CASE_INSENSITIVE.setInProperties(properties, caseInsensitive)
    allowedCharacters?.let {
        RgxGenOption.DOT_MATCHES_ONLY.setInProperties(
            properties,
            it.toRgxGenProperties(),
        )
    }

    val rgxGenWhiteSpaces = WhitespaceChar.entries.associateBy { it.get() }
    RgxGenOption.WHITESPACE_DEFINITION.setInProperties(
        properties,
        allowedWhitespaces.map {
            rgxGenWhiteSpaces[it]
                ?: error("$it is not a valid whitespace character, valid characters are: ${WhitespaceChar.entries.map { it.get() }}")
        },
    )
    return properties
}

@Suppress("NOTHING_TO_INLINE")
private inline fun <T : Comparable<T>> ClosedRange<T>.isNotEmpty(): Boolean = this.isEmpty() == false
