package kotlinx.fuzz

import java.nio.charset.Charset
import kotlin.text.Charsets

typealias FloatRange = ClosedFloatingPointRange<Float>
typealias DoubleRange = ClosedFloatingPointRange<Double>

interface KFuzzer {
    // Add DSL-like approach
    // Add random method call
    // Add consuming of an object (related to random method call)
    // Add different distributions

    /**
     * Consumes a not null boolean from the fuzzer input.
     *
     * @return boolean
     */
    fun boolean(): Boolean

    /**
     * Consumes a nullable boolean from the fuzzer input.
     *
     * @return nullable boolean
     */
    fun booleanOrNull(): Boolean?

    /**
     * Consumes a not null boolean array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @return boolean array
     */
    fun booleans(maxLength: Int): BooleanArray

    /**
     * Consumes a nullable boolean array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @return nullable boolean array
     */
    fun booleansOrNull(maxLength: Int): BooleanArray?

    /**
     * Consumes a not null byte from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Byte.MIN_VALUE, Byte.MAX_VALUE].
     * [Byte.MIN_VALUE, Byte.MAX_VALUE] by default.
     * @return byte that has value in the given range
     */
    fun byte(range: IntRange = Byte.MIN_VALUE..Byte.MAX_VALUE): Byte

    /**
     * Consumes a nullable byte from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Byte.MIN_VALUE, Byte.MAX_VALUE].
     * [Byte.MIN_VALUE, Byte.MAX_VALUE] by default.
     * @return nullable byte that has value in the given range
     */
    fun byteOrNull(range: IntRange = Byte.MIN_VALUE..Byte.MAX_VALUE): Byte?

    /**
     * Consumes a not null byte array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Byte.MIN_VALUE, Byte.MAX_VALUE].
     * [Byte.MIN_VALUE, Byte.MAX_VALUE] by default.
     * @return byte array that has each value in given range
     */
    fun bytes(maxLength: Int, range: IntRange = Byte.MIN_VALUE..Byte.MAX_VALUE): ByteArray

    /**
     * Consumes a nullable byte array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Byte.MIN_VALUE, Byte.MAX_VALUE].
     * [Byte.MIN_VALUE, Byte.MAX_VALUE] by default.
     * @return nullable byte array that has each value in given range
     */
    fun bytesOrNull(maxLength: Int, range: IntRange = Byte.MIN_VALUE..Byte.MAX_VALUE): ByteArray?

    /**
     * Consumes remaining fuzzing input as not null byte array. After calling this method, further calls to methods of
     * this interface will return fixed values only.
     *
     * @return byte array
     */
    fun remainingAsByteArray(): ByteArray

    /**
     * Consumes a not null short from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Short.MIN_VALUE, Short.MAX_VALUE].
     * [Short.MIN_VALUE, Short.MAX_VALUE] by default.
     * @return short that has value in the given range
     */
    fun short(range: IntRange = Short.MIN_VALUE..Short.MAX_VALUE): Short

    /**
     * Consumes a nullable short from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Short.MIN_VALUE, Short.MAX_VALUE].
     * [Short.MIN_VALUE, Short.MAX_VALUE] by default.
     * @return nullable short that has value in the given range
     */
    fun shortOrNull(range: IntRange = Short.MIN_VALUE..Short.MAX_VALUE): Short?

    /**
     * Consumes a not null short array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Short.MIN_VALUE, Short.MAX_VALUE].
     * [Short.MIN_VALUE, Short.MAX_VALUE] by default.
     * @return short array that has each value in given range
     */
    fun shorts(maxLength: Int, range: IntRange = Short.MIN_VALUE..Short.MAX_VALUE): ShortArray

    /**
     * Consumes a nullable short array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Short.MIN_VALUE, Short.MAX_VALUE].
     * [Short.MIN_VALUE, Short.MAX_VALUE] by default.
     * @return nullable short array that has each value in given range
     */
    fun shortsOrNull(maxLength: Int, range: IntRange = Short.MIN_VALUE..Short.MAX_VALUE): ShortArray?

