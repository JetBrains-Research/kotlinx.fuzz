package kotlinx.fuzz

import java.nio.charset.Charset
import kotlin.text.Charsets

typealias FloatRange = ClosedFloatingPointRange<Float>
typealias DoubleRange = ClosedFloatingPointRange<Double>

interface KFuzzer {
    // Add regex-based string generation
    // Add DSL-like approach
    // Add random method call
    // Add consuming of an object (related to random method call)
    // Add different distributions

    /**
     * Consumes a not null boolean from the fuzzer input.
     *
     * @return boolean
     */
    fun consumeBoolean(): Boolean

    /**
     * Consumes a nullable boolean from the fuzzer input.
     *
     * @return nullable boolean
     */
    fun consumeBooleanOrNull(): Boolean?

    /**
     * Consumes a not null boolean array from the fuzzer input. It will have size of maxLength unless fuzzer input is insufficiently long. In this case it will be shorter. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     *
     * @return boolean array
     */
    fun consumeBooleans(maxLength: Int): BooleanArray

    /**
     * Consumes a nullable boolean array from the fuzzer input. It will have size of maxLength unless fuzzer input is insufficiently long. In this case it will be shorter. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     *
     * @return nullable boolean array
     */
    fun consumeBooleansOrNull(maxLength: Int): BooleanArray?

    /**
     * Consumes a not null byte from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Byte.MIN_VALUE, Byte.MAX_VALUE]. [Byte.MIN_VALUE, Byte.MAX_VALUE] by default
     *
     * @return byte that has value in the given range
     */
    fun consumeByte(range: IntRange = Byte.MIN_VALUE..Byte.MAX_VALUE): Byte

    /**
     * Consumes a nullable byte from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Byte.MIN_VALUE, Byte.MAX_VALUE]. [Byte.MIN_VALUE, Byte.MAX_VALUE] by default
     *
     * @return nullable byte that has value in the given range
     */
    fun consumeByteOrNull(range: IntRange = Byte.MIN_VALUE..Byte.MAX_VALUE): Byte?

    /**
     * Consumes a not null byte array from the fuzzer input. It will have size of maxLength unless fuzzer input is insufficiently long. In this case it will be shorter. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Byte.MIN_VALUE, Byte.MAX_VALUE]. [Byte.MIN_VALUE, Byte.MAX_VALUE] by default
     *
     * @return byte array that has each value in given range
     */
    fun consumeBytes(maxLength: Int, range: IntRange = Byte.MIN_VALUE..Byte.MAX_VALUE): ByteArray

    /**
     * Consumes a nullable byte array from the fuzzer input. It will have size of maxLength unless fuzzer input is insufficiently long. In this case it will be shorter. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Byte.MIN_VALUE, Byte.MAX_VALUE]. [Byte.MIN_VALUE, Byte.MAX_VALUE] by default
     *
     * @return nullable byte array that has each value in given range
     */
    fun consumeBytesOrNull(maxLength: Int, range: IntRange = Byte.MIN_VALUE..Byte.MAX_VALUE): ByteArray?

    /**
     * Consumes remaining fuzzing input as not null byte array. After calling this method, further calls to methods of this interface will return fixed values only.
     *
     * @return byte array
     */
    fun consumeRemainingAsByteArray(): ByteArray

    /**
     * Consumes a not null short from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Short.MIN_VALUE, Short.MAX_VALUE]. [Short.MIN_VALUE, Short.MAX_VALUE] by default
     *
     * @return short that has value in the given range
     */
    fun consumeShort(range: IntRange = Short.MIN_VALUE..Short.MAX_VALUE): Short

    /**
     * Consumes a nullable short from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Short.MIN_VALUE, Short.MAX_VALUE]. [Short.MIN_VALUE, Short.MAX_VALUE] by default
     *
     * @return nullable short that has value in the given range
     */
    fun consumeShortOrNull(range: IntRange = Short.MIN_VALUE..Short.MAX_VALUE): Short?

