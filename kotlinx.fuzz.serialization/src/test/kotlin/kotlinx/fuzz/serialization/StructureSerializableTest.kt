package kotlinx.fuzz.serialization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.fuzz.KFuzzerImpl
import kotlinx.serialization.Serializable

class StructureSerializableTest {
    @Test
    fun `fuzz list`() {
        val data = byteArrayOf(0)
        val fuzzer = KFuzzerImpl(data)
        val serialization = fuzzer.serialization {
            collectionSize { 10 }
            int { 5 }
        }
        val result = serialization.fuzz<List<Int>>()
        val expected = List(10) { 5 }
        assertEquals(expected, result)
    }

    @Test
    fun `fuzz map`() {
        val data = byteArrayOf(0)
        val fuzzer = KFuzzerImpl(data)
        val serialization = fuzzer.serialization {
            collectionSize { 1 }
            int { 5 }
            boolean { true }
        }
        val result = serialization.fuzz<Map<Int, Boolean>>()
        val expected = mapOf(5 to true)
        assertEquals(expected, result)
    }

    @Test
    fun `fuzz set`() {
        val data = byteArrayOf(0)
        val fuzzer = KFuzzerImpl(data)
        val serialization = fuzzer.serialization {
            collectionSize { 1 }
            boolean { true }
        }
        val result = serialization.fuzz<Set<Boolean>>()
        val expected = setOf(true)
        assertEquals(expected, result)
    }

    @Test
    fun `fuzz class`() {
        val data = byteArrayOf(0)
        val fuzzer = KFuzzerImpl(data)
        val serialization = fuzzer.serialization {
            boolean { false }
            byte { 0 }
            short { 0 }
            int { 0 }
            long { 0 }
            float { 0f }
            double { 0.0 }
            char { '0' }
            string { "0" }
            collectionSize { 1 }
        }
        val result = serialization.fuzz<Class>()
        val expected = Class(
            boolean = false,
            byte = 0,
            short = 0,
            int = 0,
            long = 0,
            float = 0f,
            double = 0.0,
            char = '0',
            string = "0",
            nested = Class.Nested(
                set = setOf(0),
                list = listOf(false),
                map = mapOf("0" to 0),
                inline = Class.Inline(int = 0),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `fuzz null`() {
        val data = byteArrayOf(0)
        val fuzzer = KFuzzerImpl(data)
        val serialization = fuzzer.serialization {
            isNull { true }
        }
        val result = serialization.fuzz<String?>()
        assertNull(result)
    }

    @Serializable
    private data class Class(
        val boolean: Boolean,
        val byte: Byte,
        val short: Short,
        val int: Int,
        val long: Long,
        val float: Float,
        val double: Double,
        val char: Char,
        val string: String,
        val nested: Nested,
    ) {
        @Serializable
        data class Nested(
            val set: Set<Byte>,
            val list: List<Boolean>,
            val map: Map<String, Int>,
            val inline: Inline,
        )

        @Serializable
        @JvmInline
        value class Inline(val int: Int)
    }
}