    /**
     * Consumes a not null int from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Int.MIN_VALUE, Int.MAX_VALUE].
     * [Int.MIN_VALUE, Int.MAX_VALUE] by default.
     * @return int that has value in the given range
     */
    fun int(range: IntRange = Int.MIN_VALUE..Int.MAX_VALUE): Int

    /**
     * Consumes a nullable int from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Int.MIN_VALUE, Int.MAX_VALUE].
     * [Int.MIN_VALUE, Int.MAX_VALUE] by default.
     * @return nullable int that has value in the given range
     */
    fun intOrNull(range: IntRange = Int.MIN_VALUE..Int.MAX_VALUE): Int?

    /**
     * Consumes a not null int array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Int.MIN_VALUE, Int.MAX_VALUE].
     * [Int.MIN_VALUE, Int.MAX_VALUE] by default.
     * @return int array that has each value in given range
     */
    fun ints(maxLength: Int, range: IntRange = Int.MIN_VALUE..Int.MAX_VALUE): IntArray

    /**
     * Consumes a nullable int array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Int.MIN_VALUE, Int.MAX_VALUE].
     * [Int.MIN_VALUE, Int.MAX_VALUE] by default.
     * @return nullable int array that has each value in given range
     */
    fun intsOrNull(maxLength: Int, range: IntRange = Int.MIN_VALUE..Int.MAX_VALUE): IntArray?

    /**
     * Consumes a not null long from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Long.MIN_VALUE, Long.MAX_VALUE].
     * [Long.MIN_VALUE, Long.MAX_VALUE] by default.
     * @return long that has value in the given range
     */
    fun long(range: LongRange = Long.MIN_VALUE..Long.MAX_VALUE): Long

    /**
     * Consumes a nullable long from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Long.MIN_VALUE, Long.MAX_VALUE].
     * [Long.MIN_VALUE, Long.MAX_VALUE] by default.
     * @return nullable long that has value in the given range
     */
    fun longOrNull(range: LongRange = Long.MIN_VALUE..Long.MAX_VALUE): Long?

    /**
     * Consumes a not null long array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Long.MIN_VALUE, Long.MAX_VALUE].
     * [Long.MIN_VALUE, Long.MAX_VALUE] by default.
     * @return long array that has each value in given range
     */
    fun longs(maxLength: Int, range: LongRange = Long.MIN_VALUE..Long.MAX_VALUE): LongArray

    /**
     * Consumes a nullable long array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Long.MIN_VALUE, Long.MAX_VALUE].
     * [Long.MIN_VALUE, Long.MAX_VALUE] by default.
     * @return nullable long array that has each value in given range
     */
    fun longsOrNull(maxLength: Int, range: LongRange = Long.MIN_VALUE..Long.MAX_VALUE): LongArray?

    /**
     * Consumes a not null float from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Float.MIN_VALUE, Float.MAX_VALUE].
     * [Float.MIN_VALUE, Float.MAX_VALUE] by default.
     * @return float that has value in the given range
     */
    fun float(range: FloatRange = Float.MIN_VALUE..Float.MAX_VALUE): Float

    /**
     * Consumes a nullable float from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Float.MIN_VALUE, Float.MAX_VALUE].
     * [Float.MIN_VALUE, Float.MAX_VALUE] by default.
     * @return nullable float that has value in the given range
     */
    fun floatOrNull(range: FloatRange = Float.MIN_VALUE..Float.MAX_VALUE): Float?

    /**
     * Consumes a not null float array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Float.MIN_VALUE, Float.MAX_VALUE].
     * [Float.MIN_VALUE, Float.MAX_VALUE] by default.
     * @return float array that has each value in given range
     */
    fun floats(maxLength: Int, range: FloatRange = Float.MIN_VALUE..Float.MAX_VALUE): FloatArray

