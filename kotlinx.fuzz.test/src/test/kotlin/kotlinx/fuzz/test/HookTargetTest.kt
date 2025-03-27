package kotlinx.fuzz.test

import kotlinx.fuzz.KFuzzTest
import kotlinx.fuzz.KFuzzer
import java.util.Random

class HookTargetTest {

    @KFuzzTest(maxFuzzTime = "3s", keepGoing = 1)
    fun random(kf: KFuzzer) {
        val random = Random(kf.long())
        if (random.nextInt() != 5) error("hooks are not hooking")
    }

}
