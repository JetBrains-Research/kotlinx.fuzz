package kotlinx.fuzz.serialization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.fuzz.KFuzzerImpl
import kotlinx.serialization.Serializable

class EnumSerializableTest {
    @Test
    fun `fuzz enum`() {
        val data = byteArrayOf(0)
        val fuzzer = KFuzzerImpl(data)
        val result = fuzzer.serialization.fuzz<Enum>()
        assertEquals(expected = Enum.A, actual = result)
    }
    @Serializable
    enum class Enum {
        A
    }
}
