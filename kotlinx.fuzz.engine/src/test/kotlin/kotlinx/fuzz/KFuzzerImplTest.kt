package kotlinx.fuzz

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KFuzzerImplTest {
    /**
     * Reverts the KFuzzerImpl::fitIntoRange functionality
     */
    private fun byte(expected: Int, range: IntRange): Byte {
        val rangeSize = range.last.toLong() - range.first + 1
        for (n in range) {
            val fitted = (n.toLong() - Byte.MIN_VALUE) % rangeSize + range.first
            if (fitted.toInt() == expected) {
                return n.toByte()
            }
        }
        error("Could not find a correct number")
    }

    /**
     * Reverts the KFuzzerImpl::fitIntoRange functionality
     */
    private fun int(expected: Int, range: IntRange): ByteArray {
        val result = byteArrayOf(0, 0, 0, 0)
        val rangeSize = range.last.toLong() - range.first + 1
        for (n in range) {
            val fitted = (n.toLong() - Int.MIN_VALUE) % rangeSize + range.first
            if (fitted.toInt() == expected) {
                result[0] = (n ushr 24).toByte()
                result[1] = (n ushr 16).toByte()
                result[2] = (n ushr 8).toByte()
                result[3] = n.toByte()
                return result
            }
        }
        error("Could not find a correct mapping for number $expected in range $range")
    }

    @Test
    fun `test boolean`() {
        val data = byteArrayOf(0)
        val kFuzzer = KFuzzerImpl(data)
        assertFalse(kFuzzer.boolean())
    }

    @Test
    fun `test booleans`() {
        val data = byteArrayOf(1, 0, 1)
        val kFuzzer = KFuzzerImpl(data)
        val booleans = kFuzzer.booleans(3)
        assertArrayEquals(booleanArrayOf(true, false, true), booleans)
    }

    @Test
    fun `test booleansOrNull`() {
        val data = byteArrayOf(0)
        val kFuzzer = KFuzzerImpl(data)
        assertNull(kFuzzer.booleansOrNull(5))
    }

    @Test
    fun `test byte`() {
        val data = byteArrayOf(0x7F.toByte())
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.byte(50..100)
        assertEquals(50.toByte(), result)
    }

    @Test
    fun `test byteOrNull`() {
        val data = byteArrayOf(1, 100)
        val kFuzzer = KFuzzerImpl(data)
        assertEquals(100.toByte(), kFuzzer.byteOrNull())
    }

    @Test
    fun `test bytes`() {
        val data = byteArrayOf(1, 2, 3)
        val kFuzzer = KFuzzerImpl(data)
        assertThrows<IllegalArgumentException>(
            "range should be a subset of [Byte.MIN_VALUE..Byte.MAX_VALUE] but was 0..255",
        ) {
            kFuzzer.bytes(3, 0..255)
        }
    }

    @Test
    fun `test remainingAsByteArray`() {
        val data = byteArrayOf(10, 20, 30)
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.remainingAsByteArray()
        assertArrayEquals(byteArrayOf(10, 20, 30), result)
    }

    @Test
    fun `test short`() {
        val data = byteArrayOf(0, 0)
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.short(0..Short.MAX_VALUE)
        assertEquals(0.toShort(), result)
    }

    @Test
    fun `test ints edge cases`() {
        val data = byteArrayOf(
            0x80.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0A.toByte(),  // Int.MIN_VALUE + 10

            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFA.toByte(),  // -6

            0x7F.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFA.toByte(),  // Int.MAX_VALUE - 5
        )
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.ints(3, (Int.MIN_VALUE + 20)..(Int.MAX_VALUE - 20))
        assertArrayEquals(intArrayOf(Int.MIN_VALUE + 30, 14, Int.MIN_VALUE + 54), result)
    }

    @Test
    fun `test int range distribution`() {
        val range = 0..50
        val multiplier = 5
        val limit = range.count() * multiplier
        val data = buildList {
            repeat(limit) {
                add(0x00.toByte())
                add(0x00.toByte())
                add(0x00.toByte())
                add(it.toByte())
            }
        }.toByteArray()
        val kFuzzer = KFuzzerImpl(data)
        val distribution = buildMap<Int, Int> {
            repeat(limit) {
                val result = kFuzzer.int(range)
                this[result] = this.getOrDefault(result, 0) + 1
            }
        }
        assertEquals(range.count(), distribution.size)
        assertTrue { distribution.values.all { it == multiplier } }
    }

    @Test
    fun `test string`() {
        val data = byteArrayOf(97, 98, 99)
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.string(3, Charsets.UTF_8)
        assertEquals("abc", result)
    }

    @Test
    fun `test letter`() {
        val letterRange = 0 until CharacterSet.US_LETTERS.size
        val data = byteArrayOf(
            *int(26, letterRange),  // ('a'..'z').count()
        )
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.letter()
        assertEquals('A', result)
    }

    @Test
    fun `test asciiString`() {
        val data = byteArrayOf(120, 121, 122)
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.asciiString(3)
        assertEquals("xyz", result)
    }

    @Test
    fun `test letterString`() {
        val letterRange = 0 until CharacterSet.US_LETTERS.size
        val data = byteArrayOf(
            *int('a' - 'a', letterRange),
            *int('b' - 'a', letterRange),
            *int('c' - 'a', letterRange),
        )
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.letterString(3)
        assertEquals("abc", result)
    }

    @Test
    fun `test long`() {
        val data = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 100)
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.long()
        assertEquals(100, result)
    }

    @Test
    fun `test longs edge cases`() {
        val data = byteArrayOf(
            0x80.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0A.toByte(),  // Long.MIN_VALUE + 10

            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFA.toByte(),  // -6

            0x7F.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFA.toByte(),  // Long.MAX_VALUE - 5
        )
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.longs(3, (Long.MIN_VALUE + 20)..(Long.MAX_VALUE - 20))
        assertArrayEquals(longArrayOf(Long.MIN_VALUE + 30, 14, Long.MIN_VALUE + 54), result)
    }

    @Test
    fun `test char`() {
        val data = byteArrayOf(0x80.toByte(), 0)
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.char('A'..'Z')
        assertEquals('A', result)
    }

    @Test
    fun `test regexString`() {
        val data = buildList {
            repeat(10_000) { add(((it * 31) % 512).toByte()) }
        }.toByteArray()
        val kFuzzer = KFuzzerImpl(data)

        repeat(10) {
            val reg = Regex("[a-z]+(abc){3,}[a-z]{1,2}q")
            val result = kFuzzer.string(reg)
            assertTrue(result.matches(reg))
        }

        repeat(10) {
            val reg = Regex("a+")
            val caseInsensitiveReg = Regex("[aA]+")
            val result = kFuzzer.string(
                reg,
                KFuzzer.RegexConfiguration(
                    maxInfinitePatternLength = 10,
                    caseInsensitive = true,
                ),
            )
            assertFalse(result.matches(reg))
            assertTrue(result.matches(caseInsensitiveReg))
            assertTrue(result.length <= 10)
        }

        repeat(10) {
            val reg = Regex(".*")
            val chars = setOf('a', 'b', '0', 'y')
            val result = kFuzzer.string(
                reg,
                KFuzzer.RegexConfiguration(
                    maxInfinitePatternLength = 10,
                    allowedCharacters = CharacterSet(chars),
                ),
            )
            assertTrue(result.matches(reg))
            assertTrue(result.all { it in chars })
            assertTrue(result.length <= 20)
        }
    }

    @Test
    fun `test all finite floats`() {
        val kFuzzer = KFuzzerImpl(randomByteArray(4 * 10_000))
        val range = FiniteFloatRange(-Float.MAX_VALUE, Float.MAX_VALUE)
        repeat(10_000) {
            assertTrue { kFuzzer.float(range).isFinite() }
        }
    }

    @Test
    fun `test all finite doubles`() {
        val kFuzzer = KFuzzerImpl(randomByteArray(4 * 10_000))
        val range = FiniteDoubleRange(-Double.MAX_VALUE, Double.MAX_VALUE)
        repeat(10_000) {
            assertTrue { kFuzzer.double(range).isFinite() }
        }
    }

    private fun randomByteArray(size: Int): ByteArray = Random(0xDEAD_BEEF)
        .nextBytes(size = size)

    class FiniteFloatRange(
        override val start: Float,
        override val endInclusive: Float,
    ) : ClosedFloatingPointRange<Float> {
        init {
            require(start.isFinite()) { "Start must be finite" }
            require(endInclusive.isFinite()) { "End must be finite" }
        }

        override fun contains(value: Float): Boolean = value.isFinite() && value >= start && value <= endInclusive

        override fun lessThanOrEquals(a: Float, b: Float): Boolean = a <= b
    }

    class FiniteDoubleRange(
        override val start: Double,
        override val endInclusive: Double,
    ) : ClosedFloatingPointRange<Double> {
        init {
            require(start.isFinite()) { "Start must be finite" }
            require(endInclusive.isFinite()) { "End must be finite" }
        }

        override fun contains(value: Double): Boolean = value.isFinite() && value >= start && value <= endInclusive

        override fun lessThanOrEquals(a: Double, b: Double): Boolean = a <= b
    }
}