    /**
     * Consumes a nullable float array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [-Float.MAX_VALUE, Float.MAX_VALUE].
     * [-Float.MAX_VALUE, Float.MAX_VALUE] by default.
     * @return nullable float array that has each value in given range
     */
    fun floatsOrNull(maxLength: Int, range: FloatRange = -Float.MAX_VALUE..Float.MAX_VALUE): FloatArray?

    /**
     * Consumes a not null double from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [-Double.MAX_VALUE, Double.MAX_VALUE].
     * [-Double.MAX_VALUE, Double.MAX_VALUE] by default.
     * @return double that has value in the given range
     */
    fun double(range: DoubleRange = -Double.MAX_VALUE..Double.MAX_VALUE): Double

    /**
     * Consumes a nullable double from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Double.MIN_VALUE, Double.MAX_VALUE].
     * [Double.MIN_VALUE, Double.MAX_VALUE] by default.
     * @return nullable double that has value in the given range
     */
    fun doubleOrNull(range: DoubleRange = Double.MIN_VALUE..Double.MAX_VALUE): Double?

    /**
     * Consumes a not null double array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Double.MIN_VALUE, Double.MAX_VALUE].
     * [Double.MIN_VALUE, Double.MAX_VALUE] by default.
     * @return double array that has each value in given range
     */
    fun doubles(maxLength: Int, range: DoubleRange = Double.MIN_VALUE..Double.MAX_VALUE): DoubleArray

    /**
     * Consumes a nullable double array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Double.MIN_VALUE, Double.MAX_VALUE].
     * [Double.MIN_VALUE, Double.MAX_VALUE] by default.
     * @return nullable double array that has each value in given range
     */
    fun doublesOrNull(maxLength: Int, range: DoubleRange = Double.MIN_VALUE..Double.MAX_VALUE): DoubleArray?

    /**
     * Consumes a not null char from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Char.MIN_VALUE, Char.MAX_VALUE].
     * [Char.MIN_VALUE, Char.MAX_VALUE] by default.
     * @return char that has value in the given range
     */
    fun char(range: CharRange = Char.MIN_VALUE..Char.MAX_VALUE): Char

    /**
     * Consumes a nullable char from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Char.MIN_VALUE, Char.MAX_VALUE].
     * [Char.MIN_VALUE, Char.MAX_VALUE] by default.
     * @return nullable char that has value in the given range
     */
    fun charOrNull(range: CharRange = Char.MIN_VALUE..Char.MAX_VALUE): Char?

    /**
     * Consumes a not null char from the fuzzer input.
     *
     * @param charset denotes desired set of valid characters
     * @return char that has value in the given range
     */
    fun char(charset: Charset): Char

    /**
     * Consumes a nullable char from the fuzzer input.
     *
     * @param charset denotes desired set of valid characters
     * @return nullable char that has value in the given range
     */
    fun charOrNull(charset: Charset): Char?

    /**
     * Consumes a not null char from the fuzzer input.
     *
     * @param charset denotes desired set of valid characters
     * @return char that has value in the given range
     */
    fun char(charset: CharacterSet): Char

    /**
     * Consumes a nullable char from the fuzzer input.
     *
     * @param charset denotes desired set of valid characters
     * @return nullable char that has value in the given range
     */
    fun charOrNull(charset: CharacterSet): Char?

    /**
     * Consumes a not null char array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired set of valid characters
     * @return char array that has each value in given range
     */
    fun chars(maxLength: Int, range: CharRange = Char.MIN_VALUE..Char.MAX_VALUE): CharArray

    /**
     * Consumes a nullable char array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired set of valid characters
     * @return nullable char array that has each value in given range
     */
    fun charsOrNull(maxLength: Int, range: CharRange = Char.MIN_VALUE..Char.MAX_VALUE): CharArray?

    /**
     * Consumes a not null char array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @param charset denotes desired set of valid characters
     * @return char array that has each value in given range
     */
    fun chars(maxLength: Int, charset: Charset): CharArray

