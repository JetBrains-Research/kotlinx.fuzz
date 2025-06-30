package kotlinx.fuzz.serialization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.fuzz.KFuzzerImpl

class PrimitivesSerializableTest {
    @Test
    fun `fuzz boolean`() {
        val data = byteArrayOf(0)
        val fuzzer = KFuzzerImpl(data)
        val serialization = fuzzer.serialization {
            boolean { true }
        }
        val result = serialization.fuzz<Boolean>()
        assertEquals(expected = true, actual = result)
    }

    @Test
    fun `fuzz byte`() {
        val data = byteArrayOf(0)
        val fuzzer = KFuzzerImpl(data)
        val serialization = fuzzer.serialization {
            byte { 0 }
        }
        val result = serialization.fuzz<Byte>()
        assertEquals(expected = 0, actual = result)
    }

    @Test
    fun `fuzz short`() {
        val data = byteArrayOf(0)
        val fuzzer = KFuzzerImpl(data)
        val serialization = fuzzer.serialization {
            short { 0 }
        }
        val result = serialization.fuzz<Short>()
        assertEquals(expected = 0, actual = result)
    }

    @Test
    fun `fuzz int`() {
        val data = byteArrayOf(0)
        val fuzzer = KFuzzerImpl(data)
        val serialization = fuzzer.serialization {
            int { 0 }
        }
        val result = serialization.fuzz<Int>()
        assertEquals(expected = 0, actual = result)
    }

    @Test
    fun `fuzz long`() {
        val data = byteArrayOf(0)
        val fuzzer = KFuzzerImpl(data)
        val serialization = fuzzer.serialization {
            long { 0 }
        }
        val result = serialization.fuzz<Long>()
        assertEquals(expected = 0, actual = result)
    }

    @Test
    fun `fuzz float`() {
        val data = byteArrayOf(0)
        val fuzzer = KFuzzerImpl(data)
        val serialization = fuzzer.serialization {
            float { 0f }
        }
        val result = serialization.fuzz<Float>()
        assertEquals(expected = 0f, actual = result)
    }

    @Test
    fun `fuzz double`() {
        val data = byteArrayOf(0)
        val fuzzer = KFuzzerImpl(data)
        val serialization = fuzzer.serialization {
            double { 0.0 }
        }
        val result = serialization.fuzz<Double>()
        assertEquals(expected = 0.0, actual = result)
    }

    @Test
    fun `fuzz char`() {
        val data = byteArrayOf(0)
        val fuzzer = KFuzzerImpl(data)
        val serialization = fuzzer.serialization {
            char { '0' }
        }
        val result = serialization.fuzz<Char>()
        assertEquals(expected = '0', actual = result)
    }

    @Test
    fun `fuzz string`() {
        val data = byteArrayOf(0)
        val fuzzer = KFuzzerImpl(data)
        val serialization = fuzzer.serialization {
            string { "0" }
        }
        val result = serialization.fuzz<String>()
        assertEquals(expected = "0", actual = result)
    }
}
