package kotlinx.fuzz.test

import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer

class HookTargetTest {

    @KFuzzTest(maxFuzzTime = "3s", keepGoing = 1)
    fun simpleHook(kf: KFuzzer) {
        val random = java.util.Random(kf.long())
        if (random.nextInt() != 5) error("hooks are not hooking")
    }

    @KFuzzTest(maxFuzzTime = "3s", keepGoing = 1, customHookExcludes = ["kotlinx.fuzz.test.hooks.excluded"])
    fun excludedHook(kf: KFuzzer) {
        val random = kotlin.random.Random(kf.long())
        if (random.nextInt() == 5) error("hook was not excluded")
    }

}
