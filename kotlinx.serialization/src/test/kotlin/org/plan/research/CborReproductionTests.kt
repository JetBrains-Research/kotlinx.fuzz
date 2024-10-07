package org.plan.research

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test

@OptIn(ExperimentalSerializationApi::class)
object CborReproductionTests {
    @Test
    fun `unhandled illegal state exception`() {
        val byteArray = byteArrayOf(126)
        val serializer = Cbor.Default
        assertThrows<SerializationException> {
            serializer.decodeFromByteArray<String>(byteArray)
        }
    }

    @Test
    fun `unhandled negative array size exception`() {
        val byteArray = byteArrayOf(127, 40)
        val serializer = Cbor.Default
        assertThrows<SerializationException> {
            serializer.decodeFromByteArray<String>(byteArray)
        }
    }

    @Test
    fun `unhandled stack overflow error`() {
        val byteArray = byteArrayOf(127, 0, 0)
        val serializer = Cbor.Default
        assertThrows<SerializationException> {
            serializer.decodeFromByteArray<String>(byteArray)
        }
    }

    @Test
    fun `unhandled array index oob exception`() {
        val byteArray = byteArrayOf(
            -103, 7, 127, 127, -61, 111, 98, 106, 0, 0, -1, -66, -1, -9, -29, 47, 38, 38, 38, 38, 1, 38, 38, 38,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 38, 38, 38, 38, 38, 38, 111, 98, 106, -17, -65, -67, -17, -65, -67,
            -17, -65, -67, -17, -65, -67, -17, -65, -67, -17, -65, -67, 122, -17, -65, -67, -17, -65, -67, -17,
            -65, -67, -17, -65, -67, -17, -65, -67, -17, -65, -67, -17, -65, -67, -17, -65, -67, -17, -65, -67,
            -17, -65, -67, -17, -65, -67, -17, -65, -67, -17, -65, -67, -17, -65, -67, -17, 38, 38, 38, 38, 38,
            38, 38, 126, 126, 126, 38, 35, -128, -128, -128, -128, -128, -128, -128, -128, -128, 126, 126, 126,
            126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126,
            126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126,
            126, -67, -17, -65, -67, -17, 126, 126, 126, 126, 5, 0, 126, 126, 126, 126, 126, 126, 126, 126, 126,
            126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, 126, -1, -1, -1, -1, -1, -1, -1,
            -1, 126, 126
        )
        val serializer = Cbor {
            ignoreUnknownKeys = true
        }
        val value = serializer.decodeFromByteArray<Value>(byteArray)
        println(value)
    }
}