    /**
     * Consumes a nullable char array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @param charset denotes desired set of valid characters
     * @return nullable char array that has each value in given range
     */
    fun charsOrNull(maxLength: Int, charset: Charset): CharArray?

    /**
     * Consumes a not null char array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @param charset denotes desired set of valid characters
     * @return char array that has each value in given range
     */
    fun chars(maxLength: Int, charset: CharacterSet): CharArray

    /**
     * Consumes a nullable char array from the fuzzer input. It will have size of maxLength unless fuzzer input is
     * insufficiently long. In this case it will be shorter.
     *
     * @param maxLength the maximum length of the array
     * @param charset denotes desired set of valid characters
     * @return nullable char array that has each value in given range
     */
    fun charsOrNull(maxLength: Int, charset: CharacterSet): CharArray?

    /**
     * Consumes a not null string from the fuzzer input. The returned string may be of any length between 0 and maxLength,
     * even if there is more fuzzer input available.
     *
     * @param maxLength the maximum length of the string
     * @param charset charset that is used for resulting string. UTF-8 by default
     * @return string of length between 0 and maxLength (inclusive) in given charset
     */
    fun string(maxLength: Int, charset: Charset = Charsets.UTF_8): String

    /**
     * Consumes a nullable string from the fuzzer input. The returned string may be of any length between 0 and maxLength,
     * even if there is more fuzzer input available.
     *
     * @param maxLength the maximum length of the string
     * @param charset charset that is used for resulting string. UTF-8 by default
     * @return nullable string of length between 0 and maxLength (inclusive) in given charset
     */
    fun stringOrNull(maxLength: Int, charset: Charset = Charsets.UTF_8): String?

    /**
     * Consumes remaining fuzzing input as not null string. After calling this method, further calls to methods of this
     * interface will return fixed values only.
     *
     * @param charset charset that is used for resulting string. UTF-8 by default
     * @return string in given charset
     */
    fun remainingAsString(charset: Charset = Charsets.UTF_8): String

    /**
     * Consumes a not null string from the fuzzer input. The returned string may be of any length between 0 and maxLength,
     * even if there is more fuzzer input available.
     *
     * @param maxLength the maximum length of the string
     * @param charset denotes desired set of valid characters
     * @return string of length between 0 and maxLength (inclusive) in given charset
     */
    fun string(maxLength: Int, charset: CharacterSet): String

    /**
     * Consumes a nullable string from the fuzzer input. The returned string may be of any length between 0 and maxLength,
     * even if there is more fuzzer input available.
     *
     * @param maxLength the maximum length of the string
     * @param charset denotes desired set of valid characters
     * @return nullable string of length between 0 and maxLength (inclusive) in given charset
     */
    fun stringOrNull(maxLength: Int, charset: CharacterSet): String?

    /**
     * Consumes remaining fuzzing input as not null string. After calling this method, further calls to methods of this
     * interface will return fixed values only.
     *
     * @param charset denotes desired set of valid characters
     * @return string in given charset
     */
    fun remainingAsString(charset: CharacterSet): String

    /**
     * Consumes a not null string from the fuzzer input. The returned string may be of any length between 0 and maxLength, even if there is more fuzzer input available.
     *
     * @param regex regular expression that will be used as a template for string
     * @param configuration configuration of the generation parameters
     * @return string that matches given regex
     */
    fun string(regex: Regex, configuration: RegexConfiguration = RegexConfiguration.DEFAULT): String

    /**
     * Consumes a nullable string from the fuzzer input. The returned string may be of any length between 0 and maxLength, even if there is more fuzzer input available.
     *
     * @param regex regular expression that will be used as a template for string
     * @param configuration configuration of the generation parameters
     * @return nullable string that matches given regex
     * */
    fun stringOrNull(regex: Regex, configuration: RegexConfiguration = RegexConfiguration.DEFAULT): String?