    /**
     * Consumes a not null short array from the fuzzer input. It will have size of maxLength unless fuzzer input is insufficiently long. In this case it will be shorter. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Short.MIN_VALUE, Short.MAX_VALUE]. [Short.MIN_VALUE, Short.MAX_VALUE] by default
     *
     * @return short array that has each value in given range
     */
    fun consumeShorts(maxLength: Int, range: IntRange = Short.MIN_VALUE..Short.MAX_VALUE): ShortArray

    /**
     * Consumes a nullable short array from the fuzzer input. It will have size of maxLength unless fuzzer input is insufficiently long. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Short.MIN_VALUE, Short.MAX_VALUE]. [Short.MIN_VALUE, Short.MAX_VALUE] by default
     *
     * @return nullable short array that has each value in given range
     */
    fun consumeShortsOrNull(maxLength: Int, range: IntRange = Short.MIN_VALUE..Short.MAX_VALUE): ShortArray?

    /**
     * Consumes a not null int from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Int.MIN_VALUE, Int.MAX_VALUE]. [Int.MIN_VALUE, Int.MAX_VALUE] by default
     *
     * @return int that has value in the given range
     */
    fun consumeInt(range: IntRange = Int.MIN_VALUE..Int.MAX_VALUE): Int

    /**
     * Consumes a nullable int from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Int.MIN_VALUE, Int.MAX_VALUE]. [Int.MIN_VALUE, Int.MAX_VALUE] by default
     *
     * @return nullable int that has value in the given range
     */
    fun consumeIntOrNull(range: IntRange = Int.MIN_VALUE..Int.MAX_VALUE): Int?

    /**
     * Consumes a not null int array from the fuzzer input. It will have size of maxLength unless fuzzer input is insufficiently long. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Int.MIN_VALUE, Int.MAX_VALUE]. [Int.MIN_VALUE, Int.MAX_VALUE] by default
     *
     * @return int array that has each value in given range
     */
    fun consumeInts(maxLength: Int, range: IntRange = Int.MIN_VALUE..Int.MAX_VALUE): IntArray

    /**
     * Consumes a nullable int array from the fuzzer input. It will have size of maxLength unless fuzzer input is insufficiently long. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Int.MIN_VALUE, Int.MAX_VALUE]. [Int.MIN_VALUE, Int.MAX_VALUE] by default
     *
     * @return nullable int array that has each value in given range
     */
    fun consumeIntsOrNull(maxLength: Int, range: IntRange = Int.MIN_VALUE..Int.MAX_VALUE): IntArray?

    /**
     * Consumes a not null long from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Long.MIN_VALUE, Long.MAX_VALUE]. [Long.MIN_VALUE, Long.MAX_VALUE] by default
     *
     * @return long that has value in the given range
     */
    fun consumeLong(range: LongRange = Long.MIN_VALUE..Long.MAX_VALUE): Long

    /**
     * Consumes a nullable long from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Long.MIN_VALUE, Long.MAX_VALUE]. [Long.MIN_VALUE, Long.MAX_VALUE] by default
     *
     * @return nullable long that has value in the given range
     */
    fun consumeLongOrNull(range: LongRange = Long.MIN_VALUE..Long.MAX_VALUE): Long?

    /**
     * Consumes a not null long array from the fuzzer input. It will have size of maxLength unless fuzzer input is insufficiently long. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Long.MIN_VALUE, Long.MAX_VALUE]. [Long.MIN_VALUE, Long.MAX_VALUE] by default
     *
     * @return long array that has each value in given range
     */
    fun consumeLongs(maxLength: Int, range: LongRange = Long.MIN_VALUE..Long.MAX_VALUE): LongArray

    /**
     * Consumes a nullable long array from the fuzzer input. It will have size of maxLength unless fuzzer input is insufficiently long. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Long.MIN_VALUE, Long.MAX_VALUE]. [Long.MIN_VALUE, Long.MAX_VALUE] by default
     *
     * @return nullable long array that has each value in given range
     */
    fun consumeLongsOrNull(maxLength: Int, range: LongRange = Long.MIN_VALUE..Long.MAX_VALUE): LongArray?

    /**
     * Consumes a not null float from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Float.MIN_VALUE, Float.MAX_VALUE]. [Float.MIN_VALUE, Float.MAX_VALUE] by default
     *
     * @return float that has value in the given range
     */
    fun consumeFloat(range: FloatRange = Float.MIN_VALUE..Float.MAX_VALUE): Float

