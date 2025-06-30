package kotlinx.fuzz.serialization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.fuzz.KFuzzerImpl
import kotlinx.serialization.Serializable

class InlineSerializableTest {
    @Test
    fun `fuzz inline`() {
        val data = byteArrayOf(0)
        val fuzzer = KFuzzerImpl(data)
        val result = fuzzer.serialization.fuzz<IntWrapper>()
        assertEquals(expected = 1, actual = result.int)
    }
    @Serializable
    @JvmInline
    @KFuzzerOptions(IntWrapper.Options::class)
    value class IntWrapper(
        @KFuzzerOptions(IntWrapper.IntOptions::class)
        val int: Int,
    ) {
        object Options : KFuzzerOptions.Provider {
            override fun KFuzzerSerialization.Builder.apply() {
                int { 0 }
            }
        }

        // Options must override each other the deeper you go
        object IntOptions : KFuzzerOptions.Provider {
            override fun KFuzzerSerialization.Builder.apply() {
                int { 1 }
            }
        }
    }
}