    /**
     * Class that allows to configure parameters of regex string generation
     *
     * @param maxInfinitePatternLength limit of repetitions for infinite patterns, such as a+, a* and a{n,} (default value `100`)
     * @param caseInsensitive flag to use case-insensitive matching (default value `false`)
     * @param allowedCharacters characters that are allowed to appear in the resulting string (default value `null` -> all characters are allowed)
     * @param allowedWhitespaces characters that are allowed to appear in \s pattern (default value `listOf(SPACE, TAB)`)
     */
    data class RegexConfiguration(
        val maxInfinitePatternLength: Int = 100,
        val caseInsensitive: Boolean = false,
        val allowedCharacters: CharacterSet? = null,
        val allowedWhitespaces: CharacterSet = CharacterSet.WHITESPACES,
    ) {
        companion object {
            val DEFAULT = RegexConfiguration()
        }
    }
}

/**
 * Consumes a not null letter ([a-zA-Z]) from the fuzzer input.
 *
 * @return char that has value in the given range
 */
fun KFuzzer.letter(): Char = char(CharacterSet.US_LETTERS)

/**
 * Consumes a nullable letter ([a-zA-Z]) from the fuzzer input.
 *
 * @return nullable char that has value in the given range
 */
fun KFuzzer.letterOrNull(): Char? = when {
    boolean() -> letter()
    else -> null
}

/**
 * Consumes a not null letter ([a-zA-Z]) array from the fuzzer input. It will have size of maxLength unless fuzzer
 * input is insufficiently long. In this case it will be shorter.
 *
 * @param maxLength the maximum length of the array
 * @return char array that has each value in given range
 */
fun KFuzzer.letters(maxLength: Int): CharArray {
    require(maxLength > 0) { "maxLength must be greater than 0" }

    val list = mutableListOf<Char>()
    while (list.size < maxLength) {
        list.add(letter())
    }
    return list.toCharArray()
}

/**
 * Consumes a nullable letter ([a-zA-Z]) array from the fuzzer input. It will have size of maxLength unless fuzzer
 * input is insufficiently long. In this case it will be shorter.
 *
 * @param maxLength the maximum length of the array
 * @return nullable char array that has each value in given range
 */
fun KFuzzer.lettersOrNull(maxLength: Int): CharArray? = when {
    boolean() -> letters(maxLength)
    else -> null
}

/**
 * Consumes a not null ascii string from the fuzzer input. The returned string may be of any length between 0 and
 * maxLength, even if there is more fuzzer input available.
 *
 * @param maxLength the maximum length of the string
 * @return ascii string of length between 0 and maxLength (inclusive)
 */
fun KFuzzer.asciiString(maxLength: Int): String = string(maxLength, charset = Charsets.US_ASCII)

/**
 * Consumes a nullable ascii string from the fuzzer input. The returned string may be of any length between 0 and
 * maxLength, even if there is more fuzzer input available.
 *
 * @param maxLength the maximum length of the string
 * @return nullable ascii string of length between 0 and maxLength (inclusive)
 */
fun KFuzzer.asciiStringOrNull(maxLength: Int): String? = when {
    boolean() -> asciiString(maxLength)
    else -> null
}

/**
 * Consumes remaining fuzzing input as not null ascii string. After calling this method, further calls to methods of
 * this interface will return fixed values only.
 *
 * @return ascii string
 */
fun KFuzzer.remainingAsAsciiString(): String = remainingAsString(charset = Charsets.US_ASCII)

/**
 * Consumes a not null string consisting of letters of latin alphabet from the fuzzer input. The returned string may
 * be of any length between 0 and maxLength, even if there is more fuzzer input available.
 *
 * @param maxLength the maximum length of the string
 * @return string consisting of letters of latin alphabet of length between 0 and maxLength (inclusive)
 */
fun KFuzzer.letterString(maxLength: Int): String = string(maxLength, charset = CharacterSet.US_LETTERS)

