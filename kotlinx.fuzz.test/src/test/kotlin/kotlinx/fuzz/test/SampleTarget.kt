package kotlinx.fuzz.test

import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer
import kotlinx.fuzz.KFuzzerImpl
import kotlin.test.Test
import kotlin.test.assertEquals

// TODO: kotlin.test requires a class with empty ctor, NOT an object. Will this be a problem?
class SampleTarget {

    @KFuzzTest
    @Test
    fun test() {
        // for now, just checking that all dependencies resolve
        val kfuzzer: KFuzzer = KFuzzerImpl(byteArrayOf(1))
        assertEquals(1, kfuzzer.consumeByte())
    }

}