    /**
     * Consumes a nullable float from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Float.MIN_VALUE, Float.MAX_VALUE]. [Float.MIN_VALUE, Float.MAX_VALUE] by default
     *
     * @return nullable float that has value in the given range
     */
    fun consumeFloatOrNull(range: FloatRange = Float.MIN_VALUE..Float.MAX_VALUE): Float?

    /**
     * Consumes a not null float array from the fuzzer input. It will have size of maxLength unless fuzzer input is insufficiently long. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Float.MIN_VALUE, Float.MAX_VALUE]. [Float.MIN_VALUE, Float.MAX_VALUE] by default
     *
     * @return float array that has each value in given range
     */
    fun consumeFloats(maxLength: Int, range: FloatRange = Float.MIN_VALUE..Float.MAX_VALUE): FloatArray

    /**
     * Consumes a nullable float array from the fuzzer input. It will have size of maxLength unless fuzzer input is insufficiently long. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Float.MIN_VALUE, Float.MAX_VALUE]. [Float.MIN_VALUE, Float.MAX_VALUE] by default
     *
     * @return nullable float array that has each value in given range
     */
    fun consumeFloatsOrNull(maxLength: Int, range: FloatRange = Float.MIN_VALUE..Float.MAX_VALUE): FloatArray?

    /**
     * Consumes a not null double from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Double.MIN_VALUE, Double.MAX_VALUE]. [Double.MIN_VALUE, Double.MAX_VALUE] by default
     *
     * @return double that has value in the given range
     */
    fun consumeDouble(range: DoubleRange = Double.MIN_VALUE..Double.MAX_VALUE): Double

    /**
     * Consumes a nullable double from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Double.MIN_VALUE, Double.MAX_VALUE]. [Double.MIN_VALUE, Double.MAX_VALUE] by default
     *
     * @return nullable double that has value in the given range
     */
    fun consumeDoubleOrNull(range: DoubleRange = Double.MIN_VALUE..Double.MAX_VALUE): Double?

    /**
     * Consumes a not null double array from the fuzzer input. It will have size of maxLength unless fuzzer input is insufficiently long. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Double.MIN_VALUE, Double.MAX_VALUE]. [Double.MIN_VALUE, Double.MAX_VALUE] by default
     *
     * @return double array that has each value in given range
     */
    fun consumeDoubles(maxLength: Int, range: DoubleRange = Double.MIN_VALUE..Double.MAX_VALUE): DoubleArray

    /**
     * Consumes a nullable double array from the fuzzer input. It will have size of maxLength unless fuzzer input is insufficiently long. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Double.MIN_VALUE, Double.MAX_VALUE]. [Double.MIN_VALUE, Double.MAX_VALUE] by default
     *
     * @return nullable double array that has each value in given range
     */
    fun consumeDoublesOrNull(maxLength: Int, range: DoubleRange = Double.MIN_VALUE..Double.MAX_VALUE): DoubleArray?

    /**
     * Consumes a not null char from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Char.MIN_VALUE, Char.MAX_VALUE]. [Char.MIN_VALUE, Char.MAX_VALUE] by default
     *
     * @return char that has value in the given range
     */
    fun consumeChar(range: CharRange = Char.MIN_VALUE..Char.MAX_VALUE): Char

    /**
     * Consumes a nullable char from the fuzzer input.
     *
     * @param range denotes desired range for resulting value. Should be a subset of [Char.MIN_VALUE, Char.MAX_VALUE]. [Char.MIN_VALUE, Char.MAX_VALUE] by default
     *
     * @return nullable char that has value in the given range
     */
    fun consumeCharOrNull(range: CharRange = Char.MIN_VALUE..Char.MAX_VALUE): Char?

    /**
     * Consumes a not null char array from the fuzzer input. It will have size of maxLength unless fuzzer input is insufficiently long. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Char.MIN_VALUE, Char.MAX_VALUE]. [Char.MIN_VALUE, Char.MAX_VALUE] by default
     *
     * @return char array that has each value in given range
     */
    fun consumeChars(maxLength: Int, range: CharRange = Char.MIN_VALUE..Char.MAX_VALUE): CharArray

