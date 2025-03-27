package kotlinx.fuzz.test.hooks

import com.code_intelligence.jazzer.api.HookType
import com.code_intelligence.jazzer.api.MethodHook
import java.lang.invoke.MethodHandle

object Hooks {

    @MethodHook(
        type = HookType.REPLACE,
        targetClassName = "java.util.Random",
        targetMethod = "nextInt"
    )
    @JvmStatic
    fun deterministicRandom(
        method: MethodHandle,
        thisObject: Any,
        arguments: Array<Any>,
        hookId: Int,
    ): Int {
        return 5
    }

}
