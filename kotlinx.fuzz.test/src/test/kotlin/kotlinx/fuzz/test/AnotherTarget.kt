package kotlinx.fuzz.test

import kotlinx.collections.immutable.persistentListOf
import kotlinx.fuzz.IgnoreFailures
import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer
import kotlinx.fuzz.asciiStringOrNull
import kotlinx.fuzz.test.RealUserCode.method1

class AnotherTarget {
    @KFuzzTest
    fun test(data: KFuzzer) {
        persistentListOf(1, 2, 3)
        method1(data.int(), data.int(), data.int(), data.boolean())
    }

    @KFuzzTest
    @IgnoreFailures
    fun `test which fails`(data: KFuzzer) {
        if (data.boolean()) {
            error("Expected failure")
        }
    }

    @KFuzzTest
    @IgnoreFailures
    fun `test with two fails`(data: KFuzzer) {
        if (data.boolean()) {
            if (data.boolean()) error("Expected failure")
            else throw NullPointerException()
        }
    }

    @KFuzzTest
    @IgnoreFailures
    fun `reproducer test`(data: KFuzzer) {
        if (data.boolean()) {
            if (data.method1() == 3) {
                if (data.method2() == 4) {
                    method3()
                } else {
                    error("Expected failure")
                }
            }
        }
    }

    @KFuzzTest
    fun extensionFunctionCheck(data: KFuzzer) {
        if (data.asciiStringOrNull(9) == "abibaboba") {
            error("Expected failure")
        }
    }
}

fun KFuzzer.method1() = int()
private fun KFuzzer.method2() = int()
private fun method3() {
    throw NullPointerException()
}