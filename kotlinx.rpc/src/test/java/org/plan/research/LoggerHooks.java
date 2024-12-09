package org.plan.research;

import com.code_intelligence.jazzer.api.HookType;
import com.code_intelligence.jazzer.api.MethodHook;

import java.lang.invoke.MethodHandle;

public class LoggerHooks {

    // TODO: hook logger (factory) ctor

    @MethodHook(
            type = HookType.REPLACE,
            targetClassName = "org.plan.research.KLAL",
            targetMethod = "haha"
    )
    public static void testHook(
            MethodHandle handle, Object thisObject, Object[] args, int hookId
    ) {
        System.out.println("thisObject class: " + thisObject.getClass().getName());
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            System.out.println("arg " + i + ": " + arg);
        }
        throw new IllegalStateException("hook exception");
    }


    @MethodHook(
            type = HookType.REPLACE,
            targetClassName = "kotlinx.rpc.krpc.internal.logging.CommonLogger",
            targetMethod = "error"
    )
//    @MethodHook(
//            type = HookType.REPLACE,
//            targetClassName = "kotlinx.rpc.krpc.server.internal.",
//            targetMethod = "error"
//    )
    public static void throwOnError(
            MethodHandle handle, Object thisObject, Object[] args, int hookId
    ) {
        System.out.println("thisObject class: " + thisObject.getClass().getName());
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            System.out.println("arg " + i + ": " + arg);
        }
        throw new IllegalStateException("haha");
    }

}