    /**
     * Consumes a nullable char array from the fuzzer input. It will have size of maxLength unless fuzzer input is insufficiently long. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     * @param range denotes desired range for each value. Should be a subset of [Char.MIN_VALUE, Char.MAX_VALUE]. [Char.MIN_VALUE, Char.MAX_VALUE] by default
     *
     * @return nullable char array that has each value in given range
     */
    fun consumeCharsOrNull(maxLength: Int, range: CharRange = Char.MIN_VALUE..Char.MAX_VALUE): CharArray?

    /**
     * Consumes a not null letter ([a-zA-Z]) from the fuzzer input.
     *
     * @return char that has value in the given range
     */
    fun consumeLetter(): Char

    /**
     * Consumes a nullable letter ([a-zA-Z]) from the fuzzer input.
     *
     * @return nullable char that has value in the given range
     */
    fun consumeLetterOrNull(): Char?

    /**
     * Consumes a not null letter ([a-zA-Z]) array from the fuzzer input. It will have size of maxLength unless fuzzer input is insufficiently long. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     *
     * @return char array that has each value in given range
     */
    fun consumeLetters(maxLength: Int): CharArray

    /**
     * Consumes a nullable letter ([a-zA-Z]) array from the fuzzer input. It will have size of maxLength unless fuzzer input is insufficiently long. In this case it will be shorter
     *
     * @param maxLength the maximum length of the array
     *
     * @return nullable char array that has each value in given range
     */
    fun consumeLettersOrNull(maxLength: Int): CharArray?

    /**
     * Consumes a not null string from the fuzzer input. The returned string may be of any length between 0 and maxLength, even if there is more fuzzer input available.
     *
     * @param maxLength the maximum length of the string
     * @param charset charset that is used for resulting string. UTF-8 by default
     *
     * @return string of length between 0 and maxLength (inclusive) in given charset
     */
    fun consumeString(maxLength: Int, charset: Charset = Charsets.UTF_8): String

    /**
     * Consumes a nullable string from the fuzzer input. The returned string may be of any length between 0 and maxLength, even if there is more fuzzer input available.
     *
     * @param maxLength the maximum length of the string
     * @param charset charset that is used for resulting string. UTF-8 by default
     *
     * @return nullable string of length between 0 and maxLength (inclusive) in given charset
     */
    fun consumeStringOrNull(maxLength: Int, charset: Charset = Charsets.UTF_8): String?

    /**
     * Consumes remaining fuzzing input as not null string. After calling this method, further calls to methods of this interface will return fixed values only.
     *
     * @param charset charset that is used for resulting string. UTF-8 by default
     *
     * @return string in given charset
     */
    fun consumeRemainingAsString(charset: Charset = Charsets.UTF_8): String

    /**
     * Consumes a not null ascii string from the fuzzer input. The returned string may be of any length between 0 and maxLength, even if there is more fuzzer input available.
     *
     * @param maxLength the maximum length of the string
     *
     * @return ascii string of length between 0 and maxLength (inclusive)
     */
    fun consumeAsciiString(maxLength: Int): String

    /**
     * Consumes a nullable ascii string from the fuzzer input. The returned string may be of any length between 0 and maxLength, even if there is more fuzzer input available.
     *
     * @param maxLength the maximum length of the string
     *
     * @return nullable ascii string of length between 0 and maxLength (inclusive)
     */
    fun consumeAsciiStringOrNull(maxLength: Int): String?

    /**
     * Consumes remaining fuzzing input as not null ascii string. After calling this method, further calls to methods of this interface will return fixed values only.
     *
     * @return ascii string
     */
    fun consumeRemainingAsAsciiString(): String

    /**
     * Consumes a not null string consisting of letters of latin alphabet from the fuzzer input. The returned string may be of any length between 0 and maxLength, even if there is more fuzzer input available.
     *
     * @param maxLength the maximum length of the string
     *
     * @return string consisting of letters of latin alphabet of length between 0 and maxLength (inclusive)
     */
    fun consumeLetterString(maxLength: Int): String