/**
 * Consumes a nullable string consisting of letters of latin alphabet from the fuzzer input. The returned string may
 * be of any length between 0 and maxLength, even if there is more fuzzer input available.
 *
 * @param maxLength the maximum length of the string
 * @return nullable string consisting of letters of latin alphabet of length between 0 and maxLength (inclusive)
 */
fun KFuzzer.letterStringOrNull(maxLength: Int): String? = when {
    boolean() -> letterString(maxLength)
    else -> null
}

/**
 * Consumes remaining fuzzing input as not null string consisting of letters of latin alphabet. After calling this
 * method, further calls to methods of this interface will return fixed values only.
 *
 * @return string consisting of letters of latin alphabet
 */
fun KFuzzer.remainingAsLetterString(): String = remainingAsString(charset = CharacterSet.US_LETTERS)

/**
 * Picks an element from collection based on the fuzzer input.
 *
 * @param collection collection to pick an element from
 * @return an element from collection chosen based on the fuzzer input
 */
fun <T> KFuzzer.pick(collection: Collection<T>): T {
    require(collection.isNotEmpty()) { "collection is empty" }
    return collection.elementAt(int(collection.indices))
}

/**
 * Picks an element from array based on the fuzzer input.
 *
 * @param array array to pick an element from
 * @return an element from array chosen based on the fuzzer input
 */
fun <T> KFuzzer.pick(array: Array<T>): T {
    require(array.isNotEmpty()) { "array is empty" }
    return array[int(array.indices)]
}

/**
 * Picks an element from boolean array based on the fuzzer input.
 *
 * @param array boolean array to pick an element from
 * @return an element from boolean array chosen based on the fuzzer input
 */
fun KFuzzer.pick(array: BooleanArray): Boolean {
    require(array.isNotEmpty()) { "array is empty" }
    return array[int(array.indices)]
}

/**
 * Picks an element from byte array based on the fuzzer input.
 *
 * @param array byte array to pick an element from
 * @return an element from byte array chosen based on the fuzzer input
 */
fun KFuzzer.pick(array: ByteArray): Byte {
    require(array.isNotEmpty()) { "array is empty" }
    return array[int(array.indices)]
}

/**
 * Picks an element from short array based on the fuzzer input.
 *
 * @param array short array to pick an element from
 * @return an element from short array chosen based on the fuzzer input
 */
fun KFuzzer.pick(array: ShortArray): Short {
    require(array.isNotEmpty()) { "array is empty" }
    return array[int(array.indices)]
}

/**
 * Picks an element from int array based on the fuzzer input.
 *
 * @param array int array to pick an element from
 * @return an element from int array chosen based on the fuzzer input
 */
fun KFuzzer.pick(array: IntArray): Int {
    require(array.isNotEmpty()) { "array is empty" }
    return array[int(array.indices)]
}

/**
 * Picks an element from long array based on the fuzzer input.
 *
 * @param array long array to pick an element from
 * @return an element from long array chosen based on the fuzzer input
 */
fun KFuzzer.pick(array: LongArray): Long {
    require(array.isNotEmpty()) { "array is empty" }
    return array[int(array.indices)]
}

/**
 * Picks an element from double array based on the fuzzer input.
 *
 * @param array double array to pick an element from
 * @return an element from double array chosen based on the fuzzer input
 */
fun KFuzzer.pick(array: DoubleArray): Double {
    require(array.isNotEmpty()) { "array is empty" }
    return array[int(array.indices)]
}

/**
 * Picks an element from float array based on the fuzzer input.
 *
 * @param array float array to pick an element from
 * @return an element from float array chosen based on the fuzzer input
 */
fun KFuzzer.pick(array: FloatArray): Float {
    require(array.isNotEmpty()) { "array is empty" }
    return array[int(array.indices)]
}

/**
 * Picks an element from char array based on the fuzzer input.
 *
 * @param array char array to pick an element from
 * @return an element from char array chosen based on the fuzzer input
 */
fun KFuzzer.pick(array: CharArray): Char {
    require(array.isNotEmpty()) { "array is empty" }
    return array[int(array.indices)]
}
