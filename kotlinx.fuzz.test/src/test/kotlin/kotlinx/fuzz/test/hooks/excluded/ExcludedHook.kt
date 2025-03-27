package kotlinx.fuzz.test.hooks.excluded

import com.code_intelligence.jazzer.api.HookType
import com.code_intelligence.jazzer.api.MethodHook
import java.lang.invoke.MethodHandle

object ExcludedHook {

    @MethodHook(
        type = HookType.REPLACE,
        targetClassName = "kotlin.random.Random",
        targetMethod = "nextInt"
    )
    @JvmStatic
    fun excludedHook(
        method: MethodHandle,
        thisObject: Any,
        arguments: Array<Any>,
        hookId: Int,
    ): Int {
        return 5
    }
}