    /**
     * Consumes a nullable string consisting of letters of latin alphabet from the fuzzer input. The returned string may be of any length between 0 and maxLength, even if there is more fuzzer input available.
     *
     * @param maxLength the maximum length of the string
     *
     * @return nullable string consisting of letters of latin alphabet of length between 0 and maxLength (inclusive)
     */
    fun consumeLetterStringOrNull(maxLength: Int): String?

    /**
     * Consumes remaining fuzzing input as not null string consisting of letters of latin alphabet. After calling this method, further calls to methods of this interface will return fixed values only.
     *
     * @return string consisting of letters of latin alphabet
     */
    fun consumeRemainingAsLetterString(): String

    /**
     * Picks an element from collection based on the fuzzer input.
     *
     * @param collection collection to pick an element from
     * @param T the type of the collection element
     *
     * @return an element from collection chosen based on the fuzzer input
     */
    fun <T> KFuzzer.pickValue(collection: Collection<T>): T {
        require(collection.isNotEmpty()) { "collection is empty" }
        return collection.elementAt(consumeInt(collection.indices))
    }

    /**
     * Picks an element from array based on the fuzzer input.
     *
     * @param array array to pick an element from
     * @param T the type of the array element
     *
     * @return an element from array chosen based on the fuzzer input
     */
    fun <T> pickValue(array: Array<T>): T {
        require(array.isNotEmpty()) { "array is empty" }
        return array[consumeInt(array.indices)]
    }

    /**
     * Picks an element from boolean array based on the fuzzer input.
     *
     * @param array boolean array to pick an element from
     *
     * @return an element from boolean array chosen based on the fuzzer input
     */
    fun pickValue(array: BooleanArray): Boolean {
        require(array.isNotEmpty()) { "array is empty" }
        return array[consumeInt(array.indices)]
    }

    /**
     * Picks an element from byte array based on the fuzzer input.
     *
     * @param array byte array to pick an element from
     *
     * @return an element from byte array chosen based on the fuzzer input
     */
    fun pickValue(array: ByteArray): Byte {
        require(array.isNotEmpty()) { "array is empty" }
        return array[consumeInt(array.indices)]
    }

    /**
     * Picks an element from short array based on the fuzzer input.
     *
     * @param array short array to pick an element from
     *
     * @return an element from short array chosen based on the fuzzer input
     */
    fun pickValue(array: ShortArray): Short {
        require(array.isNotEmpty()) { "array is empty" }
        return array[consumeInt(array.indices)]
    }

    /**
     * Picks an element from int array based on the fuzzer input.
     *
     * @param array int array to pick an element from
     *
     * @return an element from int array chosen based on the fuzzer input
     */
    fun pickValue(array: IntArray): Int {
        require(array.isNotEmpty()) { "array is empty" }
        return array[consumeInt(array.indices)]
    }

    /**
     * Picks an element from long array based on the fuzzer input.
     *
     * @param array long array to pick an element from
     *
     * @return an element from long array chosen based on the fuzzer input
     */
    fun pickValue(array: LongArray): Long {
        require(array.isNotEmpty()) { "array is empty" }
        return array[consumeInt(array.indices)]
    }

    /**
     * Picks an element from double array based on the fuzzer input.
     *
     * @param array double array to pick an element from
     *
     * @return an element from double array chosen based on the fuzzer input
     */
    fun pickValue(array: DoubleArray): Double {
        require(array.isNotEmpty()) { "array is empty" }
        return array[consumeInt(array.indices)]
    }

    /**
     * Picks an element from float array based on the fuzzer input.
     *
     * @param array float array to pick an element from
     *
     * @return an element from float array chosen based on the fuzzer input
     */
    fun pickValue(array: FloatArray): Float {
        require(array.isNotEmpty()) { "array is empty" }
        return array[consumeInt(array.indices)]
    }

    /**
     * Picks an element from char array based on the fuzzer input.
     *
     * @param array char array to pick an element from
     *
     * @return an element from char array chosen based on the fuzzer input
     */
    fun pickValue(array: CharArray): Char {
        require(array.isNotEmpty()) { "array is empty" }
        return array[consumeInt(array.indices)]
    }
}