package kotlinx.fuzz.test

import kotlinx.fuzz.IgnoreFailures
import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer
import kotlinx.fuzz.test.RealUserCode.method1

class AnotherTarget {
    @KFuzzTest
    fun test(data: KFuzzer) {
        method1(data.int(), data.int(), data.int(), data.boolean())
    }

    @KFuzzTest
    @IgnoreFailures
    fun `test which fails`(data: KFuzzer) {
        if (data.boolean()) {
            error("Expected failure")
        }
    }
}
