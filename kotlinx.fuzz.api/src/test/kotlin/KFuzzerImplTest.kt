package kotlinx.fuzz

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KFuzzerImplTest {
    @Test
    fun `test consumeBoolean`() {
        val data = byteArrayOf(0)
        val kFuzzer = KFuzzerImpl(data)
        assertFalse(kFuzzer.consumeBoolean())
    }

    @Test
    fun `test consumeBooleans`() {
        val data = byteArrayOf(1, 0, 1)
        val kFuzzer = KFuzzerImpl(data)
        val booleans = kFuzzer.consumeBooleans(3)
        assertArrayEquals(booleanArrayOf(true, false, true), booleans)
    }

    @Test
    fun `test consumeBooleansOrNull`() {
        val data = byteArrayOf(1)
        val kFuzzer = KFuzzerImpl(data)
        assertNull(kFuzzer.consumeBooleansOrNull(5))
    }

    @Test
    fun `test consumeByte`() {
        val data = byteArrayOf(0x7F.toByte())
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.consumeByte(50..100)
        assertEquals(result, 50.toByte())
    }

    @Test
    fun `test consumeByteOrNull`() {
        val data = byteArrayOf(0, 100)
        val kFuzzer = KFuzzerImpl(data)
        assertEquals(kFuzzer.consumeByteOrNull(), 100.toByte())
    }

    @Test
    fun `test consumeBytes`() {
        val data = byteArrayOf(1, 2, 3)
        val kFuzzer = KFuzzerImpl(data)
        org.junit.jupiter.api.assertThrows<IllegalArgumentException>("range should be a subset of [Byte.MIN_VALUE..Byte.MAX_VALUE] but was 0..255") {
            kFuzzer.consumeBytes(3, 0..255)
        }
    }

    @Test
    fun `test consumeRemainingAsByteArray`() {
        val data = byteArrayOf(10, 20, 30)
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.consumeRemainingAsByteArray()
        assertArrayEquals(byteArrayOf(10, 20, 30), result)
    }

    @Test
    fun `test consumeShort`() {
        val data = byteArrayOf(0, 0)
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.consumeShort(0..Short.MAX_VALUE)
        assertEquals(result, 0.toShort())
    }

    @Test
    fun `test consumeInts edge cases`() {
        val data = byteArrayOf(
            0x80.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0A.toByte(), // Int.MIN_VALUE + 10

            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFA.toByte(), // -6

            0x7F.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFA.toByte(), // Int.MAX_VALUE - 5
        )
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.consumeInts(3, (Int.MIN_VALUE + 20)..(Int.MAX_VALUE - 20))
        assertArrayEquals(result, intArrayOf(Int.MIN_VALUE + 30, 14, Int.MIN_VALUE + 54))
    }

    @Test
    fun `test consumeString`() {
        val data = byteArrayOf(0x80.toByte(), 0x00, 0x00, 0x03, 97, 98, 99)
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.consumeString(10, Charsets.UTF_8)
        assertEquals("abc", result)
    }

    @Test
    fun `test consumeLetter`() {
        val data = byteArrayOf(0x9A.toByte())
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.consumeLetter()
        assertEquals('A', result)
    }

    @Test
    fun `test consumeLetterString`() {
        val data = byteArrayOf(0x80.toByte(), 0x00, 0x00, 0x03, 0x97.toByte(), 0x98.toByte(), 0x99.toByte(),)
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.consumeLetterString(10)
        assertEquals("xyz", result)
    }

    @Test
    fun `test consumeLong`() {
        val data = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 100)
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.consumeLong()
        assertEquals(result, 100)
    }


    @Test
    fun `test consumeLongs edge cases`() {
        val data = byteArrayOf(
            0x80.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0A.toByte(), // Long.MIN_VALUE + 10

            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFA.toByte(), // -6

            0x7F.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFA.toByte(), // Long.MAX_VALUE - 5
        )
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.consumeLongs(3, (Long.MIN_VALUE + 20)..(Long.MAX_VALUE - 20))
        assertArrayEquals(result, longArrayOf(Long.MIN_VALUE + 30, 14, Long.MIN_VALUE + 54))
    }

    @Test
    fun `test consumeChar`() {
        val data = byteArrayOf(0x80.toByte(), 0)
        val kFuzzer = KFuzzerImpl(data)
        val result = kFuzzer.consumeChar('A'..'Z')
        assertEquals('A', result)
    }
}